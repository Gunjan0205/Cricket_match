package cricket_score_backend;

import cricket_score_backend.ScoreModels.CreateMatchRequest;
import cricket_score_backend.ScoreModels.DeliveryRequest;
import cricket_score_backend.ScoreModels.MatchView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class ScoreController {
    private final ScoreService scores;
    private final LiveScoreEvents events;

    public ScoreController(ScoreService scores, LiveScoreEvents events) {
        this.scores = scores;
        this.events = events;
    }

    @GetMapping("/matches")
    public List<MatchView> matches() {
        return scores.listMatches();
    }

    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchView createMatch(@Valid @RequestBody CreateMatchRequest request) {
        return scores.createMatch(request);
    }

    @GetMapping("/matches/{id}")
    public MatchView match(@PathVariable long id) {
        return scores.getMatch(id);
    }

    @PostMapping("/matches/{id}/deliveries")
    public MatchView addDelivery(@PathVariable long id, @Valid @RequestBody DeliveryRequest request) {
        return scores.addDelivery(id, request);
    }

    @GetMapping("/events")
    public SseEmitter events() {
        return events.subscribe();
    }
}