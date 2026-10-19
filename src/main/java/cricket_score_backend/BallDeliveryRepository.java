package cricket_score_backend;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BallDeliveryRepository extends JpaRepository<BallDelivery, Long> {
    List<BallDelivery> findByMatchIdOrderByIdAsc(Long matchId);
}