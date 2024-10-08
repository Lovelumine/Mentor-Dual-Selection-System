package mentordualselectionsystem.mysql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long uid;

    private String username;

    private String password;

    private String email;

    @Column(name = "full_name", nullable = false) // 用户全名字段
    private String fullName;

    @Column(name = "avatar_url") // 头像 URL 地址字段
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.EAGER) // 多对一，用户和角色之间的关系
    @JoinColumn(name = "role_id")  // users 表中 role_id 外键列，指向 roles 表
    private Role role;

    @ColumnDefault("0")
    @Column(name = "accepted_students")
    private Integer acceptedStudents; // 接收学生数

    @Column(name = "mentor_id")
    private Long mentorId; // 导师 ID，学生与导师之间的关系

    @Transient
    private UserDetail userDetail; // 非数据库映射字段，用于在 User 中存储关联的 UserDetail 信息

    // Getter 和 Setter 方法
    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getAcceptedStudents() {
        return acceptedStudents;
    }

    public void setAcceptedStudents(Integer acceptedStudents) {
        this.acceptedStudents = acceptedStudents;
    }

    public Long getMentorId() {
        return mentorId;
    }

    public void setMentorId(Long mentorId) {
        this.mentorId = mentorId;
    }

    public UserDetail getUserDetail() {
        return userDetail;
    }

    public void setUserDetail(UserDetail userDetail) {
        this.userDetail = userDetail;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(role.getRoleName()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
