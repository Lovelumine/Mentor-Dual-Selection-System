package mentordualselectionsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获 403 Forbidden 异常
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        // 构建返回的 JSON 格式
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 403);  // 错误状态码
        Map<String, String> errorData = new HashMap<>();
        errorData.put("error", "Access denied. You don't have the necessary permissions.");
        responseBody.put("data", errorData);

        // 返回自定义的 403 错误响应
        return new ResponseEntity<>(responseBody, HttpStatus.FORBIDDEN);
    }

    // 其他异常处理器可以根据需要添加
}
