package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import mentordualselectionsystem.dto.UserRequest;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.UserRepository;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
            // 获取当前用户的认证信息
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            String uidString = jwtUtils.validateToken(jwtToken);
            Long uid = Long.valueOf(uidString);

            // 获取用户信息
            User user = userService.getUserByUid(uid);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);

            Map<String, Object> data = new HashMap<>();
            data.put("uid", user.getId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("fullName", user.getFullName());
            data.put("avatarUrl", user.getAvatarUrl());
            data.put("role", user.getRole().getRoleName());

            responseBody.put("data", data);
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            return buildErrorResponse(401, "token无效或已过期");
        }
    }

    @Operation(summary = "获取所有用户信息", description = "验证用户并返回数据库中的所有用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回所有用户信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);

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

        } catch (Exception e) {
            return buildErrorResponse(401, "token无效或已过期");
        }
    }

    /**
     * 新建或更新用户
     * 根据传入的 JSON 数据新建或更新用户信息
     */
    @Operation(summary = "新建或更新用户", description = "根据传入的 JSON 数据新建或更新用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功更新或创建用户"),
            @ApiResponse(responseCode = "400", description = "无效的输入参数"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "用户信息",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserRequest.class),
                    examples = {
                            @ExampleObject(name = "新建用户", summary = "新建用户的请求体", value = "{ \"role\": \"STUDENT\", \"fullName\": \"New Student User\", \"email\": \"newstudent@example.com\", \"username\": \"newstudent\" }"),
                            @ExampleObject(name = "更新用户", summary = "更新用户的请求体", value = "{ \"uid\": 1, \"role\": \"ADMIN\", \"fullName\": \"Updated Admin User\", \"email\": \"updatedadmin@example.com\", \"username\": \"admin\" }")
                    }
            )
    )
    @PostMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserRequest userRequest) {
        try {
            Long uid = userRequest.getUid();
            String role = userRequest.getRole();
            String avatarUrl = userRequest.getAvatarUrl();
            String fullName = userRequest.getFullName();
            String email = userRequest.getEmail();
            String username = userRequest.getUsername();
            String password = userRequest.getPassword() != null ? userRequest.getPassword() : "123";  // 默认密码为123

            // 如果用户 ID 为空，则新建用户
            if (uid == null) {
                if (userRepository.findByUsername(username).isPresent()) {
                    return buildErrorResponse(400, "用户名已存在");
                }
                // 新建用户
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setFullName(fullName);
                newUser.setEmail(email);
                newUser.setAvatarUrl(avatarUrl != null ? avatarUrl : "https://minio.lumoxuan.cn/mentor-dual-selection-system/defaultavater.webp");
                newUser.setPassword(password);  // 设置默认密码
                newUser.setRole(userService.getRoleByName(role));  // 设置角色
                userService.saveUser(newUser);
            } else {
                // 更新现有用户
                User existingUser = userRepository.findById(uid)
                        .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

                // 如果用户名已更改，检查新用户名是否已存在
                if (!existingUser.getUsername().equals(username) && userRepository.findByUsername(username).isPresent()) {
                    return buildErrorResponse(400, "用户名已存在");
                }

                existingUser.setUsername(username);
                existingUser.setFullName(fullName);
                existingUser.setEmail(email);
                existingUser.setAvatarUrl(avatarUrl != null ? avatarUrl : "https://minio.lumoxuan.cn/mentor-dual-selection-system/defaultavater.webp");
                existingUser.setPassword(password);
                existingUser.setRole(userService.getRoleByName(role));
                userService.saveUser(existingUser);
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("message", "用户信息更新成功");

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }

    // 自定义错误响应格式
    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String message) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);
        Map<String, String> errorData = new HashMap<>();
        errorData.put("error", message);
        responseBody.put("data", errorData);
        return ResponseEntity.status(HttpStatus.valueOf(code)).body(responseBody);
    }
}
