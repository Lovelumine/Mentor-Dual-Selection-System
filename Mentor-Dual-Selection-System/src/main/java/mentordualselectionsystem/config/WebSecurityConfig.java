package mentordualselectionsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                        .requestMatchers("/hello").permitAll()  // 允许匿名访问 /hello
                        .requestMatchers(
                                "/swagger-ui/**",   // 允许匿名访问 Swagger UI
                                "/v3/api-docs/**",  // 允许匿名访问 OpenAPI 文档
                                "/swagger-ui.html"  // 允许匿名访问 Swagger UI 页面
                        ).permitAll()
                        .anyRequest().authenticated()          // 其他请求需要身份验证
                )
                .formLogin(Customizer.withDefaults())      // 启用默认登录页面
                .logout(Customizer.withDefaults());        // 启用默认登出页面
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.builder()
                        .username("user")
                        .password(passwordEncoder().encode("password"))
                        .roles("USER")
                        .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
