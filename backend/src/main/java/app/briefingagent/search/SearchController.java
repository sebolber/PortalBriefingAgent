package app.briefingagent.search;

import app.briefingagent.security.CurrentAuthor;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final SearchService searchService;
    private final CurrentAuthor currentAuthor;

    public SearchController(SearchService searchService, CurrentAuthor currentAuthor) {
        this.searchService = searchService;
        this.currentAuthor = currentAuthor;
    }

    @GetMapping("/api/search")
    public Response search(@RequestParam(name = "q", required = false) String q) {
        List<SearchService.Hit> hits = searchService.search(currentAuthor.requireUserId(), q);
        return new Response(q, hits);
    }

    public record Response(String query, List<SearchService.Hit> hits) {
    }
}
