package mentordualselectionsystem.controller;

import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.services.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserDetailService userDetailService;

    @Autowired
    public AdminController(UserDetailService userDetailService) {
        this.userDetailService = userDetailService;
    }

    // 获取所有教师的详细信息
    @GetMapping("/teachers")
    public List<UserDetail> getAllTeacherDetails() {
        return userDetailService.getAllTeacherDetails();
    }

    // 获取所有学生的详细信息
    @GetMapping("/students")
    public List<UserDetail> getAllStudentDetails() {
        return userDetailService.getAllStudentDetails();
    }

    // 根据UID获取用户详细信息
    @GetMapping("/user-detail/{uid}")
    public UserDetail getUserDetailByUid(@PathVariable Long uid) {
        return userDetailService.getUserDetailByUid(uid);
    }
}
