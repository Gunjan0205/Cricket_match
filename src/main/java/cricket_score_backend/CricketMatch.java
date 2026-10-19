package cricket_score_backend;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "cricket_matches")
public class CricketMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    private String title;
    private String teamOne;
    private String teamTwo;
    private String venue;
    private String format;
    private String status;
    private String battingTeam;
    private String striker;
    private String nonStriker;
    private String currentBowler;
    private Integer target;
    private String summary;
    private Instant startedAt;

    protected CricketMatch() {
    }

    public CricketMatch(String title, String teamOne, String teamTwo, String venue,
            String format, String status, String battingTeam, String striker,
            String nonStriker, String currentBowler, Integer target, String summary) {
        this.title = title;
        this.teamOne = teamOne;
        this.teamTwo = teamTwo;
        this.venue = venue;
        this.format = format;
        this.status = status;
        this.battingTeam = battingTeam;
        this.striker = striker;
        this.nonStriker = nonStriker;
        this.currentBowler = currentBowler;
        this.target = target;
        this.summary = summary;
        this.startedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTeamOne() {
        return teamOne;
    }

    public String getTeamTwo() {
        return teamTwo;
    }

    public String getVenue() {
        return venue;
    }

    public String getFormat() {
        return format;
    }

    public String getStatus() {
        return status;
    }

    public String getBattingTeam() {
        return battingTeam;
    }

    public String getStriker() {
        return striker;
    }

    public String getNonStriker() {
        return nonStriker;
    }

    public String getCurrentBowler() {
        return currentBowler;
    }

    public Integer getTarget() {
        return target;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void updateScore(int runs, int wickets, int legalBalls,
            String nextStriker, String nextNonStriker, String nextBowler) {
        this.striker = nextStriker;
        this.nonStriker = nextNonStriker;
        this.currentBowler = nextBowler;
        int maximumOvers = maximumOvers();
        if (target != null && runs >= target) {
            this.status = "COMPLETED";
            this.summary = battingTeam + " won by " + (10 - wickets) + " wickets";
        } else if (wickets >= 10 || legalBalls >= maximumOvers * 6) {
            this.status = "COMPLETED";
            this.summary = battingTeam + " finished " + runs + "/" + wickets;
        }
    }

    private int maximumOvers() {
        if (format != null && format.matches("(?i).*\\bT\\d+\\b.*")) {
            String overs = format.replaceFirst("(?is).*?\\bT(\\d+)\\b.*", "$1");
            return Integer.parseInt(overs);
        }
        return format != null && format.toUpperCase().contains("ODI") ? 50 : 20;
    }
}