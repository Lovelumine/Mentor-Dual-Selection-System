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

        // 返回 UserDetails 对象
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())  // 数据库中的加密密码
                .roles(user.getRole().getRoleName())  // 使用单个角色
                .build();
    }

    // 通过用户名获取自定义 User 实体类
    public User getUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // 通过 uid 查找用户
    public User getUserByUid(Long uid) throws UsernameNotFoundException {
        return userRepository.findById(uid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with uid: " + uid));
    }

    // 保存用户信息（包括加密密码）
    public void saveUser(User user) {
        // 对密码进行加密，如果是新用户或密码被修改
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
    }

    // 新增方法：根据角色名称获取 Role 实体
    public Role getRoleByName(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("角色未找到: " + roleName));
    }

    // 新增方法：根据导师 ID 获取该导师的学生列表
    public List<User> getStudentsByMentorId(Long mentorId) {
        return userRepository.findByMentorId(mentorId);
    }

    // 修改后的方法：获取所有导师和学生的关系
    public List<Map<String, Object>> getAllMentorStudentRelationships() {
        List<User> mentors = userRepository.findAllByRole_RoleName("TEACHER"); // 假设角色名为 'TEACHER'
        List<Map<String, Object>> mentorStudentRelationships = new ArrayList<>();

        for (User mentor : mentors) {
            Map<String, Object> mentorMap = new HashMap<>();
            mentorMap.put("uid", mentor.getId());
            mentorMap.put("fullName", mentor.getFullName());
            mentorMap.put("email", mentor.getEmail());

            List<User> students = userRepository.findByMentorId(mentor.getId());
            List<Map<String, Object>> studentList = students.stream().map(student -> {
                Map<String, Object> studentMap = new HashMap<>();
                studentMap.put("uid", student.getId());
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
}
