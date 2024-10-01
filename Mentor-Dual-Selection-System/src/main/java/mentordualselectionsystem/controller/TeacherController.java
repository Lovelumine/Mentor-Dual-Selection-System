package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.services.UserDetailService;
import mentordualselectionsystem.services.UserService;
import mentordualselectionsystem.services.ApplicationService;
import mentordualselectionsystem.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "教师信息管理接口")
@RequestMapping("/teacher")
public class TeacherController {

    private final ApplicationService applicationService;
    private final UserDetailService userDetailService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @Autowired
    public TeacherController(ApplicationService applicationService, UserDetailService userDetailService, UserService userService, JwtUtils jwtUtils) {
        this.applicationService = applicationService;
        this.userDetailService = userDetailService;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @Operation(summary = "获取某学生的详细信息", description = "根据Uid查看学生的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "403", description = "无权查看该学生的详细信息"),
            @ApiResponse(responseCode = "404", description = "学生未找到")
    })
    @GetMapping("/student/{uid}")
    public ResponseEntity<Map<String, Object>> getStudentDetail(Authentication authentication, @PathVariable Long uid) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long teacherId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取教师ID
        User teacher = userService.getUserByUid(teacherId);  // 获取当前教师

        User student;
        try {
            student = userService.getUserByUid(uid);
            if (student == null) {
                return ResponseEntity.status(404).body(formatResponse(404, "账号未找到"));
            }
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(formatResponse(404, "账号未找到"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(formatResponse(500, "内部服务器错误"));
        }


        // 判断角色是否为学生
        if (!"STUDENT".equals(student.getRole().getRoleName())) {
            return ResponseEntity.status(403).body(formatResponse(403, "该用户不是学生，无权查看该用户的详细信息"));
        }

        UserDetail userDetail = userDetailService.getUserDetailByUid(uid);
        return ResponseEntity.ok(formatResponse(200, userDetail));
    }



    @Operation(summary = "获取所有学生详细信息", description = "返回所有学生的详细信息列表。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
    })
    @GetMapping("/students")
    public ResponseEntity<Map<String, Object>> getAllStudentDetails(Authentication authentication) {
        List<UserDetail> studentDetails = userDetailService.getAllStudentDetails();
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long teacherId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取教师ID
        User teacher = userService.getUserByUid(teacherId);  // 获取当前教师

        // 判断角色是否为学生
        if (!"TEACHER".equals(teacher.getRole().getRoleName())) {
            return ResponseEntity.status(403).body(formatResponse(403, "该用户不是老师，无权查看所有学生的详细信息"));
        }
        return ResponseEntity.ok(formatResponse(200, studentDetails));
    }


    @Operation(summary = "获取教师自己的详细信息", description = "返回教师的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/my-detail")
    public ResponseEntity<Map<String, Object>> getMyDetail(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long teacherId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取教师ID
        UserDetail userDetail = userDetailService.getUserDetailByUid(teacherId);


        if (userDetail == null) {
            return ResponseEntity.ok(formatResponse(201, "当前还没有编辑个人信息"));
        }
        return ResponseEntity.ok(formatResponse(200, userDetail));
    }

    @Operation(summary = "更新老师个人详细信息", description = "验证身份并更新老师的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "403", description = "当前用户不是老师"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateMyDetail(Authentication authentication, @RequestBody UserDetail userDetail) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取用户ID
        User currentUser = userService.getUserByUid(userId);  // 获取当前用户

        // 判断角色类型
        String roleName = currentUser.getRole().getRoleName();
        if (!"TEACHER".equals(roleName)) {
            return ResponseEntity.status(403).body(formatResponse(403, "当前用户不是老师，无法更新详细信息"));
        }

        // 更新老师详细信息
        UserDetail updatedDetail = userDetailService.updateUserDetail(userId, userDetail);
        return ResponseEntity.ok(formatResponse(200, updatedDetail));
    }

    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
