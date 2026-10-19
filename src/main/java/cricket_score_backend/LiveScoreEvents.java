package cricket_score_backend;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class LiveScoreEvents {
    private final Set<SseEmitter> clients = ConcurrentHashMap.newKeySet();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        clients.add(emitter);
        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(() -> clients.remove(emitter));
        emitter.onError(error -> clients.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("Live score stream connected"));
        } catch (IOException exception) {
            clients.remove(emitter);
        }
        return emitter;
    }

    public void publish(ScoreModels.MatchView match) {
        clients.removeIf(client -> {
            try {
                client.send(SseEmitter.event().name("score-update").data(match));
                return false;
            } catch (IOException exception) {
                return true;
            }
        });
    }
}