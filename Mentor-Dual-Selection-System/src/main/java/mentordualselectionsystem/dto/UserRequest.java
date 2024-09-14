package mentordualselectionsystem.dto;

public class UserRequest {

    private Long uid;
    private String role;
    private String avatarUrl;
    private String fullName;
    private String email;
    private String username;
    private String password;  // 新增密码字段

    // Getters and setters

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {  // 新增的 getter 方法
        return password;
    }

    public void setPassword(String password) {  // 新增的 setter 方法
        this.password = password;
    }
}
