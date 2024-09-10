package mentordualselectionsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.services.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final JwtUtils jwtUtils;
    private final UserService userService; // 引入 UserService

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserService userService) {
        super(authenticationManager);
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.replace("Bearer ", "");

        // 使用 JwtUtils 从 token 中解析出 uid 而不是 username
        Long uid = jwtUtils.validateTokenAndGetUid(token);

        if (uid != null) {
            // 从 UserService 中通过 uid 获取自定义的 User 实体
            User user = userService.getUserByUid(uid);

            // 使用 UserDetails 进行授权
            UserDetails userDetails = userService.loadUserByUsername(user.getUsername());

            // 创建一个基于 UserDetails 的认证对象
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, token, userDetails.getAuthorities()
            );

            // 将认证信息存储在 SecurityContext 中
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }
}
