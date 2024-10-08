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

        // 生成新的验证码
        String token = UUID.randomUUID().toString().substring(0, 6);  // 生成6位验证码
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setUid(user.getUid());
        passwordResetToken.setTokenType(PasswordResetToken.TokenType.EMAIL);
        passwordResetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30)); // 令牌30分钟有效
        passwordResetToken.setUsed(false);

        // 保存令牌到数据库
        passwordResetTokenRepository.save(passwordResetToken);

        // 构建HTML邮件内容并发送邮件
        String subject = "密码重置请求 - 中山大学导师双选系统";
        String body = "<html><body>"
                + "<p>您好，" + user.getFullName() + "，</p>"
                + "<p>您请求了密码重置。请使用以下验证码来重置您的密码：</p>"
                + "<h3 style='color: blue;'>" + token + "</h3>"
                + "<p>该验证码将在30分钟后过期。</p>"
                + "<p>如果您没有请求密码重置，请忽略此邮件。</p>"
                + "<hr>"
                + "<p>中山大学导师双选系统</p>"
                + "<p style='font-size: small; color: gray;'>由永新县爱荧科技有限责任公司提供技术支持</p>"
                + "</body></html>";

        // 使用 EmailService 发送HTML邮件
        emailService.sendHtmlEmail(user.getEmail(), subject, body);
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
