package mentordualselectionsystem.mysql;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;  // 唯一的重置令牌

    @Column(nullable = false)
    private Long uid;  // 关联用户表的用户ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType tokenType;  // 令牌类型，邮箱或手机找回

    @Column(nullable = false)
    private LocalDateTime expiryDate;  // 令牌过期时间

    @Column(nullable = false)
    private Boolean used = false;  // 令牌是否已使用

    // 枚举类
    public enum TokenType {
        EMAIL,
        PHONE
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }
}
