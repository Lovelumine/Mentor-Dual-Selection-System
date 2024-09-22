package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "账号信息相关接口")
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
            data.put("uid", user.getUid());
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

    @Operation(summary = "获取所有用户账号信息", description = "验证用户并返回数据库中的所有用户账号信息")
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
                userMap.put("uid", user.getUid());
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

    @Operation(summary = "获取所有老师的脱敏账号信息", description = "学生预览导师用，验证用户并返回数据库中的老师的脱敏账号信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回所有老师信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/allteacher")
    public ResponseEntity<?> getAllTeachers() {
        try {
            // 从数据库中获取所有用户
            List<User> users = userRepository.findAll();

            // 筛选出角色为 "TEACHER" 的用户
            List<Map<String, Object>> teacherList = users.stream()
                    .filter(user -> "TEACHER".equals(user.getRole().getRoleName()))
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", user.getUid());
                        userMap.put("fullName", user.getFullName());
                        userMap.put("avatarUrl", user.getAvatarUrl());
                        return userMap;
                    })
                    .toList();

            // 构建响应体
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("data", teacherList);

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

    // 新增方法：根据 UID 获取指定用户的账号信息
    @Operation(
            summary = "获取指定用户的账号信息",
            description = "根据用户的 UID 获取账号信息，仅管理员或导师可用。",
            parameters = {
                    @Parameter(name = "uid", description = "用户的 UID", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户信息"),
            @ApiResponse(responseCode = "400", description = "无效的 UID"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息"),
            @ApiResponse(responseCode = "404", description = "用户不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/get")
    public ResponseEntity<?> getUserByUid(@RequestParam Long uid) {
        try {
            // 获取当前用户的认证信息
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);

            // 获取当前用户信息
            User currentUser = userService.getUserByUid(currentUid);
            String roleName = currentUser.getRole().getRoleName();

            // 仅管理员或导师可访问
            if (!"ADMIN".equals(roleName) && !"TEACHER".equals(roleName)) {
                return buildErrorResponse(403, "无权限访问该信息");
            }

            // 获取指定 UID 的用户信息
            User user = userService.getUserByUid(uid);
            if (user == null) {
                return buildErrorResponse(404, "用户不存在");
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);

            Map<String, Object> data = new HashMap<>();
            data.put("uid", user.getUid());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("fullName", user.getFullName());
            data.put("avatarUrl", user.getAvatarUrl());
            data.put("role", user.getRole().getRoleName());

            responseBody.put("data", data);
            return ResponseEntity.ok(responseBody);

        } catch (NumberFormatException e) {
            return buildErrorResponse(400, "无效的 UID");
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

        HttpStatus status;
        switch (code) {
            case 400:
                status = HttpStatus.BAD_REQUEST;
                break;
            case 401:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case 403:
                status = HttpStatus.FORBIDDEN;
                break;
            case 404:
                status = HttpStatus.NOT_FOUND;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(status).body(responseBody);
    }
}
