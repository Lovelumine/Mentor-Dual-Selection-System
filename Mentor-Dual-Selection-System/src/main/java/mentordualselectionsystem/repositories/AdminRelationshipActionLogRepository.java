package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.AdminRelationshipActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminRelationshipActionLogRepository extends JpaRepository<AdminRelationshipActionLog, Long> {
    List<AdminRelationshipActionLog> findAllByOrderByActionDateDesc();

    List<AdminRelationshipActionLog> findAllByOrderByActionDateAsc();

    List<AdminRelationshipActionLog> findByStudentIdOrderByActionDateAsc(Long studentId);

    List<AdminRelationshipActionLog> findByMentorIdOrderByActionDateAsc(Long mentorId);

    void deleteByStudentId(Long studentId);
}
