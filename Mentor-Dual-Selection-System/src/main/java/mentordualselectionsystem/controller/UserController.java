package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.dto.StudentArchiveRequest;
import mentordualselectionsystem.dto.TeacherArchiveRequest;
import mentordualselectionsystem.dto.UserRequest;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.repositories.UserRepository;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserDetailService;
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
    private static final String DEFAULT_CARD_BACKGROUND_URL = "https://minio.lumoxuan.cn/mentor-dual-selection-system/admin/%E7%BA%AA%E8%A1%8C%EF%BC%8E%E6%8D%95%E9%A3%8E.png";
    private static final String LEGACY_DEFAULT_AVATAR_URL = "https://minio.lumoxuan.cn/mentor-dual-selection-system/defaultavater.webp";

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final UserDetailService userDetailService;  // 添加 UserDetailService 声明


    @Autowired
    public UserController(UserService userService, JwtUtils jwtUtils, UserRepository userRepository, UserDetailService userDetailService) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.userDetailService = userDetailService;
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
                userMap.put("active", user.isEnabled());
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
            List<User> users = userService.getAllTeachers();

            // 筛选出角色为 "TEACHER" 的用户
            List<Map<String, Object>> teacherList = users.stream()
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
            boolean created = uid == null;
            Long affectedUid;
            String role = userRequest.getRole();
            String avatarUrl = userRequest.getAvatarUrl();
            String normalizedAvatarUrl = normalizeAvatarUrl(avatarUrl);
            String fullName = userRequest.getFullName();
            String email = userRequest.getEmail();
            String username = userRequest.getUsername();
            String password = userRequest.getPassword();

            if (uid == null) {
                // 新增用户
                if (userRepository.findByUsername(username).isPresent()) {
                    return buildErrorResponse(400, "用户名已存在");
                }

                User newUser = new User();
                newUser.setUsername(username);
                newUser.setFullName(fullName);
                newUser.setEmail(email);
                newUser.setAvatarUrl(normalizedAvatarUrl);
                newUser.setPassword(password != null && !password.isBlank() ? password : "123");
                newUser.setActive(true);
                newUser.setDeactivatedAt(null);
                newUser.setDeactivatedReason(null);
                newUser.setDeactivatedById(null);
                newUser.setRole(userService.getRoleByName(role));
                userService.saveUser(newUser);
                affectedUid = newUser.getUid();

                // 如果是新增学生，则必须提供年级并创建学生详情
                if ("STUDENT".equals(role)) {
                    String grade = userRequest.getGrade();
                    if (grade == null || grade.isEmpty()) {
                        return buildErrorResponse(400, "学生角色必须提供年级信息");
                    }
                    UserDetail userDetail = new UserDetail();
                    userDetail.setUid(newUser.getUid());
                    userDetail.setStudentGrade(grade);
                    userDetailService.createUserDetail(userDetail);  // 使用 createUserDetail 保存用户详细信息
                }

            } else {
                // 更新用户
                User existingUser = userRepository.findById(uid)
                        .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

                if (!existingUser.getUsername().equals(username) && userRepository.findByUsername(username).isPresent()) {
                    return buildErrorResponse(400, "用户名已存在");
                }

                existingUser.setUsername(username);
                existingUser.setFullName(fullName);
                existingUser.setEmail(email);
                if (avatarUrl != null) {
                    existingUser.setAvatarUrl(normalizedAvatarUrl);
                }
                if (password != null && !password.isBlank()) {
                    existingUser.setPassword(password);
                }
                existingUser.setRole(userService.getRoleByName(role));
                userService.saveUser(existingUser);
                affectedUid = existingUser.getUid();

                // 如果是更新学生信息，不需要强制提供年级信息
                if ("STUDENT".equals(role)) {
                    String grade = userRequest.getGrade();
                    UserDetail userDetail = userDetailService.getUserDetailByUid(uid);
                    if (userDetail == null) {
                        userDetail = new UserDetail();
                        userDetail.setUid(existingUser.getUid());
                    }
                    // 如果请求中提供了年级信息，则更新年级
                    if (grade != null && !grade.isEmpty()) {
                        userDetail.setStudentGrade(grade);
                    }
                    userDetailService.updateUserDetail(uid, userDetail);  // 使用 updateUserDetail 保存用户详细信息
                }
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("message", created ? "用户信息创建成功" : "用户信息更新成功");
            Map<String, Object> data = new HashMap<>();
            data.put("uid", affectedUid);
            data.put("created", created);
            responseBody.put("data", data);

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return DEFAULT_CARD_BACKGROUND_URL;
        }

        String normalized = avatarUrl.trim();
        if (normalized.isEmpty()) {
            return DEFAULT_CARD_BACKGROUND_URL;
        }

        String lowerNormalized = normalized.toLowerCase();
        if (LEGACY_DEFAULT_AVATAR_URL.equalsIgnoreCase(normalized)
                || lowerNormalized.endsWith("/defaultavater.webp")
                || lowerNormalized.endsWith("/defaultavatar.webp")
                || lowerNormalized.endsWith("defaultavater.webp")
                || lowerNormalized.endsWith("defaultavatar.webp")) {
            return DEFAULT_CARD_BACKGROUND_URL;
        }

        return normalized;
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

    @PostMapping("/student/{uid}/archive")
    public ResponseEntity<?> archiveStudent(@PathVariable Long uid, @RequestBody(required = false) StudentArchiveRequest request) {
        try {
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);
            User currentUser = userService.getUserByUid(currentUid);

            if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
                return buildErrorResponse(403, "仅管理员可以转出学生");
            }

            User student = userService.getUserByUid(uid);
            if (!"STUDENT".equals(student.getRole().getRoleName())) {
                return buildErrorResponse(400, "只能转出学生账号");
            }

            String reason = request == null ? null : request.getReason();
            userService.archiveStudentByUid(uid, currentUser, reason);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("message", "学生已转出停用，历史记录已保留");
            return ResponseEntity.ok(responseBody);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(400, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @DeleteMapping("/student/{uid}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long uid) {
        return archiveStudent(uid, null);
    }

    @PostMapping("/teachers/archive")
    public ResponseEntity<?> archiveTeachers(@RequestBody TeacherArchiveRequest request) {
        try {
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);
            User currentUser = userService.getUserByUid(currentUid);

            if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
                return buildErrorResponse(403, "仅管理员可以删除导师");
            }

            Map<String, Object> result = userService.archiveTeachersByUids(
                    request == null ? null : request.getTeacherIds(),
                    currentUser,
                    request == null ? null : request.getReason()
            );

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("message", "导师已转为历史导师，当前学生关系已解除");
            responseBody.put("data", result);
            return ResponseEntity.ok(responseBody);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(400, e.getMessage());
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
