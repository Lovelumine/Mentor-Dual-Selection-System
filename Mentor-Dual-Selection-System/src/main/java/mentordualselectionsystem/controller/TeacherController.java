package mentordualselectionsystem.controller;

import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.services.UserDetailService;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    private final UserDetailService userDetailService;
    private final UserService userService;  // 新增 UserService

    @Autowired
    public TeacherController(UserDetailService userDetailService, UserService userService) {
        this.userDetailService = userDetailService;
        this.userService = userService;  // 初始化 UserService
    }

    // 获取某学生的详细信息（只能查看自己学生的详细信息）
    @GetMapping("/student/{uid}")
    public UserDetail getStudentDetail(Authentication authentication, @PathVariable Long uid) {
        User teacher = (User) authentication.getPrincipal();
        User student = userService.getUserByUid(uid); // 根据 UID 获取学生信息

        // 判断该学生是否属于该导师
        if (student == null || !teacher.getId().equals(student.getMentorId())) {
            throw new RuntimeException("无权查看该学生的详细信息");
        }

        // 获取学生的详细信息
        return userDetailService.getUserDetailByUid(uid);
    }

    // 获取教师自己的详细信息
    @GetMapping("/my-detail")
    public UserDetail getMyDetail(Authentication authentication) {
        User teacher = (User) authentication.getPrincipal();
        return userDetailService.getUserDetailByUid(teacher.getId());
    }
}
