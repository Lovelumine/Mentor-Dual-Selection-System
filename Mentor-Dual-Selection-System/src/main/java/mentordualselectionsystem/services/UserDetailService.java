package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.repositories.UserDetailRepository;
import mentordualselectionsystem.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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

    public Map<Long, UserDetail> getUserDetailMapByUids(Collection<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return Collections.emptyMap();
        }

        return userDetailRepository.findAllByUidIn(new java.util.ArrayList<>(new LinkedHashSet<>(uids)))
                .stream()
                .collect(Collectors.toMap(UserDetail::getUid, Function.identity()));
    }

    // 获取所有教师的详细信息
    public List<UserDetail> getAllTeacherDetails() {
        List<Long> teacherUids = userRepository.findAllByRole_RoleName("TEACHER").stream()
                .map(User::getUid)
                .toList();

        return userDetailRepository.findAllByUidIn(teacherUids);
    }


    // 获取所有学生的详细信息
    public List<UserDetail> getAllStudentDetails() {
        List<Long> studentUids = userRepository.findAllByRole_RoleName("STUDENT").stream()
                .map(User::getUid)
                .toList();

        return userDetailRepository.findAllByUidIn(studentUids);
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
            // 仅覆盖本次明确传入的字段，避免部分更新时把其它详情误清空。
            if (userDetail.getPhotoUrl() != null) {
                existingDetail.setPhotoUrl(userDetail.getPhotoUrl());
            }
            if (userDetail.getTeacherPosition() != null) {
                existingDetail.setTeacherPosition(userDetail.getTeacherPosition());
            }
            if (userDetail.getResearchDirection() != null) {
                existingDetail.setResearchDirection(userDetail.getResearchDirection());
            }
            if (userDetail.getProfessionalDirection() != null) {
                existingDetail.setProfessionalDirection(userDetail.getProfessionalDirection());
            }
            if (userDetail.getResume() != null) {
                existingDetail.setResume(userDetail.getResume());
            }
            if (userDetail.getNetid() != null) {
                existingDetail.setNetid(userDetail.getNetid());
            }
            if (userDetail.getStudentClass() != null) {
                existingDetail.setStudentClass(userDetail.getStudentClass());
            }
            if (userDetail.getStudentGrade() != null) {
                existingDetail.setStudentGrade(userDetail.getStudentGrade());
            }
            return userDetailRepository.save(existingDetail);
        } else {
            // 如果不存在，则可以选择创建新用户详细信息
            userDetail.setUid(uid);  // 确保UID正确
            return userDetailRepository.save(userDetail);
        }
    }
}
