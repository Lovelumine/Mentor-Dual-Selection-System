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
                .filter(detail -> userRepository.findById(detail.getUid())
                        .map(user -> "TEACHER".equals(user.getRole().getRoleName()))
                        .orElse(false)) // 如果用户不存在则返回 false，跳过此条记录
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

    // 创建用户详细信息
    public UserDetail createUserDetail(UserDetail userDetail) {
        return userDetailRepository.save(userDetail);
    }

    // 修改已有用户的详细信息
    public UserDetail updateUserDetail(Long uid, UserDetail userDetail) {
        UserDetail existingDetail = userDetailRepository.findByUid(uid);
        if (existingDetail != null) {
            // 更新字段
            existingDetail.setPhotoUrl(userDetail.getPhotoUrl());
            existingDetail.setTeacherPosition(userDetail.getTeacherPosition());
            existingDetail.setResearchDirection(userDetail.getResearchDirection());
            existingDetail.setProfessionalDirection(userDetail.getProfessionalDirection());
            existingDetail.setResume(userDetail.getResume());
            existingDetail.setNetid(userDetail.getNetid());
            existingDetail.setStudentClass(userDetail.getStudentClass());
            existingDetail.setStudentGrade(userDetail.getStudentGrade());
            return userDetailRepository.save(existingDetail);
        } else {
            // 如果不存在，则可以选择创建新用户详细信息
            userDetail.setUid(uid);  // 确保UID正确
            return userDetailRepository.save(userDetail);
        }
    }
}
