# 导师双选系统

**导师双选系统**用于实现导师与学生之间的互选。每位导师最多可选择3名学生，学生通过系统提交申请，导师审核并决定是否通过。如果导师拒绝申请，学生可以继续申请其他导师。

## 功能特点
- **导师选择学生**：导师可以从学生提交的申请中选择合适的学生。
- **学生申请**：学生可以向多位导师提交申请，导师进行审批。
- **重新申请**：若申请被拒，学生可继续申请其他导师。

## 文件结构

```
Mentor-Dual-Selection-System/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── mentordualselectionsystem/
│   │   │       ├── MentorDualSelectionSystemApplication.java  // 启动类
│   │   │       ├── config/
│   │   │       │   └── WebSecurityConfig.java  // 安全配置
│   │   │       ├── controller/
│   │   │       │   └── HelloWorldController.java  // 测试API
│   └── resources/
│       └── application.properties  // 配置文件
```
