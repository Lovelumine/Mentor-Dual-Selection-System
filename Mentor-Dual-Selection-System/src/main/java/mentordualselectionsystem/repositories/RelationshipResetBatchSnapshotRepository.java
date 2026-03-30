package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.RelationshipResetBatchSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelationshipResetBatchSnapshotRepository extends JpaRepository<RelationshipResetBatchSnapshot, Long> {
    List<RelationshipResetBatchSnapshot> findByBatchIdOrderByIdAsc(Long batchId);

    void deleteByStudentId(Long studentId);
}
