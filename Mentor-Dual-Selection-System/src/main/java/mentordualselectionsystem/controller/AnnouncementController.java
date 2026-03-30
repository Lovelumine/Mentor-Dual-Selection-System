package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.mysql.Announcement;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.AnnouncementService;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "公告管理接口")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @Autowired
    public AnnouncementController(AnnouncementService announcementService, JwtUtils jwtUtils, UserService userService) {
        this.announcementService = announcementService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @Operation(summary = "获取所有公告", description = "返回所有公告的列表。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/api/announcements")
    public ResponseEntity<Map<String, Object>> getAllAnnouncements() {
        List<Announcement> announcements = announcementService.getAllAnnouncements();
        return ResponseEntity.ok(formatResponse(200, announcements));
    }

    @Operation(summary = "创建公告", description = "创建新的公告。无需提供 ID 和 lastModified 字段。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "当前用户不是管理员"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/admin/announcements")
    public ResponseEntity<Map<String, Object>> createAnnouncement(@RequestBody Announcement announcement, Authentication authentication) {
        ResponseEntity<Map<String, Object>> adminCheckResponse = checkAdmin(authentication);
        if (adminCheckResponse != null) {
            return adminCheckResponse;
        }

        announcement.setLastModified(Instant.now());

        try {
            Announcement createdAnnouncement = announcementService.createAnnouncement(announcement);
            return ResponseEntity.status(HttpStatus.CREATED).body(formatResponse(201, createdAnnouncement));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 500, "服务器内部错误：" + e.getMessage());
        }
    }

    @Operation(summary = "更新公告", description = "根据公告 ID 更新公告。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "当前用户不是管理员"),
            @ApiResponse(responseCode = "404", description = "公告未找到")
    })
    @PutMapping("/admin/announcements/{id}")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(@PathVariable Integer id, @RequestBody Announcement announcement, Authentication authentication) {
        ResponseEntity<Map<String, Object>> adminCheckResponse = checkAdmin(authentication);
        if (adminCheckResponse != null) {
            return adminCheckResponse;
        }

        announcement.setLastModified(Instant.now());
        Announcement updatedAnnouncement = announcementService.updateAnnouncement(id, announcement);
        if (updatedAnnouncement == null) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, 404, "公告未找到");
        }

        return ResponseEntity.ok(formatResponse(200, updatedAnnouncement));
    }

    @Operation(summary = "删除公告", description = "根据公告 ID 删除公告。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "当前用户不是管理员"),
            @ApiResponse(responseCode = "404", description = "公告未找到")
    })
    @DeleteMapping("/admin/announcements/{id}")
    public ResponseEntity<Map<String, Object>> deleteAnnouncement(@PathVariable Integer id, Authentication authentication) {
        ResponseEntity<Map<String, Object>> adminCheckResponse = checkAdmin(authentication);
        if (adminCheckResponse != null) {
            return adminCheckResponse;
        }

        boolean isDeleted = announcementService.deleteAnnouncement(id);
        if (!isDeleted) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, 404, "公告未找到");
        }

        return ResponseEntity.ok(formatResponse(200, "公告已删除成功"));
    }

    private ResponseEntity<Map<String, Object>> checkAdmin(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, 401, "当前的 token 无效或已过期");
        }

        try {
            String jwtToken = authentication.getCredentials().toString();
            Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);
            User currentUser = userService.getUserByUid(userId);

            if (currentUser == null) {
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, 401, "当前的 token 无效或已过期");
            }

            String roleName = currentUser.getRole().getRoleName();
            if (!"ADMIN".equals(roleName)) {
                return buildErrorResponse(HttpStatus.FORBIDDEN, 403, "当前用户不是管理员，无法执行此操作");
            }

            return null;
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, 401, "当前的 token 无效或已过期");
        }
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(formatResponse(code, errorBody(message)));
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("error", message);
        return errorData;
    }

    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
