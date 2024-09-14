package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.Role;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.RoleRepository;
import mentordualselectionsystem.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
}
