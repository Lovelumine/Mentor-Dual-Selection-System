package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.mysql.Announcement;
import mentordualselectionsystem.services.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
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
            return ResponseEntity.ok(formatResponse(204, "当前没有公告")); // 改为200 OK
        }
        return ResponseEntity.ok(formatResponse(200, announcements));
    }

    @Operation(summary = "创建公告", description = "创建新的公告。无需提供 ID 和 lastModified 字段。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAnnouncement(@RequestBody Announcement announcement) {
        announcement.setLastModified(Instant.now()); // 设置当前时间为lastModified
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
    public ResponseEntity<Map<String, Object>> updateAnnouncement(@PathVariable Integer id, @RequestBody Announcement announcement) {
        announcement.setLastModified(Instant.now()); // 更新时也设置当前时间
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
    public ResponseEntity<Map<String, Object>> deleteAnnouncement(@PathVariable Integer id) {
        boolean isDeleted = announcementService.deleteAnnouncement(id);
        if (!isDeleted) {
            return ResponseEntity.status(404).body(formatResponse(404, "公告未找到"));
        }
        return ResponseEntity.ok(formatResponse(200, "公告已删除成功")); // 改为200 OK
    }

    private Map<String, Object> formatResponse(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
