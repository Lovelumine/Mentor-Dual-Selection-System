package mentordualselectionsystem.controller;

import mentordualselectionsystem.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    // 发送测试邮件的API
    @PostMapping("/send")
    public String sendEmail(
            @RequestParam String toEmail,
            @RequestParam String subject,
            @RequestParam String body) {

        try {
            emailService.sendSimpleEmail(toEmail, subject, body);
            return "Email sent successfully to " + toEmail;
        } catch (Exception e) {
            return "Error sending email: " + e.getMessage();
        }
    }
}
