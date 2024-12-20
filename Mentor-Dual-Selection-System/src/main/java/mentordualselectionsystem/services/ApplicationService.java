package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.Application;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.ApplicationRepository;
import mentordualselectionsystem.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Autowired
    public ApplicationService(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    // 每分钟执行一次任务，检查超时的申请
    @Scheduled(cron = "0 * * * * ?")
    public void autoRejectExpiredApplications() {
        // 获取所有状态为 PENDING 的申请
        List<Application> pendingApplications = applicationRepository.findByStatus("PENDING");

        for (Application application : pendingApplications) {
            // 检查是否超时（超过7天未处理）
            if (ChronoUnit.DAYS.between(application.getApplicationDate(), Instant.now()) > 7) {
                // 超时自动拒绝申请，并记录拒绝理由
                application.setStatus("REJECTED");
                application.setRejectionReason("导师超时未处理");
                application.setDecisionDate(Instant.now());

                // 更新申请状态
                applicationRepository.save(application);
            }
        }
    }


    // 添加 getApplicationById 方法
    public Application getApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId).orElse(null);
    }

    // 学生提交申请
    public Application submitApplication(Long studentId, Long mentorId, String reason) {
        // 检查学生是否存在
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        // 检查用户身份是否为学生
        if (!"STUDENT".equals(student.getRole().getRoleName())) {
            throw new RuntimeException("只有学生可以提交导师申请");
        }

        // 检查是否存在待审批的申请
        List<Application> pendingApplications = applicationRepository.findByStudentIdAndStatus(studentId, "PENDING");
        if (!pendingApplications.isEmpty()) {
            throw new RuntimeException("您已经有一个等待审批的申请，请等待导师处理后再提交新的申请");
        }

        // 检查导师是否存在
        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("导师不存在"));

        // 检查申请对象是否为导师
        if (!"TEACHER".equals(mentor.getRole().getRoleName())) {
            throw new RuntimeException("只有导师才可以接受申请");
        }

        // 检查导师是否已经有 3 名学生
        if (getAcceptedStudentCount(mentor.getUid()) >= 9) {
            throw new RuntimeException("导师已经有9名学生，无法接受更多学生");
        }

        // 检查学生是否已经有导师
        if (student.getMentorId() != null) {
            throw new RuntimeException("您已经选择了导师，无法再次申请");
        }

        // 创建申请
        Application application = new Application();
        application.setStudentId(student.getUid());
        application.setMentorId(mentor.getUid());
        application.setStatus("PENDING");
        application.setApplicationReason(reason);
        application.setApplicationDate(new Date().toInstant());

        return applicationRepository.save(application);
    }

    // 导师审批申请
    public Application approveApplication(Long applicationId, boolean approved, String rejectionReason) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        User mentor = userRepository.findById(application.getMentorId())
                .orElseThrow(() -> new RuntimeException("导师不存在"));
        User student = userRepository.findById(application.getStudentId())
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        if (approved) {
            // 检查导师是否已经有 9 名学生
            if (getAcceptedStudentCount(mentor.getUid()) >= 9) {
                throw new RuntimeException("导师已经有9名学生");
            }

            // 检查学生是否已经有导师
            if (student.getMentorId() != null) {
                throw new RuntimeException("学生已经有导师，无法被多位导师选择");
            }

            // 接受申请，设置学生的 mentorId
            application.setStatus("ACCEPTED");
            student.setMentorId(mentor.getUid());

            // 更新导师已接受学生数量
            mentor.setAcceptedStudents(getAcceptedStudentCount(mentor.getUid()) + 1);

            // 保存更新后的学生和导师
            userRepository.save(student);
            userRepository.save(mentor);
        } else {
            // 拒绝申请
            application.setStatus("REJECTED");
            application.setRejectionReason(rejectionReason);
        }

        application.setDecisionDate(new Date().toInstant());
        return applicationRepository.save(application);
    }

    // 管理员获取所有申请
    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    // 导师获取与自己相关的所有申请（不管状态）
    public List<Application> getApplicationsByMentorId(Long mentorId) {
        return applicationRepository.findByMentorId(mentorId);
    }

    // 学生获取自己提交的所有申请（不管状态）
    public List<Application> getApplicationsByStudentId(Long studentId) {
        return applicationRepository.findByStudentId(studentId);
    }

    // 获取导师已接受的学生数量
    private int getAcceptedStudentCount(Long mentorId) {
        List<User> students = userRepository.findByMentorId(mentorId);
        return students.size();
    }

    // 获取与导师相关的所有申请
    public boolean hasPendingApplication(Long studentId, Long mentorId) {
        List<Application> pendingApplications = applicationRepository.findByStudentIdAndStatus(studentId, "PENDING");
        return !pendingApplications.isEmpty() || applicationRepository.findByMentorIdAndStatus(mentorId, "PENDING").stream()
                .anyMatch(app -> app.getStudentId().equals(studentId));
    }


}
