package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDetailRepository extends JpaRepository<UserDetail, Long> {
    // 根据 UID 查询用户详细信息
    UserDetail findByUid(Long uid);
}
