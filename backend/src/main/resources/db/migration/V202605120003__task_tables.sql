-- Iter 4: tasks, status history and reminder log.

CREATE TABLE task (
    id UUID PRIMARY KEY,
    ereignis_id UUID REFERENCES ereignis(id),
    author_id UUID NOT NULL REFERENCES user_account(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    assigned_to_person_id UUID REFERENCES person(id),
    assigned_to_persongroup_id UUID REFERENCES persongroup(id),
    assigned_to_topic_id UUID REFERENCES topic(id),
    assigned_to_self BOOLEAN NOT NULL DEFAULT FALSE,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'open',
    completed_at TIMESTAMP,
    dropped_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT task_status_chk CHECK (status IN ('open', 'in_progress', 'done', 'dropped')),
    CONSTRAINT task_exactly_one_assignment CHECK (
        (CASE WHEN assigned_to_person_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN assigned_to_persongroup_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN assigned_to_topic_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN assigned_to_self THEN 1 ELSE 0 END) = 1
    )
);

CREATE INDEX task_author_status_idx ON task (author_id, status);
CREATE INDEX task_due_date_idx ON task (due_date) WHERE due_date IS NOT NULL;

CREATE TABLE task_status_history (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    note TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by_author_id UUID NOT NULL REFERENCES user_account(id),
    CONSTRAINT task_status_history_to_chk CHECK (to_status IN ('open', 'in_progress', 'done', 'dropped')),
    CONSTRAINT task_status_history_from_chk CHECK (from_status IS NULL OR from_status IN ('open', 'in_progress', 'done', 'dropped'))
);

CREATE INDEX task_status_history_task_idx ON task_status_history (task_id, changed_at DESC);

CREATE TABLE task_reminder (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    reminded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reminded_on DATE NOT NULL,
    reminder_type VARCHAR(30) NOT NULL,
    CONSTRAINT task_reminder_type_chk CHECK (reminder_type IN ('one_day_before', 'on_due_date'))
);

-- Idempotency: once a reminder of a given type has fired for a given day per
-- task, the scheduler must not re-send it. The application keeps reminded_on
-- aligned with the calendar day in the server's UTC zone.
CREATE UNIQUE INDEX task_reminder_unique ON task_reminder (task_id, reminder_type, reminded_on);
