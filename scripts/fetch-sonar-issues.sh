#!/usr/bin/env bash
# Fetch unresolved SonarCloud issues for the configured project and render them
# as a markdown snapshot on stdout. Intended for CI to upsert the body of a
# pinned GitHub Issue so Claude can read the current state via the GitHub MCP.
#
# Required env: SONAR_TOKEN
# Optional env: SONAR_PROJECT_KEY (default: sebolber_PortalBriefingAgent)
#               SONAR_HOST_URL    (default: https://sonarcloud.io)
#               WAIT_FOR_SCAN     (1 = poll until the latest analysis finishes)

set -euo pipefail

PROJECT_KEY="${SONAR_PROJECT_KEY:-sebolber_PortalBriefingAgent}"
HOST="${SONAR_HOST_URL:-https://sonarcloud.io}"

: "${SONAR_TOKEN:?SONAR_TOKEN must be set}"

api() {
    curl -fsS -u "${SONAR_TOKEN}:" "$@"
}

if [[ "${WAIT_FOR_SCAN:-0}" == "1" ]]; then
    for attempt in 1 2 3 4 5 6; do
        status=$(api "${HOST}/api/ce/component?component=${PROJECT_KEY}" \
            | jq -r '.current.status // empty')
        case "${status}" in
            SUCCESS|FAILED|CANCELED|"") break ;;
            *) sleep 5 ;;
        esac
    done
fi

issues_acc='[]'
page=1
while : ; do
    response=$(api \
        "${HOST}/api/issues/search?componentKeys=${PROJECT_KEY}&resolved=false&p=${page}&ps=500&s=SEVERITY&asc=false")
    chunk=$(jq '.issues' <<<"${response}")
    chunk_size=$(jq 'length' <<<"${chunk}")
    issues_acc=$(jq -n --argjson a "${issues_acc}" --argjson b "${chunk}" '$a + $b')
    total=$(jq '.paging.total' <<<"${response}")
    page_size=$(jq '.paging.pageSize' <<<"${response}")
    seen=$(( page * page_size ))
    [[ "${seen}" -ge "${total}" ]] && break
    [[ "${chunk_size}" -eq 0 ]] && break
    page=$(( page + 1 ))
done

total=$(jq 'length' <<<"${issues_acc}")
timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

cat <<HEAD
# Sonar Issue Snapshot — \`main\`

_Last update: ${timestamp} · auto-managed by CI · do not edit manually._

Project: [\`${PROJECT_KEY}\`](${HOST}/dashboard?id=${PROJECT_KEY})

**Open issues:** ${total}
HEAD

if [[ "${total}" -eq 0 ]]; then
    cat <<'EMPTY'

No open issues. ✓
EMPTY
    exit 0
fi

cat <<'SEVHEAD'

## By severity

SEVHEAD

jq -r '
  group_by(.severity)
  | map({
      key: (.[0].severity),
      count: length,
      ord: (if .[0].severity == "BLOCKER" then 5
            elif .[0].severity == "CRITICAL" then 4
            elif .[0].severity == "MAJOR" then 3
            elif .[0].severity == "MINOR" then 2
            else 1 end)
    })
  | sort_by(-.ord)
  | .[]
  | "- **\(.key)**: \(.count)"
' <<<"${issues_acc}"

cat <<'DETHEAD'

## By file

DETHEAD

jq -r --arg host "${HOST}" '
  group_by(.component)
  | sort_by(.[0].component)
  | .[]
  | "<details><summary><strong>\(.[0].component | sub("^[^:]+:"; ""))</strong> — \(length) issue(s)</summary>\n"
    + (map(
        "- L\(.line // 0) · `\(.severity)` · [`\(.rule)`](\($host)/coding_rules?open=\(.rule)&rule_key=\(.rule)) — \(.message | gsub("\\|"; "\\|"))"
      ) | join("\n"))
    + "\n</details>"
' <<<"${issues_acc}"
