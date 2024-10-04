package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserService;
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
@Tag(name = "认证相关接口")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
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
        String token = jwtUtils.generateToken(Long.valueOf(user.getUid().toString()));  // 使用用户的 uid 生成 token

        // 使用 formatResponse 构建返回格式
        Map<String, Object> response = formatResponse(200, Map.of("token", token));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "重置密码", description = "验证旧密码并更新为新密码。")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        String username = resetPasswordRequest.getUsername();
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

    // 新的通用响应格式化方法
    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
