package mentordualselectionsystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mentordualselectionsystem.controller.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        setFilterProcessesUrl("/api/auth/login");  // 设置登录端点
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            // 从请求体中解析用户名和密码
            LoginRequest loginRequest = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword(), Collections.emptyList());
            return authenticationManager.authenticate(authenticationToken);
        } catch (IOException e) {
            // 返回 400 错误并带有统一格式的中文错误消息
            try {
                buildErrorResponse(response, 400, "无效的登录请求格式");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return null;
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        Object principal = authResult.getPrincipal();
        String username;

        if (principal instanceof org.springframework.security.core.userdetails.User) {
            username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
        } else {
            // 处理 principal 作为 String 的情况（即用户名）
            username = principal.toString();
        }

        String token = jwtUtils.generateToken(username);  // 使用 JWT 工具类生成 token

        // 设置响应头和类型
        response.addHeader("Authorization", "Bearer " + token);
        response.setContentType("application/json;charset=UTF-8");

        // 构建返回的 JSON 格式，包含 code 和 data
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 200);  // 状态码
        Map<String, String> data = new HashMap<>();
        data.put("token", token);  // 返回生成的 JWT 令牌
        responseBody.put("data", data);

        // 输出 JSON 响应
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));

        // 输出 token 以便调试
        System.out.println("生成的令牌: " + token);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        // 区分密码错误和其他错误，并使用统一格式返回错误
        if (failed instanceof BadCredentialsException) {
            buildErrorResponse(response, 401, "密码错误");
        } else {
            buildErrorResponse(response, 401, "认证失败");
        }
    }

    private void buildErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);  // 返回状态码
        Map<String, String> data = new HashMap<>();
        data.put("error", message);  // 返回错误信息
        responseBody.put("data", data);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}
