package mentordualselectionsystem.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class DeepSeekApplicationReasonService {

    private final UserService userService;
    private final UserDetailService userDetailService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.ai.deepseek.base-url:https://api.deepseek.com}")
    private String deepSeekBaseUrl;

    @Value("${app.ai.deepseek.api-key:}")
    private String deepSeekApiKey;

    @Value("${app.ai.deepseek.model:deepseek-chat}")
    private String deepSeekModel;

    public DeepSeekApplicationReasonService(
            UserService userService,
            UserDetailService userDetailService,
            ObjectMapper objectMapper
    ) {
        this.userService = userService;
        this.userDetailService = userDetailService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String generateReason(Long studentId, Long mentorId) {
        if (mentorId == null) {
            throw new IllegalArgumentException("导师信息缺失，无法生成申请理由");
        }
        if (deepSeekApiKey == null || deepSeekApiKey.isBlank()) {
            throw new IllegalStateException("DeepSeek API 尚未配置，请先在服务端环境变量中设置");
        }

        User student = userService.getUserByUid(studentId);
        if (!"STUDENT".equals(student.getRole().getRoleName())) {
            throw new IllegalArgumentException("当前用户不是学生，无法生成申请理由");
        }

        User mentor = userService.getUserByUid(mentorId);
        if (!"TEACHER".equals(mentor.getRole().getRoleName())) {
            throw new IllegalArgumentException("目标导师不存在或角色类型错误");
        }

        UserDetail studentDetail = userDetailService.getUserDetailByUid(studentId);
        UserDetail mentorDetail = userDetailService.getUserDetailByUid(mentorId);

        String systemPrompt = """
                你是一名中文申请文书助手。
                请根据给定的学生信息和导师信息，生成一段适合导师双选系统提交的中文申请理由。
                要求：
                1. 只输出申请理由正文，不要标题，不要分点，不要引号。
                2. 语气真诚、自然、具体，避免空泛套话。
                3. 长度控制在120到220字。
                4. 不要虚构未提供的科研经历或奖项。
                """;

        String userPrompt = """
                学生信息：
                姓名：%s
                年级：%s
                班级：%s
                简介：%s

                导师信息：
                姓名：%s
                职位：%s
                研究方向：%s
                简历：%s
                """.formatted(
                safeText(student.getFullName()),
                studentDetail != null ? safeText(studentDetail.getStudentGrade()) : "未填写",
                studentDetail != null && studentDetail.getStudentClass() != null ? String.valueOf(studentDetail.getStudentClass()) : "未填写",
                studentDetail != null ? safeText(studentDetail.getResume()) : "未填写",
                safeText(mentor.getFullName()),
                mentorDetail != null ? safeText(mentorDetail.getTeacherPosition()) : "未填写",
                mentorDetail != null ? safeText(mentorDetail.getResearchDirection()) : "未填写",
                mentorDetail != null ? safeText(mentorDetail.getResume()) : "未填写"
        );

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", deepSeekModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 512
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveCompletionsUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepSeekApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode responseJson = objectMapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String errorMessage = normalizeProviderErrorMessage(
                        responseJson.path("error").path("message").asText(),
                        response.statusCode()
                );
                throw new IllegalStateException(errorMessage);
            }

            String generatedReason = responseJson.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("")
                    .trim();

            if (generatedReason.isBlank()) {
                throw new IllegalStateException("DeepSeek 已返回响应，但未生成有效内容");
            }

            return generatedReason;
        } catch (IOException e) {
            throw new IllegalStateException("解析 DeepSeek 响应失败，请稍后重试", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用 DeepSeek 时被中断，请稍后重试", e);
        }
    }

    public void streamReason(Long studentId, Long mentorId, Consumer<String> chunkConsumer) {
        if (mentorId == null) {
            throw new IllegalArgumentException("导师信息缺失，无法生成申请理由");
        }
        if (deepSeekApiKey == null || deepSeekApiKey.isBlank()) {
            throw new IllegalStateException("DeepSeek API 尚未配置，请先在服务端环境变量中设置");
        }

        User student = userService.getUserByUid(studentId);
        if (!"STUDENT".equals(student.getRole().getRoleName())) {
            throw new IllegalArgumentException("当前用户不是学生，无法生成申请理由");
        }

        User mentor = userService.getUserByUid(mentorId);
        if (!"TEACHER".equals(mentor.getRole().getRoleName())) {
            throw new IllegalArgumentException("目标导师不存在或角色类型错误");
        }

        UserDetail studentDetail = userDetailService.getUserDetailByUid(studentId);
        UserDetail mentorDetail = userDetailService.getUserDetailByUid(mentorId);

        String systemPrompt = """
                你是一名中文申请文书助手。
                请根据给定的学生信息和导师信息，生成一段适合导师双选系统提交的中文申请理由。
                要求：
                1. 只输出申请理由正文，不要标题，不要分点，不要引号。
                2. 语气真诚、自然、具体，避免空泛套话。
                3. 长度控制在120到220字。
                4. 不要虚构未提供的科研经历或奖项。
                """;

        String userPrompt = """
                学生信息：
                姓名：%s
                年级：%s
                班级：%s
                简介：%s

                导师信息：
                姓名：%s
                职位：%s
                研究方向：%s
                简历：%s
                """.formatted(
                safeText(student.getFullName()),
                studentDetail != null ? safeText(studentDetail.getStudentGrade()) : "未填写",
                studentDetail != null && studentDetail.getStudentClass() != null ? String.valueOf(studentDetail.getStudentClass()) : "未填写",
                studentDetail != null ? safeText(studentDetail.getResume()) : "未填写",
                safeText(mentor.getFullName()),
                mentorDetail != null ? safeText(mentorDetail.getTeacherPosition()) : "未填写",
                mentorDetail != null ? safeText(mentorDetail.getResearchDirection()) : "未填写",
                mentorDetail != null ? safeText(mentorDetail.getResume()) : "未填写"
        );

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", deepSeekModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 512,
                    "stream", true
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveCompletionsUrl()))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + deepSeekApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 400) {
                String responseBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                String errorMessage = normalizeProviderErrorMessage(
                        objectMapper.readTree(responseBody).path("error").path("message").asText(),
                        response.statusCode()
                );
                throw new IllegalStateException(errorMessage);
            }

            try (
                    InputStream inputStream = response.body();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }

                    String data = line.substring(5).trim();
                    if (data.isBlank()) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        break;
                    }

                    JsonNode chunkJson = objectMapper.readTree(data);
                    String content = chunkJson.path("choices")
                            .path(0)
                            .path("delta")
                            .path("content")
                            .asText("");

                    if (!content.isBlank()) {
                        chunkConsumer.accept(content);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("解析 DeepSeek 响应失败，请稍后重试", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用 DeepSeek 时被中断，请稍后重试", e);
        }
    }

    private String resolveCompletionsUrl() {
        String normalizedBaseUrl = deepSeekBaseUrl.endsWith("/")
                ? deepSeekBaseUrl.substring(0, deepSeekBaseUrl.length() - 1)
                : deepSeekBaseUrl;
        return normalizedBaseUrl + "/chat/completions";
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "未填写";
        }
        return value.trim();
    }

    private String normalizeProviderErrorMessage(String providerMessage, int statusCode) {
        String normalized = providerMessage == null ? "" : providerMessage.trim();
        String lowerCaseMessage = normalized.toLowerCase();

        if (lowerCaseMessage.contains("insufficient balance")
                || lowerCaseMessage.contains("insufficient_balance")
                || lowerCaseMessage.contains("余额不足")
                || lowerCaseMessage.contains("quota")) {
            return "AI 接口余额不足，请充值或更换可用的 API Key";
        }

        if (lowerCaseMessage.contains("invalid api key")
                || lowerCaseMessage.contains("authentication")
                || lowerCaseMessage.contains("unauthorized")
                || statusCode == 401) {
            return "AI 接口认证失败，请检查 API Key 是否正确";
        }

        if (lowerCaseMessage.contains("rate limit")
                || statusCode == 429) {
            return "AI 接口调用过于频繁，请稍后再试";
        }

        if (lowerCaseMessage.contains("context length")
                || lowerCaseMessage.contains("max context")) {
            return "提交给 AI 的内容过长，请精简后重试";
        }

        if (!normalized.isBlank()) {
            return "AI 生成失败：" + normalized;
        }

        return "AI 接口调用失败，状态码：" + statusCode;
    }
}
