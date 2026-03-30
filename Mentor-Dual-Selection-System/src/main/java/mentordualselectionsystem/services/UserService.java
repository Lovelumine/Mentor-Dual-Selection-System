package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.Role;
import mentordualselectionsystem.mysql.AdminRelationshipActionLog;
import mentordualselectionsystem.mysql.Application;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.AdminRelationshipActionLogRepository;
import mentordualselectionsystem.repositories.ApplicationRepository;
import mentordualselectionsystem.repositories.PasswordResetTokenRepository;
import mentordualselectionsystem.repositories.RelationshipResetBatchSnapshotRepository;
import mentordualselectionsystem.repositories.RoleRepository;
import mentordualselectionsystem.repositories.UserDetailRepository;
import mentordualselectionsystem.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private static final String ACTION_ADMIN_STUDENT_ARCHIVE_UNBIND = "ADMIN_STUDENT_ARCHIVE_UNBIND";
    private static final String ACTION_ADMIN_TEACHER_ARCHIVE_UNBIND = "ADMIN_TEACHER_ARCHIVE_UNBIND";
    private static final String DEFAULT_STUDENT_ARCHIVE_REASON = "学生已转出，解除当前导师关系并停用账号";
    private static final String DEFAULT_ARCHIVED_PENDING_APPLICATION_REASON = "学生已转出，申请自动关闭";
    private static final String DEFAULT_TEACHER_ARCHIVE_REASON = "导师已停用，解除当前师生关系并保留历史记录";
    private static final String DEFAULT_TEACHER_ARCHIVE_PENDING_APPLICATION_REASON = "导师已停用，申请自动关闭";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AdminRelationshipActionLogRepository adminRelationshipActionLogRepository;

    @Autowired
    private RelationshipResetBatchSnapshotRepository relationshipResetBatchSnapshotRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new DisabledException("账号已停用");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getRoleName())
                .disabled(!user.isEnabled())
                .build();
    }

    public User getUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // 新增通过邮箱查找用户的方法
    public User getUserByEmail(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public User getUserByUid(Long uid) throws UsernameNotFoundException {
        return userRepository.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with uid: " + uid));
    }

    public List<User> getUsersByUids(Collection<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return Collections.emptyList();
        }
        return userRepository.findAllById(new LinkedHashSet<>(uids));
    }

    public void saveUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty() && !isEncodedPassword(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
    }

    private boolean isEncodedPassword(String password) {
        return password.matches("^\\$2[aby]\\$.{56}$");
    }

    public Role getRoleByName(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("角色未找到: " + roleName));
    }

    public List<User> getStudentsByMentorId(Long mentorId) {
        return userRepository.findByMentorIdAndActiveTrue(mentorId);
    }

    public List<Map<String, Object>> getAllMentorStudentRelationships() {
        List<User> mentors = userRepository.findAllByRole_RoleNameAndActiveTrue("TEACHER");
        List<User> studentsWithMentors = userRepository.findByMentorIdIsNotNullAndActiveTrue();
        Map<Long, List<User>> studentsByMentorId = studentsWithMentors.stream()
                .collect(Collectors.groupingBy(User::getMentorId));
        List<Map<String, Object>> mentorStudentRelationships = new ArrayList<>();

        for (User mentor : mentors) {
            Map<String, Object> mentorMap = new HashMap<>();
            mentorMap.put("uid", mentor.getUid());
            mentorMap.put("fullName", mentor.getFullName());
            mentorMap.put("email", mentor.getEmail());

            List<User> students = studentsByMentorId.getOrDefault(mentor.getUid(), Collections.emptyList());
            List<Map<String, Object>> studentList = students.stream().map(student -> {
                Map<String, Object> studentMap = new HashMap<>();
                studentMap.put("uid", student.getUid());
                studentMap.put("fullName", student.getFullName());
                studentMap.put("email", student.getEmail());
                studentMap.put("username", student.getUsername());
                return studentMap;
            }).collect(Collectors.toList());

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("mentor", mentorMap);
            responseMap.put("students", studentList);

            mentorStudentRelationships.add(responseMap);
        }

        return mentorStudentRelationships;
    }

    public boolean checkPassword(String username, String oldPassword) {
        User user = getUserByUsername(username);
        return passwordEncoder.matches(oldPassword, user.getPassword());
    }

    public void updatePassword(Long uid, String newPassword) {
        User user = getUserByUid(uid);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 获取所有学生
    public List<User> getAllStudents() {
        return userRepository.findAllByRole_RoleNameAndActiveTrue("STUDENT");
    }

    // 根据名字模糊搜索学生
    public List<User> searchStudentsByName(String fullName) {
        return userRepository.findByFullNameContainingAndRole_RoleNameAndActiveTrue(fullName, "STUDENT");
    }

    // 获取所有老师
    public List<User> getAllTeachers() {
        return userRepository.findAllByRole_RoleNameAndActiveTrue("TEACHER");
    }

    // 根据名字模糊搜索老师
    public List<User> searchTeachersByName(String fullName) {
        return userRepository.findByFullNameContainingAndRole_RoleNameAndActiveTrue(fullName, "TEACHER");
    }

    @Transactional
    public void archiveStudentByUid(Long uid, User adminOperator, String reason) {
        User student = getUserByUid(uid);
        if (!"STUDENT".equals(student.getRole().getRoleName())) {
            throw new IllegalArgumentException("仅允许转出学生账号");
        }

        if (!student.isEnabled()) {
            throw new IllegalArgumentException("该学生账号已停用");
        }

        String normalizedReason = normalizeReason(reason, DEFAULT_STUDENT_ARCHIVE_REASON);
        Instant actionTime = Instant.now();
        Long mentorId = student.getMentorId();
        User mentor = null;
        if (mentorId != null) {
            mentor = userRepository.findById(mentorId)
                    .orElseThrow(() -> new IllegalArgumentException("学生关联的导师不存在"));

            student.setMentorId(null);
            adminRelationshipActionLogRepository.save(buildArchiveActionLog(
                    student,
                    mentor,
                    adminOperator,
                    normalizedReason,
                    actionTime
            ));
        }

        List<Application> pendingApplications = applicationRepository.findByStudentIdAndStatus(uid, "PENDING");
        for (Application application : pendingApplications) {
            application.setStatus("REJECTED");
            application.setRejectionReason(DEFAULT_ARCHIVED_PENDING_APPLICATION_REASON);
            application.setDecisionDate(actionTime);
        }
        if (!pendingApplications.isEmpty()) {
            applicationRepository.saveAll(pendingApplications);
        }

        student.setActive(false);
        student.setDeactivatedAt(actionTime);
        student.setDeactivatedReason(normalizedReason);
        student.setDeactivatedById(adminOperator.getUid());
        userRepository.save(student);

        if (mentor != null) {
            mentor.setAcceptedStudents((int) userRepository.countByMentorIdAndActiveTrue(mentorId));
            userRepository.save(mentor);
        }
    }

    @Transactional
    public Map<String, Object> archiveTeachersByUids(List<Long> teacherIds, User adminOperator, String reason) {
        LinkedHashSet<Long> uniqueTeacherIds = teacherIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(teacherIds);
        if (uniqueTeacherIds.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一名导师");
        }

        List<User> teachers = userRepository.findAllById(uniqueTeacherIds);
        if (teachers.size() != uniqueTeacherIds.size()) {
            throw new IllegalArgumentException("存在无效的导师记录");
        }

        Instant actionTime = Instant.now();
        String normalizedReason = normalizeReason(reason, DEFAULT_TEACHER_ARCHIVE_REASON);

        List<User> studentsToUpdate = new ArrayList<>();
        List<AdminRelationshipActionLog> actionLogs = new ArrayList<>();
        List<Application> pendingApplicationsToReject = new ArrayList<>();
        int affectedStudentCount = 0;
        int rejectedPendingCount = 0;

        for (User teacher : teachers) {
            if (!"TEACHER".equals(teacher.getRole().getRoleName())) {
                throw new IllegalArgumentException("仅允许停用导师账号");
            }
            if (!teacher.isEnabled()) {
                throw new IllegalArgumentException("所选导师中包含已停用账号，请刷新后重试");
            }

            List<User> activeStudents = userRepository.findByMentorIdAndActiveTrue(teacher.getUid());
            for (User student : activeStudents) {
                student.setMentorId(null);
                studentsToUpdate.add(student);
                actionLogs.add(buildTeacherArchiveActionLog(
                        student,
                        teacher,
                        adminOperator,
                        normalizedReason,
                        actionTime
                ));
            }
            affectedStudentCount += activeStudents.size();

            List<Application> pendingApplications = applicationRepository.findByMentorIdAndStatus(teacher.getUid(), "PENDING");
            for (Application application : pendingApplications) {
                application.setStatus("REJECTED");
                application.setRejectionReason(DEFAULT_TEACHER_ARCHIVE_PENDING_APPLICATION_REASON);
                application.setDecisionDate(actionTime);
            }
            pendingApplicationsToReject.addAll(pendingApplications);
            rejectedPendingCount += pendingApplications.size();

            teacher.setAcceptedStudents(0);
            teacher.setActive(false);
            teacher.setDeactivatedAt(actionTime);
            teacher.setDeactivatedReason(normalizedReason);
            teacher.setDeactivatedById(adminOperator.getUid());
        }

        if (!studentsToUpdate.isEmpty()) {
            userRepository.saveAll(studentsToUpdate);
        }
        if (!pendingApplicationsToReject.isEmpty()) {
            applicationRepository.saveAll(pendingApplicationsToReject);
        }
        if (!actionLogs.isEmpty()) {
            adminRelationshipActionLogRepository.saveAll(actionLogs);
        }
        userRepository.saveAll(teachers);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("archivedTeacherCount", teachers.size());
        result.put("archivedTeacherIds", new ArrayList<>(uniqueTeacherIds));
        result.put("affectedStudentCount", affectedStudentCount);
        result.put("rejectedPendingCount", rejectedPendingCount);
        result.put("reason", normalizedReason);
        return result;
    }

    private String normalizeReason(String reason, String defaultReason) {
        if (reason == null || reason.trim().isEmpty()) {
            return defaultReason;
        }
        return reason.trim();
    }

    private AdminRelationshipActionLog buildArchiveActionLog(
            User student,
            User mentor,
            User adminOperator,
            String actionReason,
            Instant actionTime
    ) {
        return buildRelationshipActionLog(
                student,
                mentor,
                adminOperator,
                ACTION_ADMIN_STUDENT_ARCHIVE_UNBIND,
                actionReason,
                actionTime
        );
    }

    private AdminRelationshipActionLog buildTeacherArchiveActionLog(
            User student,
            User mentor,
            User adminOperator,
            String actionReason,
            Instant actionTime
    ) {
        return buildRelationshipActionLog(
                student,
                mentor,
                adminOperator,
                ACTION_ADMIN_TEACHER_ARCHIVE_UNBIND,
                actionReason,
                actionTime
        );
    }

    private AdminRelationshipActionLog buildRelationshipActionLog(
            User student,
            User mentor,
            User adminOperator,
            String actionType,
            String actionReason,
            Instant actionTime
    ) {
        AdminRelationshipActionLog actionLog = new AdminRelationshipActionLog();
        actionLog.setStudentId(student.getUid());
        actionLog.setMentorId(mentor.getUid());
        actionLog.setAdminId(adminOperator.getUid());
        actionLog.setStudentNameSnapshot(student.getFullName());
        actionLog.setMentorNameSnapshot(mentor.getFullName());
        actionLog.setAdminNameSnapshot(adminOperator.getFullName());
        actionLog.setActionType(actionType);
        actionLog.setActionReason(actionReason);
        actionLog.setActionDate(actionTime);
        return actionLog;
    }
}
