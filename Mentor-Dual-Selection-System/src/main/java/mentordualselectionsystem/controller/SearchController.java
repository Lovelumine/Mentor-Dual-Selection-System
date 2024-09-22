package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.dto.StudentSearchRequest;
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
import java.util.stream.Collectors;

@RestController
@Tag(name = "搜索相关接口")
@RequestMapping("/api/search")
public class SearchController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Autowired
    public SearchController(UserService userService, JwtUtils jwtUtils, UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Operation(summary = "获取模糊搜索导师信息", description = "根据模糊搜索关键词返回符合条件的导师信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回符合条件的导师信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/teachers")
    public ResponseEntity<?> searchTeachers(@RequestParam String keyword) {
        try {
            List<User> teachers = userRepository.findAll().stream()
                    .filter(user -> "TEACHER".equals(user.getRole().getRoleName()) &&
                            (user.getFullName().contains(keyword) || user.getUsername().contains(keyword)
                                    || user.getEmail().contains(keyword) || String.valueOf(user.getUid()).contains(keyword)))
                    .collect(Collectors.toList());

            List<Map<String, Object>> teacherList = teachers.stream()
                    .map(teacher -> {
                        Map<String, Object> teacherMap = new HashMap<>();
                        teacherMap.put("uid", teacher.getUid());
                        teacherMap.put("fullName", teacher.getFullName());
                        teacherMap.put("avatarUrl", teacher.getAvatarUrl());
                        teacherMap.put("professionalDirection", teacher.getUserDetail().getProfessionalDirection());
                        teacherMap.put("teacherPosition", teacher.getUserDetail().getTeacherPosition());
                        teacherMap.put("researchDirection", teacher.getUserDetail().getResearchDirection());
                        return teacherMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("data", teacherList);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return buildErrorResponse(401, "token无效或已过期");
        }
    }

    @Operation(summary = "获取符合条件的学生列表", description = "根据给定的参数筛选符合条件的学生信息，仅限导师和管理员。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回符合条件的学生信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/students")
    public ResponseEntity<?> searchStudents(@RequestBody StudentSearchRequest searchRequest) {
        try {
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            Long uid = jwtUtils.validateTokenAndGetUid(jwtToken);

            // 获取当前用户信息
            User currentUser = userService.getUserByUid(uid);
            String roleName = currentUser.getRole().getRoleName();

            // 仅管理员或导师可访问
            if (!"ADMIN".equals(roleName) && !"TEACHER".equals(roleName)) {
                return buildErrorResponse(403, "无权限访问该信息");
            }

            List<User> students = userRepository.findAll().stream()
                    .filter(user -> "STUDENT".equals(user.getRole().getRoleName()) &&
                            (searchRequest.getName() == null || user.getFullName().contains(searchRequest.getName())) &&
                            (searchRequest.getStudentId() == null || user.getUsername().contains(searchRequest.getStudentId())) &&
                            (searchRequest.getGrade() == null || searchRequest.getGrade().equals(user.getUserDetail().getStudentGrade())))
                    .collect(Collectors.toList());

            List<Map<String, Object>> studentList = students.stream()
                    .map(student -> {
                        Map<String, Object> studentMap = new HashMap<>();
                        studentMap.put("uid", student.getUid());
                        studentMap.put("fullName", student.getFullName());
                        studentMap.put("username", student.getUsername());
                        studentMap.put("studentGrade", student.getUserDetail().getStudentGrade());
                        studentMap.put("studentClass", student.getUserDetail().getStudentClass());
                        return studentMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("data", studentList);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return buildErrorResponse(401, "token无效或已过期");
        }
    }

    @Operation(summary = "获取学生的详细信息", description = "根据 UID 获取学生的详细信息，仅限导师和管理员。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回学生详细信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "学生不存在")
    })
    @GetMapping("/student")
    public ResponseEntity<?> getStudentInfo(@RequestParam Long uid) {
        try {
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

            // 获取指定 UID 的学生信息
            User student = userService.getUserByUid(uid);
            if (student == null || !"STUDENT".equals(student.getRole().getRoleName())) {
                return buildErrorResponse(404, "学生不存在");
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);

            Map<String, Object> data = new HashMap<>();
            data.put("uid", student.getUid());
            data.put("username", student.getUsername());
            data.put("email", student.getEmail());
            data.put("fullName", student.getFullName());
            data.put("avatarUrl", student.getAvatarUrl());
            data.put("grade", student.getUserDetail().getStudentGrade());
            data.put("class", student.getUserDetail().getStudentClass());

            responseBody.put("data", data);
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return buildErrorResponse(401, "token无效或已过期");
        }
    }

    // 新增接口：根据研究方向搜索教师信息
    @Operation(summary = "根据研究方向获取导师信息", description = "根据研究方向关键词返回符合条件的导师信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回符合条件的导师信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/teachers/research")
    public ResponseEntity<?> searchTeachersByResearch(@RequestParam String researchKeyword) {
        try {
            List<User> teachers = userRepository.findAll().stream()
                    .filter(user -> "TEACHER".equals(user.getRole().getRoleName()) &&
                            user.getUserDetail().getResearchDirection() != null &&
                            user.getUserDetail().getResearchDirection().contains(researchKeyword))
                    .collect(Collectors.toList());

            List<Map<String, Object>> teacherList = teachers.stream()
                    .map(teacher -> {
                        Map<String, Object> teacherMap = new HashMap<>();
                        teacherMap.put("uid", teacher.getUid());
                        teacherMap.put("fullName", teacher.getFullName());
                        teacherMap.put("avatarUrl", teacher.getAvatarUrl());
                        teacherMap.put("professionalDirection", teacher.getUserDetail().getProfessionalDirection());
                        teacherMap.put("teacherPosition", teacher.getUserDetail().getTeacherPosition());
                        teacherMap.put("researchDirection", teacher.getUserDetail().getResearchDirection());
                        return teacherMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);
            responseBody.put("data", teacherList);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return buildErrorResponse(401, "token无效或已过期");
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
