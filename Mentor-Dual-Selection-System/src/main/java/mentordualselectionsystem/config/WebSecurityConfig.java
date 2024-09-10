package mentordualselectionsystem.config;

import mentordualselectionsystem.security.CustomAuthenticationProvider;
import mentordualselectionsystem.security.JwtAuthenticationFilter;
import mentordualselectionsystem.security.JwtAuthorizationFilter;
import mentordualselectionsystem.services.UserService;
import mentordualselectionsystem.security.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class WebSecurityConfig {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final CustomAuthenticationProvider customAuthenticationProvider;

    public WebSecurityConfig(UserService userService, JwtUtils jwtUtils, CustomAuthenticationProvider customAuthenticationProvider) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.customAuthenticationProvider = customAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)  // 禁用 CSRF
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // 允许这些端点公开访问
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/user/me").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/hello").permitAll()
                        // 其他任何请求都需要认证
                        .anyRequest().authenticated()
                )
                // 将自定义的 JWT 过滤器添加到过滤器链中
                .addFilter(new JwtAuthenticationFilter(authenticationManager, jwtUtils, userService))
                .addFilter(new JwtAuthorizationFilter(authenticationManager, jwtUtils, userService))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // 使用无状态会话

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 提供 AuthenticationManager Bean
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    public CustomAuthenticationProvider getCustomAuthenticationProvider() {
        return customAuthenticationProvider;
    }
}
