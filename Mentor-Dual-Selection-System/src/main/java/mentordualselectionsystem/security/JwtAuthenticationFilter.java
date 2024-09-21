package mentordualselectionsystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.security.SignatureException;
import lombok.SneakyThrows;
import mentordualselectionsystem.controller.LoginRequest;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.services.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    private final Map<String, Integer> failedLoginAttempts = new HashMap<>();
    private final Map<String, Long> lockedUsers = new HashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME = 5 * 60 * 1000; // 5分钟

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        setFilterProcessesUrl("/api/auth/login");
    }

    @SneakyThrows
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginRequest loginRequest = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);
            String username = loginRequest.getUsername();

            if (isUserLocked(username)) {
                long remainingLockTime = (LOCK_TIME - (System.currentTimeMillis() - lockedUsers.get(username))) / 1000;
                buildErrorResponse(response, 403, "账户被锁定，请在 " + remainingLockTime + " 秒后重试。");
                return null;
            }

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword(), Collections.emptyList());
            return authenticationManager.authenticate(authenticationToken);
        } catch (IOException e) {
            buildErrorResponse(response, 400, "无效的登录请求格式");
            return null;
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        String username = authResult.getName();
        if (isUserLocked(username)) {
            long remainingLockTime = (LOCK_TIME - (System.currentTimeMillis() - lockedUsers.get(username))) / 1000;
            buildErrorResponse(response, 403, "账户被锁定，请在 " + remainingLockTime + " 秒后重试。");
            return;
        }

        try {
            User user = userService.getUserByUsername(username);
            String token = jwtUtils.generateToken(Long.valueOf(user.getUid().toString()));

            response.addHeader("Authorization", "Bearer " + token);
            response.setContentType("application/json;charset=UTF-8");

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("code", 200);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("uid", user.getUid());
            responseBody.put("data", data);

            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(responseBody));
        } catch (SignatureException e) {
            buildErrorResponse(response, 401, "token无效或已过期");
        } catch (Exception e) {
            buildErrorResponse(response, 500, "生成 JWT 过程中出现错误");
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        String username = request.getParameter("username");
        if (isUserLocked(username)) {
            long remainingLockTime = (LOCK_TIME - (System.currentTimeMillis() - lockedUsers.get(username))) / 1000;
            buildErrorResponse(response, 403, "账户被锁定，请在 " + remainingLockTime + " 秒后重试。");
            return;
        }

        int attempts = failedLoginAttempts.getOrDefault(username, 0) + 1;
        failedLoginAttempts.put(username, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            lockedUsers.put(username, System.currentTimeMillis());
            buildErrorResponse(response, 403, "账户已被锁定，请在 " + (LOCK_TIME / 1000) + " 秒后重试。");
        } else {
            buildErrorResponse(response, 401, "密码错误");
        }
    }

    private boolean isUserLocked(String username) {
        Long lockTime = lockedUsers.get(username);
        if (lockTime != null) {
            long elapsedTime = System.currentTimeMillis() - lockTime;
            if (elapsedTime < LOCK_TIME) {
                return true; // 用户仍然被锁定
            } else {
                lockedUsers.remove(username); // 超过锁定时间，解除锁定
                failedLoginAttempts.remove(username); // 重置失败尝试次数
            }
        }
        return false;
    }

    private void buildErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);
        Map<String, String> data = new HashMap<>();
        data.put("error", message);
        responseBody.put("data", data);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}
