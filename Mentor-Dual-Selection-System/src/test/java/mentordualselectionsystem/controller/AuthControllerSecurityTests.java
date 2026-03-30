package mentordualselectionsystem.controller;

import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.services.PasswordResetService;
import mentordualselectionsystem.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordResetService passwordResetService;

    @Test
    void forgotPasswordShouldBeAccessibleWithoutAuthentication() throws Exception {
        User user = new User();
        user.setUid(1L);
        user.setFullName("系统维护人员");
        user.setEmail("updatedadmin@example.com");
        given(userService.getUserByEmail("updatedadmin@example.com")).willReturn(user);
        doNothing().when(passwordResetService).createPasswordResetTokenForUser(any(User.class));

        mockMvc.perform(post("/api/auth/forgot-password").param("email", "updatedadmin@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("密码重置邮件已发送到 updatedadmin@example.com"));
    }

    @Test
    void resetPasswordViaTokenShouldBeAccessibleWithoutAuthentication() throws Exception {
        User user = new User();
        user.setUid(1L);
        given(passwordResetService.validatePasswordResetToken("abc123")).willReturn(true);
        given(passwordResetService.getUserByPasswordResetToken("abc123")).willReturn(user);
        doNothing().when(passwordResetService).updatePassword(eq(user), eq("new-password"), eq("abc123"));

        mockMvc.perform(post("/api/auth/reset-password-via-token")
                        .param("token", "abc123")
                        .param("newPassword", "new-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("密码已重置"));
    }
}
