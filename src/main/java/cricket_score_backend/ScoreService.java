package cricket_score_backend;

import cricket_score_backend.ScoreModels.BowlerFigures;
import cricket_score_backend.ScoreModels.DeliveryRequest;
import cricket_score_backend.ScoreModels.DeliveryView;
import cricket_score_backend.ScoreModels.MatchView;
import cricket_score_backend.ScoreModels.PlayerFigures;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScoreService {
    private final CricketMatchRepository matches;
    private final BallDeliveryRepository deliveries;
    private final LiveScoreEvents events;

    public ScoreService(CricketMatchRepository matches, BallDeliveryRepository deliveries,
            LiveScoreEvents events) {
        this.matches = matches;
        this.deliveries = deliveries;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<MatchView> listMatches() {
        return matches.findAll().stream().map(match -> view(match, deliveries.findByMatchIdOrderByIdAsc(match.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchView getMatch(long id) {
        CricketMatch match = findMatch(id);
        return view(match, deliveries.findByMatchIdOrderByIdAsc(id));
    }

    @Transactional
    public MatchView createMatch(ScoreModels.CreateMatchRequest request) {
        CricketMatch match = matches.save(new CricketMatch(request.title(), request.teamOne(),
                request.teamTwo(), request.venue(), request.format(), "LIVE", request.battingTeam(),
                request.striker(), request.nonStriker(), request.bowler(), null, "Match in progress"));
        return view(match, List.of());
    }

    @Transactional
    public MatchView addDelivery(long matchId, DeliveryRequest request) {
        CricketMatch match = findMatch(matchId);
        if (!"LIVE".equals(match.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This match is not live");
        }
        if (request.runs() > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batter runs must be between 0 and 6");
        }
        if (request.extras() > 0 && (request.extraType() == null || request.extraType().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an extra type");
        }
        if (request.extraType() != null && !request.extraType().isBlank()
                && !List.of("WIDE", "NO_BALL", "BYE", "LEG_BYE").contains(request.extraType().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported extra type");
        }
        if (!request.batter().equals(match.getStriker()) && !request.batter().equals(match.getNonStriker())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batter must be at the crease");
        }

        List<BallDelivery> current = deliveries.findByMatchIdOrderByIdAsc(matchId);
        int legalBalls = (int) current.stream().filter(ScoreService::isLegal).count();
        int overNumber = legalBalls / 6;
        int ballNumber = legalBalls % 6 + 1;
        String extraType = request.extraType() == null ? "" : request.extraType().toUpperCase();
        BallDelivery delivery = deliveries.save(new BallDelivery(match, 1, overNumber, ballNumber,
                request.batter(), request.bowler(), request.runs(), request.extras(), extraType, request.wicket()));
        current.add(delivery);

        int totalWickets = (int) current.stream().filter(BallDelivery::isWicket).count();
        int updatedLegalBalls = legalBalls + (isLegal(delivery) ? 1 : 0);
        String striker = match.getStriker();
        String nonStriker = match.getNonStriker();
        if (request.wicket()) {
            striker = request.replacementBatter() == null || request.replacementBatter().isBlank()
                    ? "New batter"
                    : request.replacementBatter().trim();
        } else if (((request.runs() + request.extras()) % 2) == 1) {
            String previousStriker = striker;
            striker = nonStriker;
            nonStriker = previousStriker;
        }
        if (isLegal(delivery) && updatedLegalBalls % 6 == 0) {
            String previousStriker = striker;
            striker = nonStriker;
            nonStriker = previousStriker;
        }
        int totalRuns = current.stream().mapToInt(ball -> ball.getBatterRuns() + ball.getExtras()).sum();
        match.updateScore(totalRuns, totalWickets, updatedLegalBalls,
                striker, nonStriker, request.bowler());
        CricketMatch saved = matches.save(match);
        MatchView updated = view(saved, current);
        events.publish(updated);
        return updated;
    }

    private CricketMatch findMatch(long id) {
        return matches.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
    }

    private MatchView view(CricketMatch match, List<BallDelivery> allDeliveries) {
        int runs = allDeliveries.stream().mapToInt(ball -> ball.getBatterRuns() + ball.getExtras()).sum();
        int wickets = (int) allDeliveries.stream().filter(BallDelivery::isWicket).count();
        int legalBalls = (int) allDeliveries.stream().filter(ScoreService::isLegal).count();
        int completedOvers = legalBalls / 6;
        int ballsInOver = legalBalls % 6;
        double runRate = legalBalls == 0 ? 0 : round(runs * 6.0 / legalBalls);

        Map<String, BatterAccumulator> batterMap = new LinkedHashMap<>();
        Map<String, BowlerAccumulator> bowlerMap = new LinkedHashMap<>();
        for (BallDelivery ball : allDeliveries) {
            BatterAccumulator batter = batterMap.computeIfAbsent(ball.getBatter(), BatterAccumulator::new);
            batter.runs += ball.getBatterRuns();
            if (!"WIDE".equals(ball.getExtraType()))
                batter.balls++;
            if (ball.getBatterRuns() == 4)
                batter.fours++;
            if (ball.getBatterRuns() == 6)
                batter.sixes++;

            BowlerAccumulator bowler = bowlerMap.computeIfAbsent(ball.getBowler(), BowlerAccumulator::new);
            bowler.runs += ball.getBatterRuns();
            if (!"BYE".equals(ball.getExtraType()) && !"LEG_BYE".equals(ball.getExtraType())) {
                bowler.runs += ball.getExtras();
            }
            if (isLegal(ball))
                bowler.legalBalls++;
            if (ball.isWicket())
                bowler.wickets++;
        }

        List<PlayerFigures> batters = batterMap.values().stream().map(player -> new PlayerFigures(
                player.name, player.runs, player.balls, player.fours, player.sixes,
                player.balls == 0 ? 0 : round(player.runs * 100.0 / player.balls))).toList();
        List<BowlerFigures> bowlers = bowlerMap.values().stream().map(player -> new BowlerFigures(
                player.name, player.runs, player.wickets, player.legalBalls,
                player.legalBalls == 0 ? 0 : round(player.runs * 6.0 / player.legalBalls))).toList();
        List<DeliveryView> recent = allDeliveries.stream().skip(Math.max(0, allDeliveries.size() - 12L))
                .map(ball -> new DeliveryView(ball.getId(), ball.getOverNumber(), ball.getBallNumber(),
                        ball.getBatter(), ball.getBowler(), ball.getBatterRuns(), ball.getExtras(),
                        ball.getExtraType(), ball.isWicket(), ball.getRecordedAt()))
                .toList();

        return new MatchView(match.getId(), match.getTitle(), match.getTeamOne(), match.getTeamTwo(),
                match.getVenue(), match.getFormat(), match.getStatus(), match.getBattingTeam(), runs,
                wickets, completedOvers, ballsInOver, match.getTarget(), match.getStriker(),
                match.getNonStriker(), match.getCurrentBowler(), match.getSummary(), runRate,
                batters, bowlers, recent, match.getStartedAt());
    }

    private static boolean isLegal(BallDelivery ball) {
        return !"WIDE".equals(ball.getExtraType()) && !"NO_BALL".equals(ball.getExtraType());
    }

    private static double round(double number) {
        return Math.round(number * 100.0) / 100.0;
    }

    private static class BatterAccumulator {
        private final String name;
        private int runs;
        private int balls;
        private int fours;
        private int sixes;

        private BatterAccumulator(String name) {
            this.name = name;
        }
    }

    private static class BowlerAccumulator {
        private final String name;
        private int runs;
        private int wickets;
        private int legalBalls;

        private BowlerAccumulator(String name) {
            this.name = name;
        }
    }
}