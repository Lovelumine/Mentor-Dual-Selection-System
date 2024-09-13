package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // 根据导师ID和申请状态查询
    List<Application> findByMentorIdAndStatus(Long mentorId, String status);

    // 根据学生ID和申请状态查询
    List<Application> findByStudentIdAndStatus(Long studentId, String status);


}
