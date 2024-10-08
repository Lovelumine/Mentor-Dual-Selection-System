package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.Role;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.RoleRepository;
import mentordualselectionsystem.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getRoleName())
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

    public void saveUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
    }

    public Role getRoleByName(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("角色未找到: " + roleName));
    }

    public List<User> getStudentsByMentorId(Long mentorId) {
        return userRepository.findByMentorId(mentorId);
    }

    public List<Map<String, Object>> getAllMentorStudentRelationships() {
        List<User> mentors = userRepository.findAllByRole_RoleName("TEACHER");
        List<Map<String, Object>> mentorStudentRelationships = new ArrayList<>();

        for (User mentor : mentors) {
            Map<String, Object> mentorMap = new HashMap<>();
            mentorMap.put("uid", mentor.getUid());
            mentorMap.put("fullName", mentor.getFullName());
            mentorMap.put("email", mentor.getEmail());

            List<User> students = userRepository.findByMentorId(mentor.getUid());
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
        return userRepository.findAllByRole_RoleName("STUDENT");
    }

    // 根据名字模糊搜索学生
    public List<User> searchStudentsByName(String fullName) {
        return userRepository.findAll().stream()
                .filter(student -> student.getFullName().contains(fullName))
                .collect(Collectors.toList());
    }

    // 获取所有老师
    public List<User> getAllTeachers() {
        return userRepository.findAllByRole_RoleName("TEACHER");
    }

    // 根据名字模糊搜索老师
    public List<User> searchTeachersByName(String fullName) {
        return userRepository.findAll().stream()
                .filter(teacher -> teacher.getFullName().contains(fullName))
                .collect(Collectors.toList());
    }
}
