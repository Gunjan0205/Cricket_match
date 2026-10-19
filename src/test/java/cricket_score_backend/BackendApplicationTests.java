package cricket_score_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cricket_score_backend.ScoreModels.DeliveryRequest;
import cricket_score_backend.ScoreModels.MatchView;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:cricketscoretest;DB_CLOSE_DELAY=-1")
class BackendApplicationTests {
	@Autowired
	private ScoreService scores;

	@Test
	void contextLoads() {
	}

	@Test
	void recordingDeliveryUpdatesScorecard() {
		List<MatchView> matches = scores.listMatches();
		assertTrue(matches.size() >= 1);
		MatchView before = matches.get(0);

		MatchView after = scores.addDelivery(before.id(), new DeliveryRequest(
				before.striker(), before.currentBowler(), 2, 0, null, false, null));

		assertEquals(before.runs() + 2, after.runs());
		assertTrue(after.batters().stream()
				.anyMatch(player -> player.name().equals(before.striker()) && player.runs() >= 2));
	}

}
