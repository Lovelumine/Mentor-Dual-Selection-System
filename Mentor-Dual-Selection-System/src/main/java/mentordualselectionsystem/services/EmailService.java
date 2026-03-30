package mentordualselectionsystem.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromAddress;

    @Value("${spring.mail.host:localhost}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendSimpleEmail(String toEmail, String subject, String body) {
        ensureMailConfiguration();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(fromAddress);
        mailSender.send(message);
    }

    public void sendHtmlEmail(String toEmail, String subject, String body) {
        ensureMailConfiguration();
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.setFrom(fromAddress);
            mailSender.send(message);
        } catch (MailException e) {
            throw new IllegalStateException("邮件发送失败，请检查 SMTP 配置或网络连接", e);
        } catch (MessagingException e) {
            throw new RuntimeException("发送 HTML 邮件时出现问题", e);
        }
    }

    private void ensureMailConfiguration() {
        boolean usingDefaultHost = "localhost".equalsIgnoreCase(mailHost);
        boolean usingDefaultFrom = "no-reply@example.com".equalsIgnoreCase(fromAddress);
        boolean missingAuthIdentity = !StringUtils.hasText(mailUsername);
        if (usingDefaultHost && usingDefaultFrom && missingAuthIdentity) {
            throw new IllegalStateException("邮件服务尚未配置，无法发送找回密码邮件");
        }
    }

}
