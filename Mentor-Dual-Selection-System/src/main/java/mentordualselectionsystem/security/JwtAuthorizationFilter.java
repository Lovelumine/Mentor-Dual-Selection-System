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

        try {
            Long uid = jwtUtils.validateTokenAndGetUid(token);
            if (uid == null) {
                writeUnauthorized(response, "token无效或已过期");
                return;
            }

            User user = userService.getUserByUid(uid);
            if (user == null) {
                writeUnauthorized(response, "token无效或已过期");
                return;
            }

            if (!user.isEnabled()) {
                writeUnauthorized(response, "账号已停用");
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user, token, user.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response, "token无效或已过期");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":401,\"data\":{\"error\":\"%s\"}}", message));
    }
}
