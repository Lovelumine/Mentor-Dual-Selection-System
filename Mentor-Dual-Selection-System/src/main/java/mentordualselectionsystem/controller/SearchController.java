package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.dto.StudentSearchRequest;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.repositories.UserRepository;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserDetailService;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Tag(name = "搜索相关接口")
@RequestMapping("/api/search")
public class SearchController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final UserDetailService userDetailService;

    @Autowired
    public SearchController(UserService userService, JwtUtils jwtUtils, UserRepository userRepository, UserDetailService userDetailService) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.userDetailService = userDetailService;
    }

    @Operation(summary = "获取符合条件的学生列表", description = "根据给定的参数筛选符合条件的学生信息，仅限导师和管理员。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回符合条件的学生信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息")
    })
    @PostMapping("/students")
    public ResponseEntity<?> searchStudents(Authentication authentication, @RequestBody StudentSearchRequest searchRequest) {
        if (authentication == null || authentication.getCredentials() == null) {
            return buildErrorResponse(401, "当前的 token 无效");
        }

        String jwtToken = (String) authentication.getCredentials();
        Long uid;
        try {
            uid = jwtUtils.validateTokenAndGetUid(jwtToken);
        } catch (Exception e) {
            return buildErrorResponse(401, "token 无效或已过期");
        }

        // 获取当前用户信息
        User currentUser = userService.getUserByUid(uid);
        if (currentUser == null) {
            return buildErrorResponse(401, "用户不存在");
        }
        String roleName = currentUser.getRole().getRoleName();

        // 仅管理员或导师可访问
        if (!"ADMIN".equals(roleName) && !"TEACHER".equals(roleName)) {
            return buildErrorResponse(403, "无权限访问该信息");
        }

        try {
            List<User> students = userRepository.findAll().stream()
                    .filter(user -> {
                        if (!"STUDENT".equals(user.getRole().getRoleName())) {
                            return false;
                        }
                        if (searchRequest.getName() != null && !user.getFullName().contains(searchRequest.getName())) {
                            return false;
                        }
                        if (searchRequest.getStudentId() != null && !user.getUsername().contains(searchRequest.getStudentId())) {
                            return false;
                        }
                        if (searchRequest.getGrade() != null) {
                            // 根据 User 的 uid 获取 UserDetail 信息
                            UserDetail userDetail = userDetailService.getUserDetailByUid(user.getUid());
                            return userDetail != null && searchRequest.getGrade().equals(userDetail.getStudentGrade());
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            List<Map<String, Object>> studentList = students.stream()
                    .map(student -> {
                        Map<String, Object> studentMap = new HashMap<>();
                        studentMap.put("uid", student.getUid());
                        studentMap.put("username", student.getUsername());
                        studentMap.put("fullName", student.getFullName());
                        studentMap.put("email", student.getEmail());
                        studentMap.put("avatarUrl", student.getAvatarUrl());

                        // 根据 User 的 uid 获取 UserDetail 信息
                        UserDetail userDetail = userDetailService.getUserDetailByUid(student.getUid());
                        if (userDetail != null) {
                            studentMap.put("studentGrade", userDetail.getStudentGrade());
                            studentMap.put("studentClass", userDetail.getStudentClass());
                            // 合并其他需要的字段
                        }
                        return studentMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("data", studentList);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return buildErrorResponse(500, "内部服务器错误: " + e.getMessage());
        }
    }

    @Operation(summary = "获取学生的详细信息", description = "根据 UID 获取学生的详细信息，仅限导师和管理员。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回学生详细信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息"),
            @ApiResponse(responseCode = "404", description = "学生不存在")
    })
    @GetMapping("/student")
    public ResponseEntity<?> getStudentInfo(Authentication authentication, @RequestParam Long uid) {
        if (authentication == null || authentication.getCredentials() == null) {
            return buildErrorResponse(401, "当前的 token 无效");
        }

        String jwtToken = (String) authentication.getCredentials();
        Long currentUid;
        try {
            currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);
        } catch (Exception e) {
            return buildErrorResponse(401, "token 无效或已过期");
        }

        User currentUser = userService.getUserByUid(currentUid);
        if (currentUser == null) {
            return buildErrorResponse(401, "用户不存在");
        }
        String roleName = currentUser.getRole().getRoleName();

        // 仅管理员或导师可访问
        if (!"ADMIN".equals(roleName) && !"TEACHER".equals(roleName)) {
            return buildErrorResponse(403, "无权限访问该信息");
        }

        try {
            // 获取指定 UID 的学生信息
            User student = userService.getUserByUid(uid);
            if (student == null || !"STUDENT".equals(student.getRole().getRoleName())) {
                return buildErrorResponse(404, "学生不存在");
            }

            // 获取学生的详细信息
            UserDetail userDetail = userDetailService.getUserDetailByUid(student.getUid());

            Map<String, Object> data = new HashMap<>();
            data.put("uid", student.getUid());
            data.put("username", student.getUsername());
            data.put("email", student.getEmail());
            data.put("fullName", student.getFullName());
            data.put("avatarUrl", student.getAvatarUrl());

            if (userDetail != null) {
                data.put("studentGrade", userDetail.getStudentGrade());
                data.put("studentClass", userDetail.getStudentClass());
                // 合并其他需要的字段
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("data", data);
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return buildErrorResponse(500, "内部服务器错误: " + e.getMessage());
        }
    }

    @Operation(summary = "根据研究方向获取导师信息", description = "根据研究方向关键词返回符合条件的导师信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回符合条件的导师信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息")
    })
    @GetMapping("/teachers/research")
    public ResponseEntity<?> searchTeachersByResearch(Authentication authentication, @RequestParam String researchKeyword) {
        if (authentication == null || authentication.getCredentials() == null) {
            return buildErrorResponse(401, "当前的 token 无效");
        }

        String jwtToken = (String) authentication.getCredentials();
        try {
            jwtUtils.validateToken(jwtToken); // 验证 token 的有效性
        } catch (Exception e) {
            return buildErrorResponse(401, "token 无效或已过期");
        }

        try {
            List<User> teachers = userRepository.findAll().stream()
                    .filter(user -> {
                        if (!"TEACHER".equals(user.getRole().getRoleName())) {
                            return false;
                        }
                        UserDetail userDetail = userDetailService.getUserDetailByUid(user.getUid());
                        if (userDetail == null || userDetail.getResearchDirection() == null) {
                            return false;
                        }
                        return userDetail.getResearchDirection().contains(researchKeyword);
                    })
                    .collect(Collectors.toList());

            List<Map<String, Object>> teacherList = teachers.stream()
                    .map(teacher -> {
                        Map<String, Object> teacherMap = new HashMap<>();
                        teacherMap.put("uid", teacher.getUid());
                        teacherMap.put("username", teacher.getUsername());
                        teacherMap.put("fullName", teacher.getFullName());
                        teacherMap.put("email", teacher.getEmail());
                        teacherMap.put("avatarUrl", teacher.getAvatarUrl());

                        // 根据 User 的 uid 获取 UserDetail 信息
                        UserDetail userDetail = userDetailService.getUserDetailByUid(teacher.getUid());
                        if (userDetail != null) {
                            teacherMap.put("professionalDirection", userDetail.getProfessionalDirection());
                            teacherMap.put("teacherPosition", userDetail.getTeacherPosition());
                            teacherMap.put("researchDirection", userDetail.getResearchDirection());
                            // 合并其他需要的字段
                        }
                        return teacherMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("data", teacherList);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return buildErrorResponse(500, "内部服务器错误: " + e.getMessage());
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
