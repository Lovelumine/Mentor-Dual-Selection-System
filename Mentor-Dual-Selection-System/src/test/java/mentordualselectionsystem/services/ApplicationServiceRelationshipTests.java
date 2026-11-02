package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.RelationshipResetBatch;
import mentordualselectionsystem.mysql.RelationshipResetBatchSnapshot;
import mentordualselectionsystem.mysql.Role;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.RelationshipResetBatchRepository;
import mentordualselectionsystem.repositories.RelationshipResetBatchSnapshotRepository;
import mentordualselectionsystem.repositories.RoleRepository;
import mentordualselectionsystem.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationServiceRelationshipTests {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RelationshipResetBatchRepository relationshipResetBatchRepository;

    @Autowired
    private RelationshipResetBatchSnapshotRepository relationshipResetBatchSnapshotRepository;

    @Test
    void partialUnbindShouldCreateRestorableBatchAndRestoreOnlyAffectedStudents() {
        String suffix = String.valueOf(System.nanoTime());
        Role teacherRole = getOrCreateRole("TEACHER");
        Role studentRole = getOrCreateRole("STUDENT");
        Role adminRole = getOrCreateRole("ADMIN");

        User mentor = createUser("t-" + suffix, "导师甲", teacherRole);
        User otherMentor = createUser("t2-" + suffix, "导师乙", teacherRole);
        User admin = createUser("admin-" + suffix, "管理员", adminRole);

        User student = createUser("s1-" + suffix, "学生甲", studentRole);
        student.setMentorId(mentor.getUid());
        student = userRepository.save(student);

        User untouched = createUser("s2-" + suffix, "学生乙", studentRole);
        untouched.setMentorId(otherMentor.getUid());
        untouched = userRepository.save(untouched);

        RelationshipResetBatch batch = applicationService.removeMentorStudentRelationships(
                List.of(student.getUid()),
                admin,
                "单元测试解除关系"
        );

        assertThat(batch).isNotNull();
        assertThat(batch.getAffectedStudentCount()).isEqualTo(1);
        assertThat(batch.getAffectedMentorCount()).isEqualTo(1);
        assertThat(batch.getReason()).isEqualTo("单元测试解除关系");

        assertThat(userRepository.findById(student.getUid()).orElseThrow().getMentorId()).isNull();
        assertThat(userRepository.findById(untouched.getUid()).orElseThrow().getMentorId())
                .isEqualTo(otherMentor.getUid());

        List<RelationshipResetBatchSnapshot> snapshots =
                relationshipResetBatchSnapshotRepository.findByBatchIdOrderByIdAsc(batch.getId());
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getStudentId()).isEqualTo(student.getUid());
        assertThat(snapshots.get(0).getMentorId()).isEqualTo(mentor.getUid());

        applicationService.restoreRelationshipsFromBatch(batch.getId(), admin);

        assertThat(userRepository.findById(student.getUid()).orElseThrow().getMentorId())
                .isEqualTo(mentor.getUid());
        assertThat(userRepository.findById(untouched.getUid()).orElseThrow().getMentorId())
                .isEqualTo(otherMentor.getUid());

        RelationshipResetBatch restoredBatch = relationshipResetBatchRepository.findById(batch.getId()).orElseThrow();
        assertThat(restoredBatch.getRestoreCount()).isEqualTo(1);
        assertThat(restoredBatch.getLastRestoredAt()).isNotNull();
    }

    private Role getOrCreateRole(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName(roleName);
                    return roleRepository.save(role);
                });
    }

    private User createUser(String username, String fullName, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(username + "@example.com");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setActive(true);
        user.setAcceptedStudents(0);
        return userRepository.save(user);
    }
}
