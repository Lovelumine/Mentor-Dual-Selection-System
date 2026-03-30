package mentordualselectionsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.dto.GenerateApplicationReasonRequest;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.services.UserDetailService;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.DeepSeekApplicationReasonService;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
    private final DeepSeekApplicationReasonService deepSeekApplicationReasonService;
    private final ObjectMapper objectMapper;

    @Autowired
    public StudentController(
            UserDetailService userDetailService,
            JwtUtils jwtUtils,
            UserService userService,
            DeepSeekApplicationReasonService deepSeekApplicationReasonService,
            ObjectMapper objectMapper
    ) {
        this.userDetailService = userDetailService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.deepSeekApplicationReasonService = deepSeekApplicationReasonService;
        this.objectMapper = objectMapper;
    }


    @Operation(summary = "获取所有教师详细信息", description = "返回所有教师的详细信息列表。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
    })
    @GetMapping("/teachers")
    public ResponseEntity<Map<String, Object>> getAllTeacherDetails() {
        List<UserDetail> teacherDetails = userDetailService.getAllTeacherDetails();
        return ResponseEntity.ok(formatResponse(200, teacherDetails));
    }


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
            return ResponseEntity.ok(formatResponse(404, "当前还没有编辑个人信息"));
        }
        return ResponseEntity.ok(formatResponse(200, userDetail));
    }


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

    @Operation(summary = "生成申请理由", description = "学生选择导师后，通过服务端调用 DeepSeek 生成申请理由。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "生成成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "403", description = "当前用户不是学生"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "503", description = "DeepSeek 服务不可用")
    })
    @PostMapping("/generate-application-reason")
    public ResponseEntity<Map<String, Object>> generateApplicationReason(
            Authentication authentication,
            @RequestBody GenerateApplicationReasonRequest request
    ) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);
        User currentUser = userService.getUserByUid(userId);

        if (!"STUDENT".equals(currentUser.getRole().getRoleName())) {
            return ResponseEntity.status(403).body(formatResponse(403, "当前用户不是学生，无法生成申请理由"));
        }

        try {
            String generatedReason = deepSeekApplicationReasonService.generateReason(userId, request.getMentorId());
            return ResponseEntity.ok(formatResponse(200, generatedReason));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(formatResponse(400, Map.of("error", e.getMessage())));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(formatResponse(503, Map.of("error", e.getMessage())));
        }
    }

    @Operation(summary = "流式生成申请理由", description = "学生选择导师后，通过服务端调用 DeepSeek，并以流式事件逐步返回申请理由。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "流式生成已开始"),
            @ApiResponse(responseCode = "403", description = "当前用户不是学生"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping(value = "/generate-application-reason/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> generateApplicationReasonStream(
            Authentication authentication,
            @RequestBody GenerateApplicationReasonRequest request
    ) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body(outputStream -> writeSseEvent(outputStream, "error", Map.of("message", "当前的token无效")));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);
        User currentUser = userService.getUserByUid(userId);

        if (!"STUDENT".equals(currentUser.getRole().getRoleName())) {
            return ResponseEntity.status(403).body(outputStream -> writeSseEvent(outputStream, "error", Map.of("message", "当前用户不是学生，无法生成申请理由")));
        }

        StreamingResponseBody body = outputStream -> {
            try {
                deepSeekApplicationReasonService.streamReason(userId, request.getMentorId(), chunk ->
                        writeSseEvent(outputStream, "chunk", Map.of("content", chunk))
                );
                writeSseEvent(outputStream, "done", Map.of("done", true));
            } catch (IllegalArgumentException | IllegalStateException e) {
                writeSseEvent(outputStream, "error", Map.of("message", e.getMessage()));
            } catch (Exception e) {
                writeSseEvent(outputStream, "error", Map.of("message", "生成申请理由失败，请稍后重试"));
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }

    private void writeSseEvent(OutputStream outputStream, String event, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String message = "event: " + event + "\n" + "data: " + json + "\n\n";
            outputStream.write(message.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException ignored) {
        }
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
