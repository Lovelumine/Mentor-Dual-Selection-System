package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.UserRepository;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.SignatureException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserService userService, JwtUtils jwtUtils, UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Operation(summary = "获取当前用户信息", description = "根据 JWT 令牌获取当前用户的详细信息。")
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

    @Operation(
            summary = "获取所有用户信息",
            description = "验证用户并返回数据库中的所有用户信息"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回所有用户信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            // 从上下文中获取当前用户的认证信息
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();

            // 提取 token 并验证
            String jwtToken = token.replace("Bearer ", "");
            System.out.println("Received token: " + jwtToken);  // 控制台输出接收到的 token

            // 验证 token 并获取用户 ID
            Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);
            System.out.println("Validated token, extracted uid: " + userId);

            // 检查用户是否存在
            User authenticatedUser = userService.getUserByUid(userId);

            // 获取所有用户
            List<User> users = userRepository.findAll();

            // 构建返回的 JSON 格式，包含用户信息
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);  // 状态码

            // 将用户列表转换为返回格式
            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("uid", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("fullName", user.getFullName());
                userMap.put("avatarUrl", user.getAvatarUrl());
                userMap.put("role", user.getRole().getRoleName());
                return userMap;
            }).toList();

            responseBody.put("data", userList);

            return ResponseEntity.ok(responseBody);

        } catch (NumberFormatException e) {
            System.err.println("Invalid token format, unable to extract uid: " + e.getMessage());
            return buildErrorResponse(401, "token无效或已过期");

        } catch (Exception e) {
            System.err.println("Error while retrieving users: " + e.getMessage());
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
