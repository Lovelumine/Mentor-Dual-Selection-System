package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import mentordualselectionsystem.mysql.Application;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.services.ApplicationService;
import mentordualselectionsystem.services.UserService;
import mentordualselectionsystem.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/application")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @Autowired
    public ApplicationController(ApplicationService applicationService, UserService userService, JwtUtils jwtUtils) {
        this.applicationService = applicationService;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @Operation(summary = "提交学生申请", description = "学生通过 token 提交申请，申请理由必须提供。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功提交申请"),
            @ApiResponse(responseCode = "400", description = "参数无效或缺失"),
            @ApiResponse(responseCode = "401", description = "token无效或已过期"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitApplication(@RequestParam Long mentorId,
                                                                 @RequestParam String reason) {
        try {
            // 从上下文中获取当前用户的认证信息
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();

            // 提取 token 并验证
            String jwtToken = token.replace("Bearer ", "");

            // 从token中解析出学生ID
            Long studentId = jwtUtils.validateTokenAndGetUid(jwtToken);

            // 提交申请
            Application application = applicationService.submitApplication(studentId, mentorId, reason);

            // 构建标准化返回
            return buildSuccessResponse(200, application);

        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @Operation(summary = "导师审批学生申请", description = "导师可以通过或拒绝学生的申请，拒绝时可以选择填写拒绝理由。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功审批申请"),
            @ApiResponse(responseCode = "400", description = "申请不存在或审批无效"),
            @ApiResponse(responseCode = "401", description = "token无效或已过期"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/approve")
    public ResponseEntity<Map<String, Object>> approveApplication(@RequestParam Long applicationId,
                                                                  @RequestParam boolean approved,
                                                                  @RequestParam(required = false) String rejectionReason) {
        try {
            // 从上下文中获取当前用户的认证信息
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();

            // 提取 token 并验证
            String jwtToken = token.replace("Bearer ", "");

            jwtUtils.validateTokenAndGetUid(jwtToken); // 验证token

            // 审批申请
            Application application = applicationService.approveApplication(applicationId, approved, rejectionReason);

            // 构建标准化返回
            return buildSuccessResponse(200, application);

        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @Operation(summary = "获取导师待审批申请或管理员获取所有申请", description = "根据角色获取相关的申请信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取申请列表"),
            @ApiResponse(responseCode = "401", description = "token无效或已过期"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingApplications() {
        try {
            // 从上下文中获取当前用户的认证信息
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();

            // 提取 token 并验证
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取用户ID

            User currentUser = userService.getUserByUid(userId);  // 获取当前用户
            List<Application> applications;
            System.out.println("当前用户"+currentUser);

            // 判断角色类型：管理员、导师或学生
            String roleName = currentUser.getRole().getRoleName();
            if ("ADMIN".equals(roleName)) {
                // 管理员获取所有申请
                System.out.println("管理员获取所有申请");
                applications = applicationService.getAllApplications();
            } else if ("TEACHER".equals(roleName)) {
                // 导师获取与自己相关的所有申请
                System.out.println("导师获取与自己相关的所有申请");
                applications = applicationService.getApplicationsByMentorId(currentUser.getId());
            } else if ("STUDENT".equals(roleName)) {
                // 学生获取自己提交的所有申请
                System.out.println("学生获取与自己相关的所有申请");
                applications = applicationService.getApplicationsByStudentId(currentUser.getId());
            } else {
                return buildErrorResponse(403, "无权访问申请信息");
            }

            // 构建标准化返回
            return buildSuccessResponse(200, applications);

        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }


    // 自定义错误响应格式
    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String message) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);  // 状态码

        Map<String, String> errorData = new HashMap<>();
        errorData.put("error", message);
        responseBody.put("data", errorData);

        return ResponseEntity.status(code == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(responseBody);
    }

    // 自定义成功响应格式
    private <T> ResponseEntity<Map<String, Object>> buildSuccessResponse(int code, T data) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);  // 状态码
        responseBody.put("data", data);  // 返回的数据

        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }
}
