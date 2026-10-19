package cricket_score_backend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CricketMatchRepository extends JpaRepository<CricketMatch, Long> {
}