package mentordualselectionsystem.controller;

import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportTemplateController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    public ImportTemplateController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/template/{kind}")
    public ResponseEntity<?> downloadTemplate(@PathVariable String kind) {
        try {
            User currentUser = requireAdmin();
            if (currentUser == null) {
                return buildErrorResponse(401, "token无效或已过期");
            }

            String normalizedKind = kind == null ? "" : kind.trim().toLowerCase();
            if (!"teacher".equals(normalizedKind) && !"student".equals(normalizedKind)) {
                return buildErrorResponse(400, "不支持的模板类型");
            }

            Path scriptPath = resolveScriptPath();
            String fileName = "teacher".equals(normalizedKind) ? "导师信息导入模板.xlsx" : "学生信息导入模板.xlsx";
            Path tempFile = Files.createTempFile("mentor-dual-selection-template-", ".xlsx");

            try {
                Process process = new ProcessBuilder(
                        "python3",
                        scriptPath.toString(),
                        normalizedKind,
                        tempFile.toString()
                ).redirectErrorStream(true).start();

                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int exitCode = process.waitFor();

                if (exitCode != 0 || !Files.exists(tempFile) || Files.size(tempFile) == 0) {
                    return buildErrorResponse(500, "模板生成失败: " + output.trim());
                }

                byte[] bytes = Files.readAllBytes(tempFile);
                ByteArrayResource resource = new ByteArrayResource(bytes);
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .contentLength(bytes.length)
                        .body(resource);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(403, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(500, "模板下载失败: " + e.getMessage());
        }
    }

    private User requireAdmin() {
        try {
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            Long uid = jwtUtils.validateTokenAndGetUid(jwtToken);
            User currentUser = userService.getUserByUid(uid);

            if (currentUser == null) {
                return null;
            }

            if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
                throw new IllegalArgumentException("只有管理员可以下载导入模板");
            }

            return currentUser;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    private Path resolveScriptPath() {
        Path currentDir = Path.of("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                currentDir.resolve("scripts/generate_import_template.py"),
                currentDir.resolve("../scripts/generate_import_template.py"),
                currentDir.resolve("../../scripts/generate_import_template.py")
        );

        return candidates.stream()
                .map(Path::normalize)
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到模板生成脚本"));
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String message) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);
        Map<String, String> errorData = new HashMap<>();
        errorData.put("error", message);
        responseBody.put("data", errorData);

        HttpStatus status = switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return ResponseEntity.status(status).body(responseBody);
    }
}
