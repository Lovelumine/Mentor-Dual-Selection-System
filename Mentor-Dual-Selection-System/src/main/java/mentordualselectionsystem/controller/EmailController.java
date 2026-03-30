package mentordualselectionsystem.controller;

import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private mentordualselectionsystem.services.UserService userService;

    // 发送测试邮件的API
    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(
            Authentication authentication,
            @RequestParam String toEmail,
            @RequestParam String subject,
            @RequestParam String body) {

        try {
            User currentUser = requireAdmin(authentication);
            if (currentUser == null) {
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, 401, "当前的token无效");
            }

            emailService.sendSimpleEmail(toEmail, subject, body);
            return ResponseEntity.ok(formatResponse(200, "Email sent successfully to " + toEmail));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, 403, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 500, "发送邮件失败");
        }
    }

    private User requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return null;
        }

        String jwtToken = authentication.getCredentials().toString();
        Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);
        User currentUser = userService.getUserByUid(currentUid);
        if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
            throw new IllegalArgumentException("仅管理员可以发送测试邮件");
        }
        return currentUser;
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(formatResponse(code, message));
    }

    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
