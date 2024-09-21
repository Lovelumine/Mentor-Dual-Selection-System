package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.services.UserDetailService;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name="学生信息管理接口")
@RequestMapping("/student")
public class StudentController {

    private final UserDetailService userDetailService;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @Autowired
    public StudentController(UserDetailService userDetailService, JwtUtils jwtUtils, UserService userService) {
        this.userDetailService = userDetailService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    /**
     * 获取所有教师的详细信息
     *
     * @return ResponseEntity<Map<String, Object>> 教师详细信息列表
     */
    @Operation(summary = "获取所有教师详细信息", description = "返回所有教师的详细信息列表。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
    })
    @GetMapping("/teachers")
    public ResponseEntity<Map<String, Object>> getAllTeacherDetails() {
        List<UserDetail> teacherDetails = userDetailService.getAllTeacherDetails();
        return ResponseEntity.ok(formatResponse(200, teacherDetails));
    }

    /**
     * 获取学生自己的详细信息
     *
     * @param authentication 当前用户的认证信息
     * @return ResponseEntity<Map<String, Object>> 学生的详细信息
     */
    @Operation(summary = "获取学生个人详细信息", description = "验证身份并返回学生的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "403", description = "当前用户不是学生"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/my-detail")
    public ResponseEntity<Map<String, Object>> getMyDetail(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取用户ID
        User currentUser = userService.getUserByUid(userId);  // 获取当前用户

        // 判断角色类型
        String roleName = currentUser.getRole().getRoleName();
        if (!"STUDENT".equals(roleName)) {
            return ResponseEntity.status(403).body(formatResponse(403, "当前用户不是学生，无法获取详细信息"));
        }

        // 获取学生详细信息
        UserDetail userDetail = userDetailService.getUserDetailByUid(currentUser.getUid());
        if (userDetail == null) {
            return ResponseEntity.ok(formatResponse(200, "当前还没有编辑个人信息"));
        }
        return ResponseEntity.ok(formatResponse(200, userDetail));
    }

    /**
     * 更新学生个人详细信息
     *
     * @param authentication 当前用户的认证信息
     * @param userDetail     要更新的学生详细信息
     * @return ResponseEntity<Map<String, Object>> 更新结果
     */
    @Operation(summary = "更新学生个人详细信息", description = "验证身份并更新学生的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "403", description = "当前用户不是学生"),
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
        if (!"STUDENT".equals(roleName)) {
            return ResponseEntity.status(403).body(formatResponse(403, "当前用户不是学生，无法更新详细信息"));
        }

        // 更新学生详细信息
        UserDetail updatedDetail = userDetailService.updateUserDetail(userId, userDetail);
        return ResponseEntity.ok(formatResponse(200, updatedDetail));
    }

    /**
     * 格式化返回的消息
     *
     * @param code 状态码
     * @param data 返回的数据
     * @return 格式化后的响应
     */
    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
