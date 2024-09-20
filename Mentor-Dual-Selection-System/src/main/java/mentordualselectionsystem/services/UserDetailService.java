package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.repositories.UserDetailRepository;
import mentordualselectionsystem.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserDetailService {

    private final UserDetailRepository userDetailRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserDetailService(UserDetailRepository userDetailRepository, UserRepository userRepository) {
        this.userDetailRepository = userDetailRepository;
        this.userRepository = userRepository;
    }

    // 根据 UID 获取用户详细信息
    public UserDetail getUserDetailByUid(Long uid) {
        return userDetailRepository.findByUid(uid);
    }

    // 获取所有教师的详细信息
    public List<UserDetail> getAllTeacherDetails() {
        return userDetailRepository.findAll()
                .stream()
                .filter(detail -> "TEACHER".equals(userRepository.findById(detail.getUid()).get().getRole().getRoleName()))
                .collect(Collectors.toList());
    }

    // 获取所有学生的详细信息
    public List<UserDetail> getAllStudentDetails() {
        return userDetailRepository.findAll()
                .stream()
                .filter(detail -> "STUDENT".equals(userRepository.findById(detail.getUid()).get().getRole().getRoleName()))
                .collect(Collectors.toList());
    }

    // 根据用户名查找用户详细信息
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
