package mentordualselectionsystem.test;

import org.springframework.security.crypto.password.PasswordEncoder;

public class BCryptPasswordEncoder {
    public static void main(String[] args) {
        PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String rawPassword = "123"; // 原始密码
        String encodedPassword = encoder.encode(rawPassword); // 加密密码
        System.out.println("Encoded password: " + encodedPassword);
    }
}
