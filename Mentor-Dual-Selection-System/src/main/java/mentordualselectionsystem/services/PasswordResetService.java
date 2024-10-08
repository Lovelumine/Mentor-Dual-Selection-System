package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.PasswordResetToken;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.PasswordResetTokenRepository;
import mentordualselectionsystem.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService; // 添加 EmailService 的依赖

    @Transactional  // 添加事务支持
    public void createPasswordResetTokenForUser(User user) {
        // 清除之前的令牌
        passwordResetTokenRepository.deleteByUid(user.getUid());

        // 生成新的密码重置令牌
        String token = UUID.randomUUID().toString();
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setUid(user.getUid());
        passwordResetToken.setTokenType(PasswordResetToken.TokenType.EMAIL);
        passwordResetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30)); // 令牌30分钟有效
        passwordResetToken.setUsed(false);

        // 保存令牌到数据库
        passwordResetTokenRepository.save(passwordResetToken);

        // 构建重置链接并发送邮件
        String resetUrl = "http://127.0.0.1:50000/api/auth/reset-password-via-token?token=" + token;
        String subject = "密码重置请求";
        String body = "您好，" + user.getFullName() + "，\n\n"
                + "您请求了密码重置。请点击以下链接重置您的密码：\n"
                + resetUrl + "\n\n"
                + "该链接将在30分钟后过期。\n\n"
                + "如果您没有请求密码重置，请忽略此邮件。\n";

        // 使用 EmailService 发送邮件
        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    public boolean validatePasswordResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        return resetToken.getExpiryDate().isAfter(LocalDateTime.now()) && !resetToken.getUsed();
    }

    public User getUserByPasswordResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        return userRepository.findById(resetToken.getUid())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional  // 添加事务支持
    public void updatePassword(User user, String newPassword, String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (!resetToken.getExpiryDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        // 更新用户密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 标记令牌为已使用
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
