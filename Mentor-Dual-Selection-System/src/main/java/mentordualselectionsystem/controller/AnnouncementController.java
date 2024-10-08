package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.mysql.Announcement;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.services.AnnouncementService;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "公告管理接口")
@RequestMapping("/admin/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final JwtUtils jwtUtils; // JWT工具类
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
            @ApiResponse(responseCode = "204", description = "当前没有公告")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAnnouncements() {
        List<Announcement> announcements = announcementService.getAllAnnouncements();
        if (announcements.isEmpty()) {
            return ResponseEntity.ok(formatResponse(204, "当前没有公告"));
        }
        return ResponseEntity.ok(formatResponse(200, announcements));
    }

    @Operation(summary = "创建公告", description = "创建新的公告。无需提供 ID 和 lastModified 字段。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAnnouncement(@RequestBody Announcement announcement, Authentication authentication) {
        ResponseEntity<Map<String, Object>> adminCheckResponse = checkAdmin(authentication);
        if (adminCheckResponse != null) {
            return adminCheckResponse;
        }

        announcement.setLastModified(Instant.now());
        try {
            Announcement createdAnnouncement = announcementService.createAnnouncement(announcement);
            return ResponseEntity.status(201).body(formatResponse(201, createdAnnouncement));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(formatResponse(500, "服务器内部错误：" + e.getMessage()));
        }
    }

    @Operation(summary = "更新公告", description = "根据公告ID更新公告。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "公告未找到")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(@PathVariable Integer id, @RequestBody Announcement announcement, Authentication authentication) {
        ResponseEntity<Map<String, Object>> adminCheckResponse = checkAdmin(authentication);
        if (adminCheckResponse != null) {
            return adminCheckResponse;
        }

        announcement.setLastModified(Instant.now());
        Announcement updatedAnnouncement = announcementService.updateAnnouncement(id, announcement);
        if (updatedAnnouncement == null) {
            return ResponseEntity.status(404).body(formatResponse(404, "公告未找到"));
        }
        return ResponseEntity.ok(formatResponse(200, updatedAnnouncement));
    }

    @Operation(summary = "删除公告", description = "根据公告ID删除公告。删除成功后返回204状态。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "公告未找到")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAnnouncement(@PathVariable Integer id, Authentication authentication) {
        ResponseEntity<Map<String, Object>> adminCheckResponse = checkAdmin(authentication);
        if (adminCheckResponse != null) {
            return adminCheckResponse;
        }

        boolean isDeleted = announcementService.deleteAnnouncement(id);
        if (!isDeleted) {
            return ResponseEntity.status(404).body(formatResponse(404, "公告未找到"));
        }
        return ResponseEntity.ok(formatResponse(200, "公告已删除成功"));
    }

    private ResponseEntity<Map<String, Object>> checkAdmin(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(200).body(formatResponse(401, "当前的token无效"));
        }

        String jwtToken = (String) authentication.getCredentials();
        Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);
        User currentUser = userService.getUserByUid(userId);

        String roleName = currentUser.getRole().getRoleName();
        if (!"ADMIN".equals(roleName)) {
            return ResponseEntity.status(200).body(formatResponse(403, "当前用户不是管理员，无法执行此操作"));
        }
        return null; // 返回null表示用户是管理员
    }

    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
