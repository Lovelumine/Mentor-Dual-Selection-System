package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // 根据 mentorId 查询已选择该导师的所有学生
    List<User> findByMentorId(Long mentorId);
}
