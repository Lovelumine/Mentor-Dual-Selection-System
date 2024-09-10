package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Operation(summary = "用户登录", description = "验证用户身份并返回 JWT 令牌。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal(); // 获取用户信息
        String token = jwtUtils.generateToken(Long.valueOf(user.getId().toString()));  // 使用用户的 uid 生成 token

        // 将返回值改为 {code: "", token: ""}
        Map<String, String> response = new HashMap<>();
        response.put("code", "200");  // 状态码
        response.put("token", token); // 返回 JWT Token

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "用户注册", description = "注册新用户。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "输入数据无效")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        userService.saveUser(user);

        // 将返回值改为 {code: "", message: ""}
        Map<String, String> response = new HashMap<>();
        response.put("code", "200");  // 状态码
        response.put("message", "用户注册成功");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "获取用户信息", description = "根据 JWT 令牌获取用户的详细信息。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户信息"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String token) {
        // 提取 token 并验证
        String jwtToken = token.replace("Bearer ", "");
        Long uid = Long.valueOf(jwtUtils.validateToken(jwtToken));  // 使用 uid 获取用户信息

        // 根据 uid 获取用户信息
        User user = userService.getUserByUid(uid);

        // 构建返回的 JSON 格式，包含用户信息
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 200);  // 状态码

        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("role", user.getRole().getRoleName());

        responseBody.put("data", data);

        return ResponseEntity.ok(responseBody);
    }
}
