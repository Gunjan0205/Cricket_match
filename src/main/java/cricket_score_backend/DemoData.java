package cricket_score_backend;

import cricket_score_backend.ScoreModels.CreateMatchRequest;
import cricket_score_backend.ScoreModels.DeliveryRequest;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoData {
    @Bean
    ApplicationRunner seedMatches(CricketMatchRepository matches, ScoreService scores) {
        return args -> {
            if (!matches.findAll().isEmpty())
                return;
            ScoreModels.MatchView live = scores.createMatch(new CreateMatchRequest(
                    "Chennai Super Kings vs Royal Challengers Bengaluru", "Chennai Super Kings",
                    "Royal Challengers Bengaluru", "M. A. Chidambaram Stadium, Chennai",
                    "T20 · Match 18", "Royal Challengers Bengaluru", "Virat Kohli",
                    "Faf du Plessis", "Deepak Chahar"));
            scores.addDelivery(live.id(), new DeliveryRequest("Virat Kohli", "Deepak Chahar", 4, 0, null, false, null));
            scores.addDelivery(live.id(), new DeliveryRequest("Virat Kohli", "Deepak Chahar", 1, 0, null, false, null));
            scores.addDelivery(live.id(),
                    new DeliveryRequest("Faf du Plessis", "Deepak Chahar", 0, 0, null, false, null));
            scores.addDelivery(live.id(),
                    new DeliveryRequest("Faf du Plessis", "Deepak Chahar", 6, 0, null, false, null));
            scores.createMatch(new CreateMatchRequest("Mumbai Indians vs Sunrisers Hyderabad",
                    "Mumbai Indians", "Sunrisers Hyderabad", "Wankhede Stadium, Mumbai",
                    "T20 · Match 19", "Mumbai Indians", "Rohit Sharma", "Ishan Kishan",
                    "Pat Cummins"));
        };
    }
}