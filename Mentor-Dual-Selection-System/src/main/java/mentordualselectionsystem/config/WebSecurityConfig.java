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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
        // JwtAuthenticationFilter 处理登录并生成 JWT
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager, jwtUtils, userService);
        jwtAuthenticationFilter.setFilterProcessesUrl("/api/auth/login");  // 配置登录路径

        // JwtAuthorizationFilter 验证所有请求中的 JWT
        JwtAuthorizationFilter jwtAuthorizationFilter = new JwtAuthorizationFilter(authenticationManager, jwtUtils, userService);  // 添加 AuthenticationManager 参数

        http
                .csrf(AbstractHttpConfigurer::disable)  // 禁用 CSRF
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // 允许这些端点公开访问
                        .requestMatchers("/api/auth/login", "/api/auth/reset-password","admin/**","/admin").permitAll()
                        .requestMatchers("/api/user/me","/api/user/all","/api/user/update","/api/application/**","api/email/send","/api/auth/forgot-password","/api/auth/reset-password-via-token").permitAll()
                        .requestMatchers("/swagger-ui/**", "/webjars/**","/api-docs/**","v3/api-docs/**","/v3/api-docs", "/v3/api-docs/swagger-config","/swagger-ui.html","/doc.html").permitAll()
                        .requestMatchers("/hello","/favicon.ico","docs.html","/api-docs/","student/**","student","student/my-detail").permitAll()
                        .requestMatchers("teacher","teacher/**","teacher/student/**","upload","upload/**").permitAll()
                        // 其他任何请求都需要认证
                        .anyRequest().authenticated()
                )
                // 将自定义的 JWT 过滤器添加到过滤器链中
                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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

    @Configuration
    public static class WebConfig {

        @Bean
        public WebMvcConfigurer corsConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void addCorsMappings(CorsRegistry registry) {
                    registry.addMapping("/**") // 允许所有路径
                            .allowedOriginPatterns("*") // 允许所有源
                            .allowedMethods("*") // 允许的方法
                            .allowedHeaders("*") // 允许的头
                            .allowCredentials(true); // 允许发送 Cookie
                }
            };
        }
    }
}
