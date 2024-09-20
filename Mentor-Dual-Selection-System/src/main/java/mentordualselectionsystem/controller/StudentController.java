package mentordualselectionsystem.controller;

import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.services.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        User student = (User) authentication.getPrincipal();
        return userDetailService.getUserDetailByUid(student.getId());
    }
}