package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(
            summary = "提交学生申请",
            description = "学生通过 token 提交申请，提供申请理由以及导师 ID。该接口仅供学生用户使用，系统会通过 JWT Token 确认用户身份。",
            parameters = {
                    @Parameter(name = "mentorId", description = "导师的ID", required = true),
                    @Parameter(name = "reason", description = "申请理由", required = true)
            }
    )
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
            // 获取当前用户 token
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");

            // 通过 token 获取学生ID
            Long studentId = jwtUtils.validateTokenAndGetUid(jwtToken);

            // 提交申请
            Application application = applicationService.submitApplication(studentId, mentorId, reason);

            // 构建返回结果
            return buildSuccessResponse(200, application);

        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @Operation(
            summary = "导师审批学生申请",
            description = "导师通过或拒绝学生的申请，导师可以选择是否填写拒绝理由。",
            parameters = {
                    @Parameter(name = "applicationId", description = "申请的ID", required = true),
                    @Parameter(name = "approved", description = "是否同意", required = true),
                    @Parameter(name = "rejectionReason", description = "拒绝理由，仅在拒绝时提供", required = false)
            }
    )
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
            // 获取当前用户 token
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");

            // 验证 token
            jwtUtils.validateTokenAndGetUid(jwtToken);

            // 审批申请
            Application application = applicationService.approveApplication(applicationId, approved, rejectionReason);

            // 构建返回结果
            return buildSuccessResponse(200, application);

        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @Operation(
            summary = "获取申请信息",
            description = "根据当前用户的角色获取相关的申请信息。管理员可以获取所有申请信息，导师可以获取与自己相关的申请，学生可以获取自己提交的申请。",
            parameters = {
                    @Parameter(name = "Authorization", description = "JWT Token，Bearer token", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取申请列表"),
            @ApiResponse(responseCode = "401", description = "token无效或已过期"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingApplications() {
        try {
            // 获取当前用户 token
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取用户ID

            User currentUser = userService.getUserByUid(userId);  // 获取当前用户
            List<Application> applications;

            // 判断角色类型：管理员、导师或学生
            String roleName = currentUser.getRole().getRoleName();
            if ("ADMIN".equals(roleName)) {
                applications = applicationService.getAllApplications();
            } else if ("TEACHER".equals(roleName)) {
                applications = applicationService.getApplicationsByMentorId(currentUser.getId());
            } else if ("STUDENT".equals(roleName)) {
                applications = applicationService.getApplicationsByStudentId(currentUser.getId());
            } else {
                return buildErrorResponse(403, "无权访问申请信息");
            }

            // 构建返回结果
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
