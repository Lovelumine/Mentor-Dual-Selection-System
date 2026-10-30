package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.AdminRelationshipActionLog;
import mentordualselectionsystem.mysql.Application;
import mentordualselectionsystem.mysql.Role;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.AdminRelationshipActionLogRepository;
import mentordualselectionsystem.repositories.ApplicationRepository;
import mentordualselectionsystem.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private AdminRelationshipActionLogRepository adminRelationshipActionLogRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void saveUserShouldEncodeRawPassword() {
        User user = new User();
        user.setPassword("123");

        userService.saveUser(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        String savedPassword = captor.getValue().getPassword();
        assertThat(savedPassword).isNotEqualTo("123");
        assertThat(new BCryptPasswordEncoder().matches("123", savedPassword)).isTrue();
    }

    @Test
    void saveUserShouldNotReencodeExistingBcryptPassword() {
        String encodedPassword = new BCryptPasswordEncoder().encode("123");
        User user = new User();
        user.setPassword(encodedPassword);

        userService.saveUser(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    void archiveStudentsByUidsShouldArchiveAllSelectedStudentsAndClosePendingApplications() {
        User admin = createUser(10L, "管理员", "ADMIN", true);
        User mentor = createUser(99L, "导师", "TEACHER", true);
        User studentWithMentor = createUser(1L, "学生甲", "STUDENT", true);
        studentWithMentor.setMentorId(mentor.getUid());
        User studentWithoutMentor = createUser(2L, "学生乙", "STUDENT", true);

        Application pendingApplication = new Application();
        pendingApplication.setStudentId(studentWithMentor.getUid());
        pendingApplication.setMentorId(mentor.getUid());
        pendingApplication.setStatus("PENDING");
        pendingApplication.setApplicationDate(Instant.now());

        when(userRepository.findAllById(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            if (ids.contains(studentWithMentor.getUid())) {
                return List.of(studentWithMentor, studentWithoutMentor);
            }
            if (ids.contains(mentor.getUid())) {
                return List.of(mentor);
            }
            return List.of();
        });
        when(applicationRepository.findByStudentIdAndStatus(studentWithMentor.getUid(), "PENDING"))
                .thenReturn(List.of(pendingApplication));
        when(applicationRepository.findByStudentIdAndStatus(studentWithoutMentor.getUid(), "PENDING"))
                .thenReturn(List.of());
        when(userRepository.countByMentorIdAndActiveTrue(mentor.getUid())).thenReturn(0L);

        Map<String, Object> result = userService.archiveStudentsByUids(
                List.of(studentWithMentor.getUid(), studentWithoutMentor.getUid()),
                admin,
                "转专业转出"
        );

        assertThat(studentWithMentor.isEnabled()).isFalse();
        assertThat(studentWithoutMentor.isEnabled()).isFalse();
        assertThat(studentWithMentor.getMentorId()).isNull();
        assertThat(studentWithMentor.getDeactivatedReason()).isEqualTo("转专业转出");
        assertThat(pendingApplication.getStatus()).isEqualTo("REJECTED");
        assertThat(result).containsEntry("archivedStudentCount", 2);
        assertThat(result).containsEntry("affectedMentorCount", 1);
        assertThat(result).containsEntry("rejectedPendingCount", 1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AdminRelationshipActionLog>> logCaptor = ArgumentCaptor.forClass(List.class);
        verify(adminRelationshipActionLogRepository).saveAll(logCaptor.capture());
        assertThat(logCaptor.getValue()).singleElement()
                .satisfies(log -> assertThat(log.getStudentId()).isEqualTo(studentWithMentor.getUid()));
        verify(userRepository, times(2)).saveAll(any());
    }

    @Test
    void archiveStudentsByUidsShouldRejectWholeBatchWhenAnyStudentIsInactive() {
        User admin = createUser(10L, "管理员", "ADMIN", true);
        User activeStudent = createUser(1L, "学生甲", "STUDENT", true);
        User inactiveStudent = createUser(2L, "学生乙", "STUDENT", false);

        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(activeStudent, inactiveStudent));

        assertThatThrownBy(() -> userService.archiveStudentsByUids(
                List.of(activeStudent.getUid(), inactiveStudent.getUid()),
                admin,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已停用");

        assertThat(activeStudent.isEnabled()).isTrue();
        assertThat(inactiveStudent.isEnabled()).isFalse();
        verify(userRepository, never()).saveAll(any());
        verifyNoInteractions(applicationRepository, adminRelationshipActionLogRepository);
    }

    private User createUser(Long uid, String fullName, String roleName, boolean active) {
        Role role = new Role();
        role.setRoleName(roleName);

        User user = new User();
        user.setUid(uid);
        user.setUsername("user-" + uid);
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
