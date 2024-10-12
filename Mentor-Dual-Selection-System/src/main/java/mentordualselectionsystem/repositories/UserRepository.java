package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 根据用户名查找用户
    Optional<User> findByUsername(String username);

    // 新增：根据邮箱查找用户
    Optional<User> findByEmail(String email);  // 添加此方法

    // 根据 mentorId 查询已选择该导师的所有学生
    List<User> findByMentorId(Long mentorId);

    // 根据角色名称查询所有用户
    List<User> findAllByRole_RoleName(String roleName);
}
