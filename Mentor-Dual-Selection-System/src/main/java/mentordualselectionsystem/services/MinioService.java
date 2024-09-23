package mentordualselectionsystem.services;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String defaultBucketName;

    @Value("${minio.url}")
    private String minioUrl;

    public String uploadFile(String username, MultipartFile file) {
        try {
            // 检查文件大小是否超过 50MB
            if (file.getSize() > 50 * 1024 * 1024) {
                return "文件上传失败：文件大小超过50MB限制。";
            }

            // 使用 username 作为目录
            String fileName = username + "/" + file.getOriginalFilename();
            InputStream inputStream = file.getInputStream();

            // 上传文件到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucketName)
                            .object(fileName)
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 返回文件访问链接
            String fileUrl = minioUrl + "/" + defaultBucketName + "/" + fileName;
            return fileUrl;
        } catch (MinioException e) {
            e.printStackTrace();
            return "文件上传失败：MinIO 服务异常 - " + e.getMessage();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return "文件上传失败：无效的秘钥 - " + e.getMessage();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "文件上传失败：不支持的加密算法 - " + e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return "文件上传失败：文件读取异常 - " + e.getMessage();
        }
    }
}
