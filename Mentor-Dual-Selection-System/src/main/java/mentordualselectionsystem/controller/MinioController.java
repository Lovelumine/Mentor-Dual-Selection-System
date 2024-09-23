package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.services.MinioService;
import mentordualselectionsystem.services.UserService;
import mentordualselectionsystem.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MinioController {

    @Autowired
    private MinioService minioService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 上传文件到 MinIO 存储
     * @param file 要上传的文件
     * @param authentication 当前用户的认证信息
     * @return 返回上传文件的URL或者错误信息
     */
    @Operation(
            summary = "上传文件",
            description = "通过此接口上传文件到 MinIO 存储系统，并返回文件的访问链接。文件大小不能超过50MB。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "文件上传成功，返回文件链接"),
            @ApiResponse(responseCode = "400", description = "无效的文件或文件大小超过限制"),
            @ApiResponse(responseCode = "500", description = "文件上传失败")
    })
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return ResponseEntity.status(401).body("当前的token无效");
        }

        String jwtToken = (String) authentication.getCredentials();
        Long currentUid = jwtUtils.validateTokenAndGetUid(jwtToken);

        // 获取当前用户信息
        User currentUser;
        try {
            currentUser = userService.getUserByUid(currentUid);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("当前用户信息不存在");
        }

        String username = currentUser.getUsername();

        // 调用 MinioService 进行文件上传
        String result = minioService.uploadFile(username, file);

        // 根据结果返回相应信息
        if (result.startsWith("文件上传失败")) {
            return ResponseEntity.status(500).body(result);
        } else if (result.startsWith("文件大小超过")) {
            return ResponseEntity.status(400).body(result);
        } else {
            return ResponseEntity.ok(result);
        }
    }
}
