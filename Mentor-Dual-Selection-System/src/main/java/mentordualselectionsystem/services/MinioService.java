package mentordualselectionsystem.services;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Service
public class MinioService {
    private static final int THUMBNAIL_MAX_SIZE = 320;
    private static final String THUMBNAIL_SUFFIX = ".thumb.jpg";

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
            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType();

            // 上传文件到 MinIO
            uploadBytes(fileName, fileBytes, contentType);

            if (isImageFile(file.getOriginalFilename(), contentType)) {
                byte[] thumbnailBytes = createThumbnail(fileBytes);
                if (thumbnailBytes != null) {
                    uploadBytes(buildThumbnailObjectName(fileName), thumbnailBytes, "image/jpeg");
                }
            }

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

    private void uploadBytes(String objectName, byte[] bytes, String contentType)
            throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucketName)
                            .object(objectName)
                            .stream(inputStream, bytes.length, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
        }
    }

    private boolean isImageFile(String originalFilename, String contentType) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }

        if (originalFilename == null) {
            return false;
        }

        String lower = originalFilename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".bmp")
                || lower.endsWith(".gif");
    }

    private byte[] createThumbnail(byte[] fileBytes) throws IOException {
        BufferedImage sourceImage;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            sourceImage = ImageIO.read(inputStream);
        }

        if (sourceImage == null) {
            return null;
        }

        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        double scale = Math.min((double) THUMBNAIL_MAX_SIZE / width, (double) THUMBNAIL_MAX_SIZE / height);
        scale = Math.min(scale, 1.0d);

        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(thumbnail, "jpg", outputStream);
            return outputStream.toByteArray();
        }
    }

    private String buildThumbnailObjectName(String objectName) {
        return objectName + THUMBNAIL_SUFFIX;
    }
}
