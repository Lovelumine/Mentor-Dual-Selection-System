package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDetailRepository extends JpaRepository<UserDetail, Long> {
    UserDetail findByUid(Long uid);

    // 自定义方法，用于查找一组 UID 的用户详细信息
    List<UserDetail> findAllByUidIn(List<Long> uids);
}
