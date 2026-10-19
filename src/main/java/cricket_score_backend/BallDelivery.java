package cricket_score_backend;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ball_deliveries")
public class BallDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CricketMatch match;

    private int inningsNumber;
    private int overNumber;
    private int ballNumber;
    private String batter;
    private String bowler;
    private int batterRuns;
    private int extras;
    private String extraType;
    private boolean wicket;
    private Instant recordedAt;

    protected BallDelivery() {
    }

    public BallDelivery(CricketMatch match, int inningsNumber, int overNumber, int ballNumber,
            String batter, String bowler, int batterRuns, int extras,
            String extraType, boolean wicket) {
        this.match = match;
        this.inningsNumber = inningsNumber;
        this.overNumber = overNumber;
        this.ballNumber = ballNumber;
        this.batter = batter;
        this.bowler = bowler;
        this.batterRuns = batterRuns;
        this.extras = extras;
        this.extraType = extraType;
        this.wicket = wicket;
        this.recordedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public int getInningsNumber() {
        return inningsNumber;
    }

    public int getOverNumber() {
        return overNumber;
    }

    public int getBallNumber() {
        return ballNumber;
    }

    public String getBatter() {
        return batter;
    }

    public String getBowler() {
        return bowler;
    }

    public int getBatterRuns() {
        return batterRuns;
    }

    public int getExtras() {
        return extras;
    }

    public String getExtraType() {
        return extraType;
    }

    public boolean isWicket() {
        return wicket;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}