package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserDetailService;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "管理员信息管理接口")
@RequestMapping("/admin")
public class AdminController {
    private final UserDetailService userDetailService;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @Autowired
    public AdminController(UserDetailService userDetailService, JwtUtils jwtUtils, UserService userService) {
        this.userDetailService = userDetailService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    //获取所有教师详细信息
    @Operation(summary = "获取所有教师详细信息", description = "返回所有教师的详细信息列表。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
    })
    @GetMapping("/teachers")
    public ResponseEntity<Map<String, Object>> getAllTeacherDetails() {
        List<UserDetail> teacherDetails = userDetailService.getAllTeacherDetails();
        return ResponseEntity.ok(formatResponse(200, teacherDetails));
    }


    //获取所有学生详细信息
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
        Long adminId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取教师ID
        User admin = userService.getUserByUid(adminId);  // 获取当前教师

        // 判断角色是否为学生
        if (!"ADMIN".equals(admin.getRole().getRoleName())) {
            return ResponseEntity.status(403).body(formatResponse(403, "该用户不是管理员，无权查看所有学生的详细信息"));
        }
        return ResponseEntity.ok(formatResponse(200, studentDetails));
    }

    // 根据UID获取用户详细信息
    @Operation(summary = "获取某用户的详细信息", description = "根据Uid查看用户的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "用户未找到")
    })
    @GetMapping("/userdetails/{uid}")
    public ResponseEntity<Map<String, Object>> getStudentDetail(Authentication authentication, @PathVariable Long uid) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long adminId = jwtUtils.validateTokenAndGetUid(jwtToken);
        User admin = userService.getUserByUid(adminId);

        User userdetail;
        try {
            userdetail = userService.getUserByUid(uid);
            if (userdetail == null) {
                return ResponseEntity.status(404).body(formatResponse(404, "账号未找到"));
            }
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(formatResponse(404, "账号未找到"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(formatResponse(500, "内部服务器错误"));
        }


        UserDetail userDetail = userDetailService.getUserDetailByUid(uid);
        return ResponseEntity.ok(formatResponse(200, userDetail));
    }


    @Operation(summary = "更新某用户的详细信息", description = "通过uid更新某用户的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PutMapping("/update/{uid}")
    public ResponseEntity<Map<String, Object>> updateMyDetail(Authentication authentication, @RequestBody UserDetail userDetail, @PathVariable Long uid) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取用户ID
        User currentUser = userService.getUserByUid(userId);  // 获取当前用户

        // 判断角色类型
        String roleName = currentUser.getRole().getRoleName();
        if (!"ADMIN".equals(roleName)) {
            return ResponseEntity.status(403).body(formatResponse(403, "当前用户不是管理员，无法更新详细信息"));
        }

        // 更新用户详细信息
        UserDetail updatedDetail = userDetailService.updateUserDetail(uid, userDetail);
        return ResponseEntity.ok(formatResponse(200, updatedDetail));
    }

    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
