package mentordualselectionsystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.security.SignatureException;
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

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            // 从请求体中解析用户名和密码
            LoginRequest loginRequest = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);
            String username = loginRequest.getUsername();

            // 构建认证对象
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword(), Collections.emptyList());
            return authenticationManager.authenticate(authenticationToken);
        } catch (IOException e) {
            // 返回 400 错误
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
        String username = authResult.getName();

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
        // 构建错误响应
        buildErrorResponse(response, 401, "密码错误");
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
