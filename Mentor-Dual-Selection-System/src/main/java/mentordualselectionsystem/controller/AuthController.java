package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.PasswordResetService;
import mentordualselectionsystem.services.SecurityRateLimitService;
import mentordualselectionsystem.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "认证相关接口", description = "用户身份验证及密码管理接口")
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final PasswordResetService passwordResetService;
    private final SecurityRateLimitService securityRateLimitService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, PasswordResetService passwordResetService, SecurityRateLimitService securityRateLimitService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.passwordResetService = passwordResetService;
        this.securityRateLimitService = securityRateLimitService;
    }

    @Operation(summary = "用户登录", description = "验证用户身份并返回 JWT 令牌。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal(); // 获取用户信息
        String token = jwtUtils.generateToken(user.getUid());  // 使用用户的 uid 生成 token

        // 使用 formatResponse 构建返回格式
        Map<String, Object> response = formatResponse(200, Map.of("token", token));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "重置密码", description = "验证旧密码并更新为新密码。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密码重置成功"),
            @ApiResponse(responseCode = "400", description = "新密码和确认密码不匹配"),
            @ApiResponse(responseCode = "401", description = "旧密码不正确"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的 token 无效或已过期"));
        }

        String username = authentication.getName();
        String oldPassword = resetPasswordRequest.getOldPassword();
        String newPassword = resetPasswordRequest.getNewPassword();
        String confirmPassword = resetPasswordRequest.getConfirmPassword();

        User user;
        try {
            // 尝试获取用户信息，如果用户不存在会抛出异常
            user = userService.getUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            // 捕获用户名不存在的异常并返回 404 错误信息
            return ResponseEntity.status(404).body(formatResponse(404, "用户不存在"));
        }

        // 检查新密码和确认密码是否匹配
        if (!newPassword.equals(confirmPassword)) {
            // 返回错误信息，新密码和确认密码不匹配
            return ResponseEntity.status(400).body(formatResponse(400, "新密码和确认密码不匹配"));
        }

        if (!isStrongPassword(newPassword)) {
            return ResponseEntity.status(400).body(formatResponse(400, "新密码长度至少为 8 位"));
        }

        // 验证旧密码
        if (!userService.checkPassword(username, oldPassword)) {
            // 返回错误信息，旧密码不正确
            return ResponseEntity.status(401).body(formatResponse(401, "旧密码不正确"));
        }

        // 更新密码
        userService.updatePassword(user.getUid(), newPassword);

        // 生成新的 JWT Token
        String token = jwtUtils.generateToken(user.getUid());

        // 返回 {code: 200, token: ""} 格式的响应
        Map<String, Object> response = formatResponse(200, Map.of("token", token));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "忘记密码", description = "发送密码重置链接到用户邮箱。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "重置链接已发送至用户邮箱"),
            @ApiResponse(responseCode = "404", description = "邮箱对应的用户不存在")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email, HttpServletRequest request) {
        String genericMessage = "如果该邮箱已绑定账号，密码重置邮件将发送到对应邮箱";
        try {
            securityRateLimitService.checkForgotPasswordAllowed(email, resolveClientIp(request));
            User user = userService.getUserByEmail(email); // 修改为通过邮箱查找用户
            passwordResetService.createPasswordResetTokenForUser(user);  // 生成并发送重置令牌
            return ResponseEntity.ok(formatResponse(200, genericMessage));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.ok(formatResponse(200, genericMessage));
        } catch (IllegalStateException e) {
            if ("请求过于频繁，请稍后再试".equals(e.getMessage())) {
                return ResponseEntity.status(429).body(formatResponse(429, e.getMessage()));
            }
            log.warn("forgot-password mail sending skipped or failed for email={}: {}", email, e.getMessage());
            return ResponseEntity.ok(formatResponse(200, genericMessage));
        } catch (RuntimeException e) {
            log.warn("forgot-password unexpected error for email={}", email, e);
            return ResponseEntity.ok(formatResponse(200, genericMessage));
        }
    }

    @Operation(summary = "通过重置令牌重置密码", description = "用户通过邮件中的重置令牌重置密码。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密码重置成功"),
            @ApiResponse(responseCode = "400", description = "无效或过期的重置令牌")
    })
    @PostMapping("/reset-password-via-token")
    public ResponseEntity<?> resetPasswordViaToken(@RequestParam String token, @RequestParam String newPassword, HttpServletRequest request) {
        try {
            securityRateLimitService.checkResetPasswordAllowed(token, resolveClientIp(request));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(429).body(formatResponse(429, e.getMessage()));
        }

        if (!passwordResetService.validatePasswordResetToken(token)) {
            return ResponseEntity.badRequest().body(formatResponse(400, "无效或过期的重置令牌"));
        }

        if (!isStrongPassword(newPassword)) {
            return ResponseEntity.badRequest().body(formatResponse(400, "新密码长度至少为 8 位"));
        }

        User user = passwordResetService.getUserByPasswordResetToken(token); // 通过令牌获取用户
        passwordResetService.updatePassword(user, newPassword, token);

        return ResponseEntity.ok(formatResponse(200, "密码已重置"));
    }

    private boolean isStrongPassword(String password) {
        return password != null && password.trim().length() >= 8;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    // 新的通用响应格式化方法
    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
