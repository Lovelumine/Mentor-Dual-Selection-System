package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.AdminRelationshipActionLog;
import mentordualselectionsystem.mysql.Application;
import mentordualselectionsystem.mysql.RelationshipResetBatch;
import mentordualselectionsystem.mysql.RelationshipResetBatchSnapshot;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.AdminRelationshipActionLogRepository;
import mentordualselectionsystem.repositories.ApplicationRepository;
import mentordualselectionsystem.repositories.RelationshipResetBatchRepository;
import mentordualselectionsystem.repositories.RelationshipResetBatchSnapshotRepository;
import mentordualselectionsystem.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String BATCH_STATUS_ACTIVE = "ACTIVE";

    private static final String ACTION_ADMIN_UNBIND = "ADMIN_UNBIND";
    private static final String ACTION_ADMIN_UNBIND_ALL = "ADMIN_UNBIND_ALL";
    private static final String ACTION_ADMIN_RESTORE_OVERWRITE_UNBIND = "ADMIN_RESTORE_OVERWRITE_UNBIND";
    private static final String ACTION_ADMIN_RESTORE_BIND = "ADMIN_RESTORE_BIND";
    private static final String ACTION_ADMIN_STUDENT_ARCHIVE_UNBIND = "ADMIN_STUDENT_ARCHIVE_UNBIND";

    private static final String DEFAULT_SINGLE_UNBIND_REASON = "管理员主动解除导师与学生关系";
    private static final String DEFAULT_UNBIND_ALL_REASON = "统一重置导师关系";

    private final AdminRelationshipActionLogRepository adminRelationshipActionLogRepository;
    private final ApplicationRepository applicationRepository;
    private final RelationshipResetBatchRepository relationshipResetBatchRepository;
    private final RelationshipResetBatchSnapshotRepository relationshipResetBatchSnapshotRepository;
    private final UserRepository userRepository;

    @Autowired
    public ApplicationService(
            AdminRelationshipActionLogRepository adminRelationshipActionLogRepository,
            ApplicationRepository applicationRepository,
            RelationshipResetBatchRepository relationshipResetBatchRepository,
            RelationshipResetBatchSnapshotRepository relationshipResetBatchSnapshotRepository,
            UserRepository userRepository
    ) {
        this.adminRelationshipActionLogRepository = adminRelationshipActionLogRepository;
        this.applicationRepository = applicationRepository;
        this.relationshipResetBatchRepository = relationshipResetBatchRepository;
        this.relationshipResetBatchSnapshotRepository = relationshipResetBatchSnapshotRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void autoRejectExpiredApplications() {
        List<Application> pendingApplications = applicationRepository.findByStatus(STATUS_PENDING);

        for (Application application : pendingApplications) {
            if (ChronoUnit.DAYS.between(application.getApplicationDate(), Instant.now()) > 7) {
                application.setStatus(STATUS_REJECTED);
                application.setRejectionReason("导师超时未处理");
                application.setDecisionDate(Instant.now());
                applicationRepository.save(application);
            }
        }
    }

    public Application getApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId).orElse(null);
    }

    public Application submitApplication(Long studentId, Long mentorId, String reason) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        if (!"STUDENT".equals(student.getRole().getRoleName())) {
            throw new RuntimeException("只有学生可以提交导师申请");
        }

        List<Application> pendingApplications = applicationRepository.findByStudentIdAndStatus(studentId, STATUS_PENDING);
        if (!pendingApplications.isEmpty()) {
            throw new RuntimeException("您已经有一个等待审批的申请，请等待导师处理后再提交新的申请");
        }

        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("导师不存在"));

        if (!"TEACHER".equals(mentor.getRole().getRoleName())) {
            throw new RuntimeException("只有导师才可以接受申请");
        }

        if (getAcceptedStudentCount(mentor.getUid()) >= 9) {
            throw new RuntimeException("导师已经有9名学生，无法接受更多学生");
        }

        if (student.getMentorId() != null) {
            throw new RuntimeException("您已经选择了导师，无法再次申请");
        }

        Application application = new Application();
        application.setStudentId(student.getUid());
        application.setMentorId(mentor.getUid());
        application.setStatus(STATUS_PENDING);
        application.setApplicationReason(reason);
        application.setApplicationDate(new Date().toInstant());

        return applicationRepository.save(application);
    }

    @Transactional
    public Application approveApplication(Long applicationId, boolean approved, String rejectionReason) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        User mentor = userRepository.findById(application.getMentorId())
                .orElseThrow(() -> new RuntimeException("导师不存在"));
        User student = userRepository.findById(application.getStudentId())
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        if (approved) {
            if (getAcceptedStudentCount(mentor.getUid()) >= 9) {
                throw new RuntimeException("导师已经有9名学生");
            }

            if (student.getMentorId() != null) {
                throw new RuntimeException("学生已经有导师，无法被多位导师选择");
            }

            application.setStatus(STATUS_ACCEPTED);
            student.setMentorId(mentor.getUid());
            mentor.setAcceptedStudents(getAcceptedStudentCount(mentor.getUid()) + 1);

            userRepository.save(student);
            userRepository.save(mentor);
        } else {
            application.setStatus(STATUS_REJECTED);
            application.setRejectionReason(rejectionReason);
        }

        application.setDecisionDate(new Date().toInstant());
        return applicationRepository.save(application);
    }

    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    public List<Application> getApplicationsByMentorId(Long mentorId) {
        return applicationRepository.findByMentorId(mentorId);
    }

    public List<Application> getApplicationsByStudentId(Long studentId) {
        return applicationRepository.findByStudentId(studentId);
    }

    public List<Application> getAcceptedApplicationsByStudentId(Long studentId) {
        return applicationRepository.findByStudentIdAndStatusOrderByDecisionDateAsc(studentId, STATUS_ACCEPTED);
    }

    public List<Application> getAcceptedApplicationsByMentorId(Long mentorId) {
        return applicationRepository.findByMentorIdAndStatusOrderByDecisionDateAsc(mentorId, STATUS_ACCEPTED);
    }

    public List<Application> getPendingApplications() {
        return applicationRepository.findByStatus(STATUS_PENDING);
    }

    @Transactional
    public RelationshipResetBatch removeMentorStudentRelationships(List<Long> studentIds, User adminOperator, String reason) {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new RuntimeException("请至少选择一名学生");
        }

        Set<Long> uniqueStudentIds = new LinkedHashSet<>(studentIds);
        List<User> students = userRepository.findAllById(uniqueStudentIds);
        if (students.size() != uniqueStudentIds.size()) {
            throw new RuntimeException("存在无效的学生记录");
        }

        List<User> affectedStudents = students.stream()
                .filter(student -> student.getMentorId() != null)
                .toList();
        if (affectedStudents.isEmpty()) {
            return null;
        }

        Instant actionTime = Instant.now();
        String normalizedReason = normalizeReason(reason, DEFAULT_SINGLE_UNBIND_REASON);
        Set<Long> affectedMentorIds = affectedStudents.stream()
                .map(User::getMentorId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        RelationshipResetBatch batch = new RelationshipResetBatch();
        batch.setReason(normalizedReason);
        batch.setStatus(BATCH_STATUS_ACTIVE);
        batch.setCreatedById(adminOperator.getUid());
        batch.setCreatedByNameSnapshot(adminOperator.getFullName());
        batch.setCreatedAt(actionTime);
        batch.setAffectedStudentCount(affectedStudents.size());
        batch.setAffectedMentorCount(affectedMentorIds.size());
        batch.setRestoreCount(0);
        batch = relationshipResetBatchRepository.save(batch);

        final Long batchId = batch.getId();
        List<RelationshipResetBatchSnapshot> snapshots = affectedStudents.stream()
                .map(student -> {
                    RelationshipResetBatchSnapshot snapshot = new RelationshipResetBatchSnapshot();
                    snapshot.setBatchId(batchId);
                    snapshot.setStudentId(student.getUid());
                    snapshot.setMentorId(student.getMentorId());
                    snapshot.setStudentNameSnapshot(student.getFullName());
                    User mentor = userRepository.findById(student.getMentorId())
                            .orElseThrow(() -> new RuntimeException("存在无效的导师记录"));
                    snapshot.setMentorNameSnapshot(mentor.getFullName());
                    return snapshot;
                })
                .toList();
        relationshipResetBatchSnapshotRepository.saveAll(snapshots);

        clearStudentMentorAssignments(
                students,
                adminOperator,
                ACTION_ADMIN_UNBIND,
                normalizedReason,
                batchId,
                actionTime
        );

        return batch;
    }

    @Transactional
    public RelationshipResetBatch removeAllMentorStudentRelationships(User adminOperator, String reason) {
        List<User> students = userRepository.findByMentorIdIsNotNullAndActiveTrue();
        if (students.isEmpty()) {
            throw new RuntimeException("当前没有需要解除的导师关系");
        }

        Instant actionTime = Instant.now();
        String normalizedReason = normalizeReason(reason, DEFAULT_UNBIND_ALL_REASON);

        Set<Long> affectedMentorIds = students.stream()
                .map(User::getMentorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        RelationshipResetBatch batch = new RelationshipResetBatch();
        batch.setReason(normalizedReason);
        batch.setStatus(BATCH_STATUS_ACTIVE);
        batch.setCreatedById(adminOperator.getUid());
        batch.setCreatedByNameSnapshot(adminOperator.getFullName());
        batch.setCreatedAt(actionTime);
        batch.setAffectedStudentCount(students.size());
        batch.setAffectedMentorCount(affectedMentorIds.size());
        batch.setRestoreCount(0);
        batch = relationshipResetBatchRepository.save(batch);

        final Long batchId = batch.getId();
        List<RelationshipResetBatchSnapshot> snapshots = students.stream()
                .map(student -> {
                    RelationshipResetBatchSnapshot snapshot = new RelationshipResetBatchSnapshot();
                    snapshot.setBatchId(batchId);
                    snapshot.setStudentId(student.getUid());
                    snapshot.setMentorId(student.getMentorId());
                    snapshot.setStudentNameSnapshot(student.getFullName());
                    User mentor = userRepository.findById(student.getMentorId())
                            .orElseThrow(() -> new RuntimeException("存在无效的导师记录"));
                    snapshot.setMentorNameSnapshot(mentor.getFullName());
                    return snapshot;
                })
                .toList();
        relationshipResetBatchSnapshotRepository.saveAll(snapshots);

        clearStudentMentorAssignments(
                students,
                adminOperator,
                ACTION_ADMIN_UNBIND_ALL,
                normalizedReason,
                batch.getId(),
                actionTime
        );

        return batch;
    }

    @Transactional
    public RelationshipResetBatch restoreRelationshipsFromBatch(Long batchId, User adminOperator) {
        RelationshipResetBatch batch = relationshipResetBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("恢复批次不存在"));

        if (!BATCH_STATUS_ACTIVE.equals(batch.getStatus())) {
            throw new RuntimeException("该批次已失效，无法恢复");
        }

        List<RelationshipResetBatchSnapshot> snapshots = relationshipResetBatchSnapshotRepository.findByBatchIdOrderByIdAsc(batchId);
        if (snapshots.isEmpty()) {
            throw new RuntimeException("该批次没有可恢复的关系快照");
        }

        Instant restoreStartTime = Instant.now();
        Set<Long> snapshotStudentIds = snapshots.stream()
                .map(RelationshipResetBatchSnapshot::getStudentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> snapshotMentorIds = snapshots.stream()
                .map(RelationshipResetBatchSnapshot::getMentorId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 只回滚该批次涉及的学生，避免恢复局部批次时误清空其他学生当前的导师关系
        List<User> currentStudents = userRepository.findAllById(snapshotStudentIds).stream()
                .filter(User::isEnabled)
                .filter(student -> student.getMentorId() != null)
                .toList();
        if (!currentStudents.isEmpty()) {
            clearStudentMentorAssignments(
                    currentStudents,
                    adminOperator,
                    ACTION_ADMIN_RESTORE_OVERWRITE_UNBIND,
                    "恢复批次 #" + batchId + " 前状态，覆盖当前导师关系",
                    batchId,
                    restoreStartTime
            );
        }

        List<User> snapshotStudents = userRepository.findAllById(snapshotStudentIds).stream()
                .filter(User::isEnabled)
                .toList();
        List<User> snapshotMentors = userRepository.findAllById(snapshotMentorIds);

        if (snapshotMentors.size() != snapshotMentorIds.size()) {
            throw new RuntimeException("恢复批次中存在无效的导师记录");
        }

        Map<Long, User> studentMap = snapshotStudents.stream()
                .collect(Collectors.toMap(User::getUid, student -> student, (left, right) -> left, LinkedHashMap::new));
        Map<Long, User> mentorMap = snapshotMentors.stream()
                .collect(Collectors.toMap(User::getUid, mentor -> mentor, (left, right) -> left, LinkedHashMap::new));

        List<AdminRelationshipActionLog> actionLogs = new ArrayList<>();
        Instant restoreActionTime = Instant.now();
        String restoreReason = "恢复至批次 #" + batchId + " 解除前状态";

        for (RelationshipResetBatchSnapshot snapshot : snapshots) {
            User student = studentMap.get(snapshot.getStudentId());
            User mentor = mentorMap.get(snapshot.getMentorId());

            if (student == null) {
                continue;
            }

            if (mentor == null) {
                throw new RuntimeException("恢复批次中存在无效的导师记录");
            }

            if (!"STUDENT".equals(student.getRole().getRoleName())) {
                throw new RuntimeException("恢复批次中存在无效的学生角色");
            }
            if (!"TEACHER".equals(mentor.getRole().getRoleName())) {
                throw new RuntimeException("恢复批次中存在无效的导师角色");
            }

            student.setMentorId(mentor.getUid());
            actionLogs.add(buildActionLog(
                    student,
                    mentor,
                    adminOperator,
                    ACTION_ADMIN_RESTORE_BIND,
                    restoreReason,
                    batchId,
                    restoreActionTime
            ));
        }

        userRepository.saveAll(snapshotStudents);
        rebuildMentorAcceptedStudentCounts(snapshotMentorIds);
        adminRelationshipActionLogRepository.saveAll(actionLogs);

        batch.setRestoreCount(batch.getRestoreCount() == null ? 1 : batch.getRestoreCount() + 1);
        batch.setLastRestoredAt(restoreActionTime);
        batch.setLastRestoredById(adminOperator.getUid());
        batch.setLastRestoredByNameSnapshot(adminOperator.getFullName());
        return relationshipResetBatchRepository.save(batch);
    }

    public List<RelationshipResetBatch> getRelationshipResetBatches() {
        return relationshipResetBatchRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<RelationshipResetBatchSnapshot> getRelationshipResetBatchSnapshots(Long batchId) {
        return relationshipResetBatchSnapshotRepository.findByBatchIdOrderByIdAsc(batchId);
    }

    public List<AdminRelationshipActionLog> getAllAdminRelationshipActionLogs() {
        return adminRelationshipActionLogRepository.findAllByOrderByActionDateDesc();
    }

    public List<AdminRelationshipActionLog> getAdminRelationshipActionLogsByStudentId(Long studentId) {
        return adminRelationshipActionLogRepository.findByStudentIdOrderByActionDateAsc(studentId);
    }

    public List<AdminRelationshipActionLog> getAdminRelationshipActionLogsByMentorId(Long mentorId) {
        return adminRelationshipActionLogRepository.findByMentorIdOrderByActionDateAsc(mentorId);
    }

    public List<AdminRelationshipActionLog> getAllAdminRelationshipActionLogsAsc() {
        return adminRelationshipActionLogRepository.findAllByOrderByActionDateAsc();
    }

    private int getAcceptedStudentCount(Long mentorId) {
        return (int) userRepository.countByMentorIdAndActiveTrue(mentorId);
    }

    public boolean hasPendingApplication(Long studentId, Long mentorId) {
        List<Application> pendingApplications = applicationRepository.findByStudentIdAndStatus(studentId, STATUS_PENDING);
        return !pendingApplications.isEmpty() || applicationRepository.findByMentorIdAndStatus(mentorId, STATUS_PENDING).stream()
                .anyMatch(app -> app.getStudentId().equals(studentId));
    }

    private String normalizeReason(String reason, String defaultReason) {
        if (reason == null || reason.trim().isEmpty()) {
            return defaultReason;
        }
        return reason.trim();
    }

    private int clearStudentMentorAssignments(
            List<User> students,
            User adminOperator,
            String actionType,
            String actionReason,
            Long batchId,
            Instant actionTime
    ) {
        Set<Long> affectedMentorIds = new LinkedHashSet<>();
        Map<Long, Long> mentorIdByStudentId = new LinkedHashMap<>();

        for (User student : students) {
            if (!"STUDENT".equals(student.getRole().getRoleName())) {
                throw new RuntimeException("仅允许解除学生的导师关系");
            }

            if (student.getMentorId() == null) {
                continue;
            }

            affectedMentorIds.add(student.getMentorId());
            mentorIdByStudentId.put(student.getUid(), student.getMentorId());
            student.setMentorId(null);
        }

        if (mentorIdByStudentId.isEmpty()) {
            return 0;
        }

        List<User> mentors = userRepository.findAllById(affectedMentorIds);
        if (mentors.size() != affectedMentorIds.size()) {
            throw new RuntimeException("存在无效的导师记录");
        }

        Map<Long, User> mentorMap = mentors.stream()
                .collect(Collectors.toMap(User::getUid, mentor -> mentor));
        List<AdminRelationshipActionLog> actionLogs = new ArrayList<>();

        for (User student : students) {
            Long originalMentorId = mentorIdByStudentId.get(student.getUid());
            if (originalMentorId == null) {
                continue;
            }

            User mentor = mentorMap.get(originalMentorId);
            if (mentor == null) {
                throw new RuntimeException("存在无效的导师记录");
            }

            actionLogs.add(buildActionLog(
                    student,
                    mentor,
                    adminOperator,
                    actionType,
                    actionReason,
                    batchId,
                    actionTime
            ));
        }

        userRepository.saveAll(students);
        rebuildMentorAcceptedStudentCounts(affectedMentorIds);
        adminRelationshipActionLogRepository.saveAll(actionLogs);
        return mentorIdByStudentId.size();
    }

    private AdminRelationshipActionLog buildActionLog(
            User student,
            User mentor,
            User adminOperator,
            String actionType,
            String actionReason,
            Long batchId,
            Instant actionTime
    ) {
        AdminRelationshipActionLog actionLog = new AdminRelationshipActionLog();
        actionLog.setStudentId(student.getUid());
        actionLog.setMentorId(mentor.getUid());
        actionLog.setAdminId(adminOperator.getUid());
        actionLog.setStudentNameSnapshot(student.getFullName());
        actionLog.setMentorNameSnapshot(mentor.getFullName());
        actionLog.setAdminNameSnapshot(adminOperator.getFullName());
        actionLog.setActionType(actionType);
        actionLog.setActionReason(actionReason);
        actionLog.setBatchId(batchId);
        actionLog.setActionDate(actionTime);
        return actionLog;
    }

    private void rebuildMentorAcceptedStudentCounts(Set<Long> mentorIds) {
        if (mentorIds == null || mentorIds.isEmpty()) {
            return;
        }

        List<User> mentors = userRepository.findAllById(mentorIds);
        for (User mentor : mentors) {
            mentor.setAcceptedStudents((int) userRepository.countByMentorIdAndActiveTrue(mentor.getUid()));
        }
        userRepository.saveAll(mentors);
    }
}
