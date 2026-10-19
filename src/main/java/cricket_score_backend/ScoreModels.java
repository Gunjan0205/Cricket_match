package cricket_score_backend;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class ScoreModels {
    private ScoreModels() {
    }

    public record CreateMatchRequest(
            @NotBlank String title,
            @NotBlank String teamOne,
            @NotBlank String teamTwo,
            @NotBlank String venue,
            @NotBlank String format,
            @NotBlank String battingTeam,
            @NotBlank String striker,
            @NotBlank String nonStriker,
            @NotBlank String bowler) {
    }

    public record DeliveryRequest(
            @NotBlank String batter,
            @NotBlank String bowler,
            @Min(0) int runs,
            @Min(0) int extras,
            String extraType,
            boolean wicket,
            String replacementBatter) {
    }

    public record PlayerFigures(String name, int runs, int balls, int fours, int sixes, double strikeRate) {
    }

    public record BowlerFigures(String name, int runsConceded, int wickets, int legalBalls, double economy) {
    }

    public record DeliveryView(Long id, int overNumber, int ballNumber, String batter, String bowler,
            int runs, int extras, String extraType, boolean wicket, Instant recordedAt) {
    }

    public record MatchView(Long id, String title, String teamOne, String teamTwo, String venue,
            String format, String status, String battingTeam, int runs, int wickets,
            int completedOvers, int ballsInOver, Integer target, String striker,
            String nonStriker, String currentBowler, String summary,
            double runRate, List<PlayerFigures> batters, List<BowlerFigures> bowlers,
            List<DeliveryView> recentDeliveries, Instant startedAt) {
    }
}