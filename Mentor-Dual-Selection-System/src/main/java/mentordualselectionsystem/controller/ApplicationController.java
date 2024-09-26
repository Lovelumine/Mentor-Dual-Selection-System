package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@Tag(name = "申请相关接口")
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
            description = "导师通过或拒绝学生的申请。**注意：导师拒绝申请时，必须填写拒绝理由；同意申请时，拒绝理由可选。**",
            parameters = {
                    @Parameter(name = "applicationId", description = "申请的ID", required = true),
                    @Parameter(name = "approved", description = "是否同意", required = true),
                    @Parameter(
                            name = "rejectionReason",
                            description = "拒绝理由，**拒绝申请时必填，同意时可选**",
                            required = false,
                            schema = @Schema(type = "string")
                    )
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

            // 验证 token 并获取用户 ID
            Long mentorId = jwtUtils.validateTokenAndGetUid(jwtToken);

            // 获取申请
            Application application = applicationService.getApplicationById(applicationId);

            // 检查申请是否存在
            if (application == null) {
                return buildErrorResponse(400, "申请不存在");
            }

            // 检查申请状态是否为 PENDING
            if (!"PENDING".equalsIgnoreCase(application.getStatus())) {
                return buildErrorResponse(400, "申请已经终结，无法再次修改，需要学生重新提交申请");
            }

            // 检查当前导师是否有权限审批该申请
            if (!application.getMentorId().equals(mentorId)) {
                return buildErrorResponse(403, "无权审批该申请");
            }

            // **新增检查：拒绝申请时必须填写拒绝理由**
            if (!approved && (rejectionReason == null || rejectionReason.trim().isEmpty())) {
                return buildErrorResponse(400, "拒绝申请时必须填写拒绝理由");
            }

            // 审批申请
            application = applicationService.approveApplication(applicationId, approved, rejectionReason);

            // 构建返回结果
            return buildSuccessResponse(200, application);

        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @Operation(
            summary = "获取所有已存在的导师与学生关系",
            description = "根据当前用户的角色，获取导师与学生之间的关系，学生获取自己的导师，导师获取自己的学生列表，管理员获取所有关系。"
    )
    @GetMapping("/mentor-student-relations")
    public ResponseEntity<Map<String, Object>> getMentorStudentRelationships() {
        try {
            // 获取当前用户的 token
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);  // 验证token并获取用户ID

            User currentUser = userService.getUserByUid(userId);  // 获取当前用户
            String roleName = currentUser.getRole().getRoleName();

            List<Map<String, Object>> mentorStudentRelationships;

            if ("STUDENT".equals(roleName)) {
                // 学生获取自己的导师
                if (currentUser.getMentorId() == null) {
                    return buildErrorResponse(404, "你还没有导师");
                }
                User mentor = userService.getUserByUid(currentUser.getMentorId());

                Map<String, Object> mentorMap = new HashMap<>();
                mentorMap.put("uid", mentor.getUid());
                mentorMap.put("fullName", mentor.getFullName());
                mentorMap.put("email", mentor.getEmail());

                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mentor", mentorMap);

                mentorStudentRelationships = Collections.singletonList(responseMap);
            } else if ("TEACHER".equals(roleName)) {
                // 导师获取自己的学生列表
                List<User> students = userService.getStudentsByMentorId(currentUser.getUid());

                Map<String, Object> mentorMap = new HashMap<>();
                mentorMap.put("uid", currentUser.getUid());
                mentorMap.put("fullName", currentUser.getFullName());
                mentorMap.put("email", currentUser.getEmail());

                List<Map<String, Object>> studentList = students.stream().map(student -> {
                    Map<String, Object> studentMap = new HashMap<>();
                    studentMap.put("uid", student.getUid());
                    studentMap.put("fullName", student.getFullName());
                    studentMap.put("email", student.getEmail());
                    studentMap.put("username", student.getUsername());

                    return studentMap;
                }).collect(Collectors.toList());

                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mentor", mentorMap);
                responseMap.put("students", studentList);

                mentorStudentRelationships = Collections.singletonList(responseMap);
            } else if ("ADMIN".equals(roleName)) {
                // 管理员获取所有导师和学生的关系
                mentorStudentRelationships = userService.getAllMentorStudentRelationships();
            } else {
                return buildErrorResponse(403, "无权访问该信息");
            }

            // 返回结果
            return buildSuccessResponse(200, mentorStudentRelationships);

        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @Operation(
            summary = "获取申请信息",
            description = "根据当前用户的角色获取相关的申请信息。管理员可以获取所有申请信息，导师可以获取与自己相关的申请，学生可以获取自己提交的申请。")
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
                applications = applicationService.getApplicationsByMentorId(currentUser.getUid());
            } else if ("STUDENT".equals(roleName)) {
                applications = applicationService.getApplicationsByStudentId(currentUser.getUid());
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

    // 自定义成功响应格式
    private <T> ResponseEntity<Map<String, Object>> buildSuccessResponse(int code, T data) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);  // 状态码
        responseBody.put("data", data);  // 返回的数据

        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }
}
