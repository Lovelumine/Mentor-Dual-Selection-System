package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@SecurityRequirement(name = "bearerAuth")  // 全局使用 bearerAuth 安全配置
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    public UserController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @Operation(summary = "获取用户信息", description = "根据 JWT 令牌获取用户的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户信息"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo() {
        try {
            // 从上下文中获取当前用户的认证信息
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();

            // 提取 token 并验证
            String jwtToken = token.replace("Bearer ", "");
            System.out.println("Received token: " + jwtToken);  // 控制台输出接收到的 token

            String uidString = jwtUtils.validateToken(jwtToken);  // 验证 token 并获取 uid
            System.out.println("Validated token, extracted uid: " + uidString);

            Long uid = Long.valueOf(uidString);

            // 根据 uid 获取用户信息
            User user = userService.getUserByUid(uid);
            System.out.println("User found: " + user.getUsername());

            // 构建返回的 JSON 格式，包含用户信息
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);  // 状态码

            Map<String, Object> data = new HashMap<>();
            data.put("uid", user.getId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("fullName", user.getFullName());
            data.put("avatarUrl", user.getAvatarUrl());
            data.put("role", user.getRole().getRoleName());

            responseBody.put("data", data);

            return ResponseEntity.ok(responseBody);

        } catch (NumberFormatException e) {
            System.err.println("Invalid token format, unable to extract uid: " + e.getMessage());
            return buildErrorResponse(401, "token无效或已过期");

        } catch (Exception e) {
            System.err.println("Error while retrieving user info: " + e.getMessage());
            return buildErrorResponse(401, "token无效或已过期");
        }
    }

    // 自定义错误响应格式
    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String message) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);  // 错误状态码

        Map<String, String> errorData = new HashMap<>();
        errorData.put("error", message);
        responseBody.put("data", errorData);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
    }
}
