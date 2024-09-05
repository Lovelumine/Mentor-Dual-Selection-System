# Mentor-Dual-Selection-System
用于师生互选，每个老师只能选3个学生，学生可以在系统上提交申请，老师审批后通过，如果老师拒绝，学生可以向下一个老师提出申请。


你可以通过自定义 Spring Security 配置来实现部分 API 不需要登录保护，同时保留其他路径的安全保护。我们将创建一个简单的 "Hello World" API，并确保这个 API 可以被匿名访问，而其他路径依旧需要登录。

###文件结构

```
Mentor-Dual-Selection-System/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── mentordualselectionsystem/
│   │   │       ├── MentorDualSelectionSystemApplication.java  // 主启动类
│   │   │       ├── config/
│   │   │       │   └── WebSecurityConfig.java  // Spring Security自定义配置
│   │   │       ├── controller/
│   │   │       │   └── HelloWorldController.java  // Hello World API控制器
│   └── resources/
│       └── application.properties  // 配置文件
```


通过自定义 `WebSecurityConfig`，我们可以实现对某些 API 路径（如 `/hello`）进行匿名访问，同时对其他路径继续进行安全保护。