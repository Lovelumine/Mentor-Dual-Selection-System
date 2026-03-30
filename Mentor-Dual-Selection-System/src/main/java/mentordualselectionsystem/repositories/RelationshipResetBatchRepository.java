package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.RelationshipResetBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelationshipResetBatchRepository extends JpaRepository<RelationshipResetBatch, Long> {
    List<RelationshipResetBatch> findAllByOrderByCreatedAtDesc();
}
