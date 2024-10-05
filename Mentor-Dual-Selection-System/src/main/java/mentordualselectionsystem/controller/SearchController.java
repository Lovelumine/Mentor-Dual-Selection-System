package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Tag(name = "搜索相关接口", description = "提供对学生和老师信息的查询和筛选功能，仅限导师和管理员访问")
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

    /**
     * 获取学生的详细信息，仅限导师和管理员。
     * @param uid 学生的UID
     * @param authentication 当前用户的认证信息
     * @return 学生的详细信息
     */
    @Operation(summary = "获取学生的详细信息", description = "根据 UID 获取学生的详细信息，仅限导师和管理员。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回学生详细信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息"),
            @ApiResponse(responseCode = "404", description = "学生不存在")
    })
    @GetMapping("/student")
    public ResponseEntity<?> getStudentInfo(@RequestParam Long uid, Authentication authentication) {

        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);

        // 获取当前用户信息
        User currentUser;
        try {
            currentUser = userService.getUserByUid(currentUid);
        } catch (Exception e) {
            return buildErrorResponse(404, "当前用户信息不存在");
        }

        String roleName = currentUser.getRole().getRoleName();

        // 仅管理员或导师可访问
        if (!"ADMIN".equals(roleName) && !"TEACHER".equals(roleName)) {
            return buildErrorResponse(403, "无权限访问该信息");
        }

        // 获取指定 UID 的学生信息
        User student;
        try {
            student = userService.getUserByUid(uid);
        } catch (Exception e) {
            return buildErrorResponse(404, "uid不存在");
        }

        if (student == null || !"STUDENT".equals(student.getRole().getRoleName())) {
            return buildErrorResponse(404, "此uid不是学生");
        }

        // 获取学生详细信息
        UserDetail userDetail;
        try {
            userDetail = userDetailService.getUserDetailByUid(uid); // 通过服务获取 UserDetail
        } catch (Exception e) {
            return buildErrorResponse(404, "学生详细信息不存在");
        }

        // 构建返回数据
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 200);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", student.getUid());
        data.put("username", student.getUsername());
        data.put("email", student.getEmail());
        data.put("fullName", student.getFullName());
        data.put("avatarUrl", student.getAvatarUrl());
        data.put("photourl", userDetail.getPhotoUrl());
        data.put("grade", userDetail.getStudentGrade());
        data.put("class", userDetail.getStudentClass());
        data.put("resume", userDetail.getResume());
        data.put("research_direction", userDetail.getResearchDirection());
        data.put("mentor_id",student.getMentorId());

        responseBody.put("data", data);
        return ResponseEntity.ok(responseBody);
    }

    /**
     * 获取所有学生的详细信息，仅限管理员和导师。
     * @param authentication 当前用户的认证信息
     * @return 所有学生的详细信息列表
     */
    @Operation(summary = "获取所有学生的详细信息", description = "仅限管理员和导师。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回所有学生的详细信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息")
    })
    @GetMapping("/students")
    public ResponseEntity<?> getAllStudentsInfo(Authentication authentication) {

        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);

        // 获取当前用户信息
        User currentUser;
        try {
            currentUser = userService.getUserByUid(currentUid);
        } catch (Exception e) {
            return buildErrorResponse(404, "当前用户信息不存在");
        }

        String roleName = currentUser.getRole().getRoleName();

        // 仅管理员或导师可访问
        if (!"ADMIN".equals(roleName) && !"TEACHER".equals(roleName)) {
            return buildErrorResponse(403, "无权限访问该信息");
        }

        // 获取所有学生信息
        List<User> students;
        try {
            students = userService.getAllStudents();
        } catch (Exception e) {
            return buildErrorResponse(500, "获取学生列表失败");
        }

        List<Map<String, Object>> studentDataList = students.stream().map(student -> {
            UserDetail userDetail;
            try {
                userDetail = userDetailService.getUserDetailByUid(student.getUid());
            } catch (Exception e) {
                userDetail = null;
            }

            Map<String, Object> studentData = new HashMap<>();
            studentData.put("uid", student.getUid());
            studentData.put("username", student.getUsername());
            studentData.put("email", student.getEmail());
            studentData.put("fullName", student.getFullName());
            studentData.put("avatarUrl", student.getAvatarUrl());
            studentData.put("mentor_id",student.getMentorId());
            if (userDetail != null) {
                studentData.put("photourl", userDetail.getPhotoUrl());
                studentData.put("grade", userDetail.getStudentGrade());
                studentData.put("class", userDetail.getStudentClass());
                studentData.put("research_direction", userDetail.getResearchDirection());
                studentData.put("resume", userDetail.getResume());
            } else {
                studentData.put("photourl", null);
                studentData.put("grade", null);
                studentData.put("class", null);
                studentData.put("resume", null);
                studentData.put("research_direction", null);
            }
            return studentData;
        }).collect(Collectors.toList());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 200);
        responseBody.put("data", studentDataList);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * 根据名字模糊搜索学生，仅限管理员和导师。
     * @param name 学生名字（模糊查询）
     * @param authentication 当前用户的认证信息
     * @return 匹配的学生信息列表
     */
    @Operation(summary = "根据名字模糊搜索学生", description = "仅限管理员和导师。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回匹配的学生信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息")
    })
    @GetMapping("/students/search")
    public ResponseEntity<?> searchStudentsByName(@RequestParam String name, Authentication authentication) {

        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);

        // 获取当前用户信息
        User currentUser;
        try {
            currentUser = userService.getUserByUid(currentUid);
        } catch (Exception e) {
            return buildErrorResponse(404, "当前用户信息不存在");
        }

        String roleName = currentUser.getRole().getRoleName();

        // 仅管理员或导师可访问
        if (!"ADMIN".equals(roleName) && !"TEACHER".equals(roleName)) {
            return buildErrorResponse(403, "无权限访问该信息");
        }

        // 根据名字模糊搜索学生
        List<User> students;
        try {
            students = userService.searchStudentsByName(name);
        } catch (Exception e) {
            return buildErrorResponse(500, "学生搜索失败");
        }

        List<Map<String, Object>> studentDataList = students.stream().map(student -> {
            UserDetail userDetail;
            try {
                userDetail = userDetailService.getUserDetailByUid(student.getUid());
            } catch (Exception e) {
                userDetail = null;
            }

            Map<String, Object> studentData = new HashMap<>();
            studentData.put("uid", student.getUid());
            studentData.put("username", student.getUsername());
            studentData.put("email", student.getEmail());
            studentData.put("fullName", student.getFullName());
            studentData.put("avatarUrl", student.getAvatarUrl());
            studentData.put("mentor_id",student.getMentorId());
            if (userDetail != null) {
                studentData.put("photourl", userDetail.getPhotoUrl());
                studentData.put("grade", userDetail.getStudentGrade());
                studentData.put("class", userDetail.getStudentClass());
                studentData.put("research_direction", userDetail.getResearchDirection());
                studentData.put("resume", userDetail.getResume());
            } else {
                studentData.put("photourl", null);
                studentData.put("grade", null);
                studentData.put("class", null);
                studentData.put("resume", null);
                studentData.put("research_direction", null);
            }
            return studentData;
        }).collect(Collectors.toList());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 200);
        responseBody.put("data", studentDataList);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * 根据年级或班级筛选学生，仅限管理员和导师。
     * @param grade 年级（可选）
     * @param studentClass 班级（可选）
     * @param authentication 当前用户的认证信息
     * @return 符合条件的学生信息列表
     */
    @Operation(summary = "根据年级或班级筛选学生", description = "仅限管理员和导师。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回符合条件的学生信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息")
    })
    @GetMapping("/students/filter")
    public ResponseEntity<?> filterStudentsByGradeOrClass(
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String studentClass,
            Authentication authentication) {

        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);

        // 获取当前用户信息
        User currentUser;
        try {
            currentUser = userService.getUserByUid(currentUid);
        } catch (Exception e) {
            return buildErrorResponse(404, "当前用户信息不存在");
        }

        String roleName = currentUser.getRole().getRoleName();

        // 仅管理员或导师可访问
        if (!"ADMIN".equals(roleName) && !"TEACHER".equals(roleName)) {
            return buildErrorResponse(403, "无权限访问该信息");
        }

        // 获取所有学生信息
        List<User> students;
        try {
            students = userService.getAllStudents();
        } catch (Exception e) {
            return buildErrorResponse(500, "获取学生列表失败");
        }
        int totalStudents = students.size(); // 获取学生总数

        // 筛选学生
        List<Map<String, Object>> filteredStudents = students.stream()
                .filter(student -> {
                    UserDetail userDetail;
                    try {
                        userDetail = userDetailService.getUserDetailByUid(student.getUid());
                    } catch (Exception e) {
                        userDetail = null;
                    }
                    boolean matchGrade = (grade != null && userDetail != null && grade.equals(userDetail.getStudentGrade()));
                    boolean matchClass = (studentClass != null && userDetail != null && studentClass.equals(userDetail.getStudentClass()));
                    return matchGrade || matchClass;
                })
                .map(student -> {
                    UserDetail userDetail;
                    try {
                        userDetail = userDetailService.getUserDetailByUid(student.getUid());
                    } catch (Exception e) {
                        userDetail = null;
                    }

                    Map<String, Object> studentData = new HashMap<>();
                    studentData.put("uid", student.getUid());
                    studentData.put("username", student.getUsername());
                    studentData.put("email", student.getEmail());
                    studentData.put("fullName", student.getFullName());
                    studentData.put("avatarUrl", student.getAvatarUrl());
                    studentData.put("mentor_id",student.getMentorId());
                    if (userDetail != null) {
                        studentData.put("photourl", userDetail.getPhotoUrl());
                        studentData.put("grade", userDetail.getStudentGrade());
                        studentData.put("class", userDetail.getStudentClass());
                        studentData.put("research_direction", userDetail.getResearchDirection());
                        studentData.put("resume", userDetail.getResume());
                    } else {
                        studentData.put("photourl", null);
                        studentData.put("grade", null);
                        studentData.put("class", null);
                        studentData.put("resume", null);
                        studentData.put("research_direction", null);
                    }
                    return studentData;
                }).collect(Collectors.toList());

        int filteredStudentsCount = filteredStudents.size(); // 获取筛选后的学生数量

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 200);
        responseBody.put("totalStudents", totalStudents); // 总学生数量
        responseBody.put("filteredStudentsCount", filteredStudentsCount); // 筛选后的学生数量
        responseBody.put("data", filteredStudents);

        return ResponseEntity.ok(responseBody);
    }

    // 获取所有老师的详细信息，仅限管理员和导师。
    @Operation(summary = "获取所有老师的详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回所有老师的详细信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息")
    })
    @GetMapping("/teachers")
    public ResponseEntity<?> getAllTeachersInfo(Authentication authentication) {

        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);

        // 获取当前用户信息
        User currentUser;
        try {
            currentUser = userService.getUserByUid(currentUid);
        } catch (Exception e) {
            return buildErrorResponse(404, "当前用户信息不存在");
        }

        String roleName = currentUser.getRole().getRoleName();


        // 获取所有老师信息
        List<User> teachers;
        try {
            teachers = userService.getAllTeachers();
        } catch (Exception e) {
            return buildErrorResponse(500, "获取老师列表失败");
        }

        List<Map<String, Object>> teacherDataList = teachers.stream().map(teacher -> {
            UserDetail userDetail;
            try {
                userDetail = userDetailService.getUserDetailByUid(teacher.getUid());
            } catch (Exception e) {
                userDetail = null;
            }

            Map<String, Object> teacherData = new HashMap<>();
            teacherData.put("uid", teacher.getUid());
            teacherData.put("username", teacher.getUsername());
            teacherData.put("email", teacher.getEmail());
            teacherData.put("fullName", teacher.getFullName());
            teacherData.put("avatarUrl", teacher.getAvatarUrl());
            teacherData.put("AcceptedStudents", teacher.getAcceptedStudents());
            if (userDetail != null) {
                teacherData.put("photourl", userDetail.getPhotoUrl());
                teacherData.put("teacherposition", userDetail.getTeacherPosition());
                teacherData.put("research_direction", userDetail.getResearchDirection());
                teacherData.put("resume", userDetail.getResume());
            } else {
                teacherData.put("photourl", null);
                teacherData.put("teacherposition", null);
                teacherData.put("research_direction", null);
                teacherData.put("resume", null);
            }
            return teacherData;
        }).collect(Collectors.toList());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 200);
        responseBody.put("data", teacherDataList);

        return ResponseEntity.ok(responseBody);
    }

    // 获取指定老师的详细信息
    @Operation(summary = "获取指定老师的详细信息", description = "根据 UID 获取老师的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回老师详细信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息"),
            @ApiResponse(responseCode = "404", description = "老师不存在")
    })
    @GetMapping("/teacher")
    public ResponseEntity<?> getTeacherInfo(@RequestParam Long uid, Authentication authentication) {

        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);

        // 获取当前用户信息
        User currentUser;
        try {
            currentUser = userService.getUserByUid(currentUid);
        } catch (Exception e) {
            return buildErrorResponse(404, "当前用户信息不存在");
        }

        String roleName = currentUser.getRole().getRoleName();


        // 获取指定 UID 的老师信息
        User teacher;
        try {
            teacher = userService.getUserByUid(uid);
        } catch (Exception e) {
            return buildErrorResponse(404, "uid不存在");
        }

        if (teacher == null || !"TEACHER".equals(teacher.getRole().getRoleName())) {
            return buildErrorResponse(404, "此uid不是老师");
        }

        // 获取老师详细信息
        UserDetail userDetail;
        try {
            userDetail = userDetailService.getUserDetailByUid(uid); // 通过服务获取 UserDetail
        } catch (Exception e) {
            return buildErrorResponse(404, "老师详细信息不存在");
        }

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 200);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", teacher.getUid());
        data.put("username", teacher.getUsername());
        data.put("email", teacher.getEmail());
        data.put("fullName", teacher.getFullName());
        data.put("avatarUrl", teacher.getAvatarUrl());
        data.put("AcceptedStudents", teacher.getAcceptedStudents());
        data.put("photourl", userDetail.getPhotoUrl());
        data.put("teacher_position", userDetail.getTeacherPosition());
        data.put("research_direction", userDetail.getResearchDirection());
        data.put("resume", userDetail.getResume());


        responseBody.put("data", data);
        return ResponseEntity.ok(responseBody);
    }

    /**
     * 根据名字模糊搜索老师。
     * @param name 老师名字（模糊查询）
     * @param authentication 当前用户的认证信息
     * @return 匹配的老师信息列表
     */
    @Operation(summary = "根据名字模糊搜索老师", description = "根据名字模糊搜索老师。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回匹配的老师信息"),
            @ApiResponse(responseCode = "401", description = "未授权，token 无效或缺失"),
            @ApiResponse(responseCode = "403", description = "无权限访问该信息")
    })
    @GetMapping("/teachers/search")
    public ResponseEntity<?> searchTeachersByName(@RequestParam String name, Authentication authentication) {

        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);

        // 获取当前用户信息
        User currentUser;
        try {
            currentUser = userService.getUserByUid(currentUid);
        } catch (Exception e) {
            return buildErrorResponse(404, "当前用户信息不存在");
        }

        String roleName = currentUser.getRole().getRoleName();


        // 根据名字模糊搜索老师
        List<User> teachers;
        try {
            teachers = userService.searchTeachersByName(name);
        } catch (Exception e) {
            return buildErrorResponse(500, "老师搜索失败");
        }

        List<Map<String, Object>> teacherDataList = teachers.stream().map(teacher -> {
            UserDetail userDetail;
            try {
                userDetail = userDetailService.getUserDetailByUid(teacher.getUid());
            } catch (Exception e) {
                userDetail = null;
            }

            Map<String, Object> teacherData = new HashMap<>();
            teacherData.put("uid", teacher.getUid());
            teacherData.put("username", teacher.getUsername());
            teacherData.put("email", teacher.getEmail());
            teacherData.put("fullName", teacher.getFullName());
            teacherData.put("avatarUrl", teacher.getAvatarUrl());
            teacherData.put("AcceptedStudents", teacher.getAcceptedStudents());
            if (userDetail != null) {
                teacherData.put("photourl", userDetail.getPhotoUrl());
                teacherData.put("teacherposition", userDetail.getTeacherPosition());
                teacherData.put("research_direction", userDetail.getResearchDirection());
                teacherData.put("resume", userDetail.getResume());
            } else {
                teacherData.put("photourl", null);
                teacherData.put("teacherposition", null);
                teacherData.put("research_direction", null);
                teacherData.put("resume", null);
            }
            return teacherData;
        }).collect(Collectors.toList());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 200);
        responseBody.put("data", teacherDataList);

        return ResponseEntity.ok(responseBody);
    }

    // 构建统一错误响应
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

    // 构建统一格式的响应
    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
