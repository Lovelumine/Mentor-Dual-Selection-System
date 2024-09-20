package mentordualselectionsystem.controller;

import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.services.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/student")
public class StudentController {

    private final UserDetailService userDetailService;

    @Autowired
    public StudentController(UserDetailService userDetailService) {
        this.userDetailService = userDetailService;
    }

    // 获取所有教师的详细信息
    @GetMapping("/teachers")
    public List<UserDetail> getAllTeacherDetails() {
        return userDetailService.getAllTeacherDetails();
    }

    // 获取学生自己的详细信息
    @GetMapping("/my-detail")
    public UserDetail getMyDetail(Authentication authentication) {
        // 通过用户名获取当前用户
        String username = authentication.getName();
        Optional<User> optionalStudent = userDetailService.getUserByUsername(username);
        if (optionalStudent.isPresent()) {
            User student = optionalStudent.get();
            return userDetailService.getUserDetailByUid(student.getId());
        }
        throw new RuntimeException("用户详细信息未找到");
    }
}
