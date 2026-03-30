package mentordualselectionsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mentordualselectionsystem.dto.AdminUnbindAllRequest;
import mentordualselectionsystem.dto.AdminUnbindRequest;
import mentordualselectionsystem.dto.ApplicationApprovalRequest;
import mentordualselectionsystem.dto.ApplicationSubmitRequest;
import mentordualselectionsystem.mysql.AdminRelationshipActionLog;
import mentordualselectionsystem.mysql.Application;
import mentordualselectionsystem.mysql.RelationshipResetBatch;
import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.mysql.UserDetail;
import mentordualselectionsystem.security.JwtUtils;
import mentordualselectionsystem.services.ApplicationService;
import mentordualselectionsystem.services.UserDetailService;
import mentordualselectionsystem.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Tag(name = "申请相关接口")
@RequestMapping("/api/application")
public class ApplicationController {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final String ACTION_ADMIN_UNBIND = "ADMIN_UNBIND";
    private static final String ACTION_ADMIN_UNBIND_ALL = "ADMIN_UNBIND_ALL";
    private static final String ACTION_ADMIN_RESTORE_OVERWRITE_UNBIND = "ADMIN_RESTORE_OVERWRITE_UNBIND";
    private static final String ACTION_ADMIN_RESTORE_BIND = "ADMIN_RESTORE_BIND";
    private static final String ACTION_ADMIN_STUDENT_ARCHIVE_UNBIND = "ADMIN_STUDENT_ARCHIVE_UNBIND";
    private static final String ACTION_ADMIN_TEACHER_ARCHIVE_UNBIND = "ADMIN_TEACHER_ARCHIVE_UNBIND";
    private static final String BATCH_STATUS_ACTIVE = "ACTIVE";

    private final ApplicationService applicationService;
    private final UserService userService;
    private final UserDetailService userDetailService;
    private final JwtUtils jwtUtils;

    @Autowired
    public ApplicationController(
            ApplicationService applicationService,
            UserService userService,
            UserDetailService userDetailService,
            JwtUtils jwtUtils
    ) {
        this.applicationService = applicationService;
        this.userService = userService;
        this.userDetailService = userDetailService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/submit")
    @Operation(
            summary = "提交学生申请",
            description = "学生通过 token 提交申请，提供申请理由以及导师 ID。该接口仅供学生用户使用，系统会通过 JWT Token 确认用户身份。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功提交申请"),
            @ApiResponse(responseCode = "400", description = "参数无效或缺失"),
            @ApiResponse(responseCode = "401", description = "token无效或已过期"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<Map<String, Object>> submitApplication(@RequestBody ApplicationSubmitRequest request) {
        try {
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            Long studentId = jwtUtils.validateTokenAndGetUid(jwtToken);

            Application application = applicationService.submitApplication(studentId, request.getMentorId(), request.getReason());
            return buildSuccessResponse(200, application);
        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @PostMapping("/approve")
    @Operation(
            summary = "导师或管理员审批学生申请",
            description = "导师或管理员通过或拒绝学生的申请。**注意：拒绝申请时，必须填写拒绝理由；同意申请时可选。**"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功审批申请"),
            @ApiResponse(responseCode = "400", description = "申请不存在或审批无效"),
            @ApiResponse(responseCode = "401", description = "token无效或已过期"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<Map<String, Object>> approveApplication(@RequestBody ApplicationApprovalRequest request) {
        try {
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");

            Long operatorId = jwtUtils.validateTokenAndGetUid(jwtToken);
            User operator = userService.getUserByUid(operatorId);
            String roleName = operator.getRole().getRoleName();

            Application application = applicationService.getApplicationById(request.getApplicationId());
            if (application == null) {
                return buildErrorResponse(400, "申请不存在");
            }

            if (!STATUS_PENDING.equalsIgnoreCase(application.getStatus())) {
                return buildErrorResponse(400, "申请已经终结，无法再次修改，需要学生重新提交申请");
            }

            if ("TEACHER".equals(roleName)) {
                if (!application.getMentorId().equals(operatorId)) {
                    return buildErrorResponse(403, "无权审批该申请");
                }
            } else if (!"ADMIN".equals(roleName)) {
                return buildErrorResponse(403, "当前用户无权审批该申请");
            }

            if (!request.isApproved() && (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
                return buildErrorResponse(400, "拒绝申请时必须填写拒绝理由");
            }

            application = applicationService.approveApplication(
                    request.getApplicationId(),
                    request.isApproved(),
                    request.getRejectionReason()
            );

            return buildSuccessResponse(200, application);
        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @Operation(
            summary = "获取所有已存在的导师与学生关系",
            description = "根据当前用户的角色，获取导师与学生之间的关系，学生获取自己的导师，导师获取自己的学生列表，管理员获取所有关系。"
    )
    @GetMapping("/mentor-student-relations")
    public ResponseEntity<Map<String, Object>> getMentorStudentRelationships() {
        try {
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);

            User currentUser = userService.getUserByUid(userId);
            String roleName = currentUser.getRole().getRoleName();

            List<Map<String, Object>> mentorStudentRelationships;

            if ("STUDENT".equals(roleName)) {
                if (currentUser.getMentorId() == null) {
                    return buildErrorResponse(404, "您还没有选择导师");
                }
                User mentor = userService.getUserByUid(currentUser.getMentorId());

                Map<String, Object> mentorMap = new HashMap<>();
                mentorMap.put("uid", mentor.getUid());
                mentorMap.put("fullName", mentor.getFullName());
                mentorMap.put("email", mentor.getEmail());

                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mentor", mentorMap);

                mentorStudentRelationships = Collections.singletonList(responseMap);
            } else if ("TEACHER".equals(roleName)) {
                List<User> students = userService.getStudentsByMentorId(currentUser.getUid());

                Map<String, Object> mentorMap = new HashMap<>();
                mentorMap.put("uid", currentUser.getUid());
                mentorMap.put("fullName", currentUser.getFullName());
                mentorMap.put("email", currentUser.getEmail());

                List<Map<String, Object>> studentList = students.stream().map(student -> {
                    Map<String, Object> studentMap = new HashMap<>();
                    studentMap.put("uid", student.getUid());
                    studentMap.put("fullName", student.getFullName());
                    studentMap.put("email", student.getEmail());
                    studentMap.put("username", student.getUsername());
                    return studentMap;
                }).toList();

                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("mentor", mentorMap);
                responseMap.put("students", studentList);

                mentorStudentRelationships = Collections.singletonList(responseMap);
            } else if ("ADMIN".equals(roleName)) {
                mentorStudentRelationships = userService.getAllMentorStudentRelationships();
            } else {
                return buildErrorResponse(403, "无权访问该信息");
            }

            return buildSuccessResponse(200, mentorStudentRelationships);
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @Operation(
            summary = "获取学生当前及历史导师关系",
            description = "学生查看当前导师、绑定时间以及历史导师关系记录。"
    )
    @GetMapping("/student/relationship-history")
    public ResponseEntity<Map<String, Object>> getStudentRelationshipHistory() {
        try {
            User currentUser = getCurrentUser();
            if (!"STUDENT".equals(currentUser.getRole().getRoleName())) {
                return buildErrorResponse(403, "当前用户无权访问导师历史关系");
            }

            List<Application> acceptedApplications = applicationService.getAcceptedApplicationsByStudentId(currentUser.getUid());
            List<AdminRelationshipActionLog> actionLogs = applicationService.getAdminRelationshipActionLogsByStudentId(currentUser.getUid());
            Set<Long> teacherIds = new LinkedHashSet<>();
            if (currentUser.getMentorId() != null) {
                teacherIds.add(currentUser.getMentorId());
            }
            acceptedApplications.forEach(application -> teacherIds.add(application.getMentorId()));
            actionLogs.forEach(log -> teacherIds.add(log.getMentorId()));

            Map<Long, User> teacherMap = userService.getUsersByUids(teacherIds).stream()
                    .filter(teacher -> "TEACHER".equals(teacher.getRole().getRoleName()))
                    .collect(Collectors.toMap(User::getUid, teacher -> teacher));
            Map<Long, UserDetail> teacherDetailMap = userDetailService.getUserDetailMapByUids(
                    teacherIds
            );

            List<Map<String, Object>> relationshipHistory = buildStudentRelationshipHistoryRecords(
                    currentUser,
                    acceptedApplications,
                    actionLogs,
                    teacherMap,
                    teacherDetailMap
            );

            Map<String, Object> currentRelationship = relationshipHistory.stream()
                    .filter(record -> Boolean.TRUE.equals(record.get("current")))
                    .findFirst()
                    .orElse(null);

            List<Map<String, Object>> historicalRelationships = relationshipHistory.stream()
                    .filter(record -> !Boolean.TRUE.equals(record.get("current")))
                    .toList();

            Map<String, Object> data = new HashMap<>();
            data.put("currentRelationship", currentRelationship);
            data.put("historicalRelationships", historicalRelationships);
            return buildSuccessResponse(200, data);
        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @Operation(
            summary = "获取导师当前及历史学生关系",
            description = "导师查看当前指导学生及历史学生关系记录。"
    )
    @GetMapping("/teacher/student-relationship-history")
    public ResponseEntity<Map<String, Object>> getTeacherStudentRelationshipHistory() {
        try {
            User currentUser = getCurrentUser();
            if (!"TEACHER".equals(currentUser.getRole().getRoleName())) {
                return buildErrorResponse(403, "当前用户无权访问学生历史关系");
            }

            List<User> currentStudents = userService.getStudentsByMentorId(currentUser.getUid());
            List<Application> acceptedApplications = applicationService.getAcceptedApplicationsByMentorId(currentUser.getUid());
            List<AdminRelationshipActionLog> actionLogs = applicationService.getAdminRelationshipActionLogsByMentorId(currentUser.getUid());

            Set<Long> studentIds = new LinkedHashSet<>();
            currentStudents.forEach(student -> studentIds.add(student.getUid()));
            acceptedApplications.forEach(application -> studentIds.add(application.getStudentId()));
            actionLogs.forEach(log -> studentIds.add(log.getStudentId()));

            Map<Long, User> studentMap = userService.getUsersByUids(studentIds).stream()
                    .filter(student -> "STUDENT".equals(student.getRole().getRoleName()))
                    .filter(student -> studentIds.contains(student.getUid()))
                    .collect(Collectors.toMap(User::getUid, student -> student));
            Map<Long, UserDetail> studentDetailMap = userDetailService.getUserDetailMapByUids(studentIds);

            List<Map<String, Object>> relationshipHistory = buildTeacherStudentRelationshipHistoryRecords(
                    currentUser,
                    currentStudents,
                    acceptedApplications,
                    actionLogs,
                    studentMap,
                    studentDetailMap
            );

            List<Map<String, Object>> currentStudentRecords = relationshipHistory.stream()
                    .filter(record -> Boolean.TRUE.equals(record.get("current")))
                    .toList();
            List<Map<String, Object>> historicalStudentRecords = relationshipHistory.stream()
                    .filter(record -> !Boolean.TRUE.equals(record.get("current")))
                    .toList();

            Map<String, Object> data = new HashMap<>();
            data.put("currentStudents", currentStudentRecords);
            data.put("historicalStudents", historicalStudentRecords);
            return buildSuccessResponse(200, data);
        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @Operation(
            summary = "获取申请信息",
            description = "根据当前用户的角色获取相关的申请信息。管理员可以获取所有申请信息，导师可以获取与自己相关的申请，学生可以获取自己提交的申请。"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取申请列表"),
            @ApiResponse(responseCode = "401", description = "token无效或已过期"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingApplications() {
        try {
            User currentUser = getCurrentUser();
            List<Application> applications;

            String roleName = currentUser.getRole().getRoleName();
            if ("ADMIN".equals(roleName)) {
                applications = applicationService.getAllApplications();
            } else if ("TEACHER".equals(roleName)) {
                applications = applicationService.getApplicationsByMentorId(currentUser.getUid());
            } else if ("STUDENT".equals(roleName)) {
                applications = applicationService.getApplicationsByStudentId(currentUser.getUid());
            } else {
                return buildErrorResponse(403, "无权访问申请信息");
            }

            Map<Long, User> studentMap = userService.getAllStudents().stream()
                    .collect(Collectors.toMap(User::getUid, student -> student));
            Map<Long, User> teacherMap = userService.getAllTeachers().stream()
                    .collect(Collectors.toMap(User::getUid, teacher -> teacher));

            List<Map<String, Object>> records = applications.stream()
                    .map(application -> buildApplicationRecord(application, studentMap, teacherMap))
                    .collect(Collectors.toCollection(ArrayList::new));

            if ("ADMIN".equals(roleName)) {
                records.addAll(applicationService.getAllAdminRelationshipActionLogs().stream()
                        .map(this::buildAdminActionRecord)
                        .toList());
            }

            records.sort((left, right) -> {
                Instant leftSortTime = resolveRecordSortTime(left);
                Instant rightSortTime = resolveRecordSortTime(right);
                if (leftSortTime == null && rightSortTime == null) {
                    return 0;
                }
                if (leftSortTime == null) {
                    return 1;
                }
                if (rightSortTime == null) {
                    return -1;
                }
                return rightSortTime.compareTo(leftSortTime);
            });

            return buildSuccessResponse(200, records);
        } catch (NumberFormatException e) {
            return buildErrorResponse(401, "token无效或已过期");
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @Operation(
            summary = "管理员获取导师分组关系",
            description = "按导师分组返回师生关系、待处理申请数和未分配学生，供管理员管理使用。"
    )
    @GetMapping("/admin/mentor-groups")
    public ResponseEntity<Map<String, Object>> getAdminMentorGroups() {
        try {
            User currentUser = getCurrentUser();
            if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
                return buildErrorResponse(403, "无权访问该信息");
            }

            List<User> mentors = userService.getAllTeachers();
            List<User> students = userService.getAllStudents();

            Map<Long, UserDetail> mentorDetailMap = userDetailService.getUserDetailMapByUids(
                    mentors.stream().map(User::getUid).toList()
            );
            Map<Long, UserDetail> studentDetailMap = userDetailService.getUserDetailMapByUids(
                    students.stream().map(User::getUid).toList()
            );

            Map<Long, List<User>> studentsByMentorId = students.stream()
                    .filter(student -> student.getMentorId() != null)
                    .collect(Collectors.groupingBy(User::getMentorId));

            List<Map<String, Object>> unassignedStudents = students.stream()
                    .filter(student -> student.getMentorId() == null)
                    .map(student -> buildStudentSummary(student, studentDetailMap.get(student.getUid())))
                    .toList();

            Map<Long, Long> pendingCountsByMentorId = applicationService.getPendingApplications().stream()
                    .collect(Collectors.groupingBy(Application::getMentorId, Collectors.counting()));

            List<Map<String, Object>> groups = mentors.stream()
                    .map(mentor -> {
                        List<User> mentorStudents = studentsByMentorId.getOrDefault(mentor.getUid(), Collections.emptyList());
                        UserDetail mentorDetail = mentorDetailMap.get(mentor.getUid());
                        int studentCount = mentorStudents.size();
                        long pendingApplicationCount = pendingCountsByMentorId.getOrDefault(mentor.getUid(), 0L);

                        Map<String, Object> mentorMap = new HashMap<>();
                        mentorMap.put("uid", mentor.getUid());
                        mentorMap.put("fullName", mentor.getFullName());
                        mentorMap.put("email", mentor.getEmail());
                        mentorMap.put("avatarUrl", mentor.getAvatarUrl());
                        mentorMap.put("photoUrl", mentorDetail != null ? mentorDetail.getPhotoUrl() : null);
                        mentorMap.put("acceptedStudents", mentor.getAcceptedStudents());
                        mentorMap.put("studentCount", studentCount);
                        mentorMap.put("pendingApplicationCount", pendingApplicationCount);
                        mentorMap.put("capacity", 9);
                        mentorMap.put("status", studentCount >= 9 ? "FULL" : (studentCount == 0 ? "EMPTY" : "ACTIVE"));

                        Map<String, Object> group = new HashMap<>();
                        group.put("mentor", mentorMap);
                        group.put("students", mentorStudents.stream()
                                .map(student -> buildStudentSummary(student, studentDetailMap.get(student.getUid())))
                                .toList());
                        return group;
                    })
                    .sorted((left, right) -> {
                        Map<String, Object> leftMentor = castMap(left.get("mentor"));
                        Map<String, Object> rightMentor = castMap(right.get("mentor"));
                        int leftCount = (int) leftMentor.get("studentCount");
                        int rightCount = (int) rightMentor.get("studentCount");
                        if (leftCount != rightCount) {
                            return Integer.compare(rightCount, leftCount);
                        }
                        return String.valueOf(leftMentor.get("fullName")).compareTo(String.valueOf(rightMentor.get("fullName")));
                    })
                    .toList();

            long fullMentorCount = groups.stream()
                    .map(group -> castMap(group.get("mentor")))
                    .filter(mentor -> "FULL".equals(mentor.get("status")))
                    .count();

            Map<String, Object> summary = new HashMap<>();
            summary.put("mentorCount", mentors.size());
            summary.put("assignedStudentCount", students.size() - unassignedStudents.size());
            summary.put("unassignedStudentCount", unassignedStudents.size());
            summary.put("fullMentorCount", fullMentorCount);

            Map<String, Object> data = new HashMap<>();
            data.put("summary", summary);
            data.put("groups", groups);
            data.put("unassignedStudents", unassignedStudents);

            return buildSuccessResponse(200, data);
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @Operation(
            summary = "管理员解除导师与学生关系",
            description = "管理员可对一个或多个学生解除当前导师关系。"
    )
    @PostMapping("/admin/unbind")
    public ResponseEntity<Map<String, Object>> adminUnbindRelationships(@RequestBody AdminUnbindRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
                return buildErrorResponse(403, "无权执行该操作");
            }

            int removedCount = applicationService.removeMentorStudentRelationships(request.getStudentIds(), currentUser);
            if (removedCount == 0) {
                return buildErrorResponse(400, "所选学生当前没有导师关系");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("removedCount", removedCount);
            return buildSuccessResponse(200, result);
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @Operation(
            summary = "管理员解除全部导师关系",
            description = "管理员可因新学期或新周期统一重置当前所有导师关系，并生成可恢复的批次快照。"
    )
    @PostMapping("/admin/unbind-all")
    public ResponseEntity<Map<String, Object>> adminUnbindAllRelationships(@RequestBody(required = false) AdminUnbindAllRequest request) {
        try {
            User currentUser = getCurrentUser();
            if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
                return buildErrorResponse(403, "无权执行该操作");
            }

            RelationshipResetBatch batch = applicationService.removeAllMentorStudentRelationships(
                    currentUser,
                    request != null ? request.getReason() : null
            );

            Map<String, Object> result = new HashMap<>();
            result.put("batchId", batch.getId());
            result.put("reason", batch.getReason());
            result.put("removedCount", batch.getAffectedStudentCount());
            result.put("affectedMentorCount", batch.getAffectedMentorCount());
            result.put("createdAt", batch.getCreatedAt());
            return buildSuccessResponse(200, result);
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @Operation(
            summary = "管理员获取批量重置记录",
            description = "返回所有可追溯的导师关系重置批次，支持后续按批次恢复。"
    )
    @GetMapping("/admin/reset-batches")
    public ResponseEntity<Map<String, Object>> getRelationshipResetBatches() {
        try {
            User currentUser = getCurrentUser();
            if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
                return buildErrorResponse(403, "无权访问该信息");
            }

            List<Map<String, Object>> batchRecords = applicationService.getRelationshipResetBatches().stream()
                    .map(this::buildResetBatchRecord)
                    .toList();
            return buildSuccessResponse(200, batchRecords);
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    @Operation(
            summary = "管理员恢复某批次前的导师关系",
            description = "按指定批次恢复当次重置前的师生关系快照，恢复后当前关系会被整体覆盖。"
    )
    @PostMapping("/admin/reset-batches/{batchId}/restore")
    public ResponseEntity<Map<String, Object>> restoreRelationshipBatch(@PathVariable Long batchId) {
        try {
            User currentUser = getCurrentUser();
            if (!"ADMIN".equals(currentUser.getRole().getRoleName())) {
                return buildErrorResponse(403, "无权执行该操作");
            }

            RelationshipResetBatch batch = applicationService.restoreRelationshipsFromBatch(batchId, currentUser);

            Map<String, Object> result = new HashMap<>();
            result.put("batchId", batch.getId());
            result.put("restoreCount", batch.getRestoreCount());
            result.put("lastRestoredAt", batch.getLastRestoredAt());
            result.put("lastRestoredByName", batch.getLastRestoredByNameSnapshot());
            return buildSuccessResponse(200, result);
        } catch (Exception e) {
            return buildErrorResponse(500, "服务器内部错误");
        }
    }

    private User getCurrentUser() {
        String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtils.validateTokenAndGetUid(jwtToken);
        return userService.getUserByUid(userId);
    }

    private Map<String, Object> buildStudentSummary(User student, UserDetail userDetail) {
        Map<String, Object> studentMap = new HashMap<>();
        studentMap.put("uid", student.getUid());
        studentMap.put("fullName", student.getFullName());
        studentMap.put("username", student.getUsername());
        studentMap.put("email", student.getEmail());
        studentMap.put("avatarUrl", student.getAvatarUrl());
        studentMap.put("mentorId", student.getMentorId());
        studentMap.put("grade", userDetail != null ? userDetail.getStudentGrade() : null);
        studentMap.put("class", userDetail != null ? userDetail.getStudentClass() : null);
        return studentMap;
    }

    private Map<String, Object> buildApplicationRecord(Application application, Map<Long, User> studentMap, Map<Long, User> teacherMap) {
        Map<String, Object> record = new HashMap<>();
        User student = studentMap.get(application.getStudentId());
        User teacher = teacherMap.get(application.getMentorId());

        record.put("id", application.getId());
        record.put("recordType", "APPLICATION");
        record.put("studentId", application.getStudentId());
        record.put("mentorId", application.getMentorId());
        record.put("studentName", student != null ? student.getFullName() : null);
        record.put("teacherName", teacher != null ? teacher.getFullName() : null);
        record.put("applicationReason", application.getApplicationReason());
        record.put("rejectionReason", application.getRejectionReason());
        record.put("status", application.getStatus());
        record.put("statusCN", translateStatus(application.getStatus()));
        record.put("applicationDate", application.getApplicationDate());
        record.put("decisionDate", application.getDecisionDate());
        record.put("processingStatusLabel", STATUS_PENDING.equals(application.getStatus()) ? "待处理" : "已完成");
        return record;
    }

    private Map<String, Object> buildAdminActionRecord(AdminRelationshipActionLog actionLog) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", "ACTION-" + actionLog.getId());
        record.put("recordType", actionLog.getActionType());
        record.put("studentId", actionLog.getStudentId());
        record.put("mentorId", actionLog.getMentorId());
        record.put("studentName", actionLog.getStudentNameSnapshot());
        record.put("teacherName", actionLog.getMentorNameSnapshot());
        record.put("applicationReason", actionLog.getActionReason());
        record.put("rejectionReason", null);
        record.put("applicationDate", null);
        record.put("decisionDate", actionLog.getActionDate());
        record.put("operatorName", actionLog.getAdminNameSnapshot());
        record.put("batchId", actionLog.getBatchId());

        if (ACTION_ADMIN_RESTORE_BIND.equals(actionLog.getActionType())) {
            record.put("status", "ADMIN_RESTORED");
            record.put("statusCN", "已恢复");
            record.put("processingStatusLabel", "批次恢复");
        } else {
            record.put("status", "ADMIN_UNBOUND");
            if (ACTION_ADMIN_STUDENT_ARCHIVE_UNBIND.equals(actionLog.getActionType())) {
                record.put("statusCN", "已转出");
            } else if (ACTION_ADMIN_TEACHER_ARCHIVE_UNBIND.equals(actionLog.getActionType())) {
                record.put("statusCN", "导师停用");
            } else {
                record.put("statusCN", "已解除");
            }
            if (ACTION_ADMIN_UNBIND_ALL.equals(actionLog.getActionType())) {
                record.put("processingStatusLabel", "统一重置");
            } else if (ACTION_ADMIN_RESTORE_OVERWRITE_UNBIND.equals(actionLog.getActionType())) {
                record.put("processingStatusLabel", "恢复覆盖");
            } else if (ACTION_ADMIN_STUDENT_ARCHIVE_UNBIND.equals(actionLog.getActionType())) {
                record.put("processingStatusLabel", "学生转出");
            } else if (ACTION_ADMIN_TEACHER_ARCHIVE_UNBIND.equals(actionLog.getActionType())) {
                record.put("processingStatusLabel", "导师停用");
            } else {
                record.put("processingStatusLabel", "管理员解除");
            }
        }
        return record;
    }

    private Map<String, Object> buildResetBatchRecord(RelationshipResetBatch batch) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", batch.getId());
        record.put("reason", batch.getReason());
        record.put("status", batch.getStatus());
        record.put("statusCN", BATCH_STATUS_ACTIVE.equals(batch.getStatus()) ? "可恢复" : "已失效");
        record.put("createdAt", batch.getCreatedAt());
        record.put("createdByName", batch.getCreatedByNameSnapshot());
        record.put("affectedStudentCount", batch.getAffectedStudentCount());
        record.put("affectedMentorCount", batch.getAffectedMentorCount());
        record.put("restoreCount", batch.getRestoreCount());
        record.put("lastRestoredAt", batch.getLastRestoredAt());
        record.put("lastRestoredByName", batch.getLastRestoredByNameSnapshot());
        record.put("restorable", BATCH_STATUS_ACTIVE.equals(batch.getStatus()));
        return record;
    }

    private List<Map<String, Object>> buildStudentRelationshipHistoryRecords(
            User student,
            List<Application> acceptedApplications,
            List<AdminRelationshipActionLog> actionLogs,
            Map<Long, User> teacherMap,
            Map<Long, UserDetail> teacherDetailMap
    ) {
        List<RelationshipTimelineEvent> events = new ArrayList<>();

        acceptedApplications.forEach(application -> events.add(new RelationshipTimelineEvent(
                application.getMentorId(),
                eventTime(application.getDecisionDate(), application.getApplicationDate()),
                true,
                null,
                null
        )));

        actionLogs.forEach(log -> {
            if (isRelationshipStartAction(log.getActionType())) {
                events.add(new RelationshipTimelineEvent(
                        log.getMentorId(),
                        log.getActionDate(),
                        true,
                        log.getActionReason(),
                        log.getAdminNameSnapshot()
                ));
            } else if (isRelationshipEndAction(log.getActionType())) {
                events.add(new RelationshipTimelineEvent(
                        log.getMentorId(),
                        log.getActionDate(),
                        false,
                        log.getActionReason(),
                        log.getAdminNameSnapshot()
                ));
            }
        });

        List<Map<String, Object>> records = buildTimelineRecords(
                events,
                counterpartId -> buildStudentRelationshipHistoryItem(counterpartId, teacherMap, teacherDetailMap, null, null, null, null, false),
                (record, event) -> {
                    if (event.start()) {
                        record.put("startDate", event.occurredAt());
                    } else {
                        record.put("endDate", event.occurredAt());
                        record.put("endReason", event.reason());
                        record.put("operatorName", event.operatorName());
                    }
                }
        );

        markCurrentStudentRelationship(student, records, teacherMap, teacherDetailMap);
        sortRelationshipRecords(records);
        return records;
    }

    private List<Map<String, Object>> buildTeacherStudentRelationshipHistoryRecords(
            User teacher,
            List<User> currentStudents,
            List<Application> acceptedApplications,
            List<AdminRelationshipActionLog> actionLogs,
            Map<Long, User> studentMap,
            Map<Long, UserDetail> studentDetailMap
    ) {
        Map<Long, List<RelationshipTimelineEvent>> eventsByStudentId = new LinkedHashMap<>();

        acceptedApplications.forEach(application -> eventsByStudentId
                .computeIfAbsent(application.getStudentId(), ignored -> new ArrayList<>())
                .add(new RelationshipTimelineEvent(
                        application.getStudentId(),
                        eventTime(application.getDecisionDate(), application.getApplicationDate()),
                        true,
                        null,
                        null
                )));

        actionLogs.forEach(log -> {
            if (isRelationshipStartAction(log.getActionType())) {
                eventsByStudentId
                        .computeIfAbsent(log.getStudentId(), ignored -> new ArrayList<>())
                        .add(new RelationshipTimelineEvent(
                                log.getStudentId(),
                                log.getActionDate(),
                                true,
                                log.getActionReason(),
                                log.getAdminNameSnapshot()
                        ));
            } else if (isRelationshipEndAction(log.getActionType())) {
                eventsByStudentId
                        .computeIfAbsent(log.getStudentId(), ignored -> new ArrayList<>())
                        .add(new RelationshipTimelineEvent(
                                log.getStudentId(),
                                log.getActionDate(),
                                false,
                                log.getActionReason(),
                                log.getAdminNameSnapshot()
                        ));
            }
        });

        List<Map<String, Object>> records = new ArrayList<>();
        for (Map.Entry<Long, List<RelationshipTimelineEvent>> entry : eventsByStudentId.entrySet()) {
            records.addAll(buildTimelineRecords(
                    entry.getValue(),
                    studentId -> buildTeacherStudentHistoryItem(studentId, studentMap, studentDetailMap, null, null, null, null, false),
                    (record, event) -> {
                        if (event.start()) {
                            record.put("startDate", event.occurredAt());
                        } else {
                            record.put("endDate", event.occurredAt());
                            record.put("endReason", event.reason());
                            record.put("operatorName", event.operatorName());
                        }
                    }
            ));
        }

        markCurrentTeacherRelationships(teacher, currentStudents, studentMap, studentDetailMap, records);
        sortRelationshipRecords(records);
        return records;
    }

    private List<Map<String, Object>> buildTimelineRecords(
            List<RelationshipTimelineEvent> events,
            java.util.function.Function<Long, Map<String, Object>> recordFactory,
            java.util.function.BiConsumer<Map<String, Object>, RelationshipTimelineEvent> eventApplier
    ) {
        List<RelationshipTimelineEvent> sortedEvents = new ArrayList<>(events);
        sortedEvents.sort(this::compareTimelineEvents);

        List<Map<String, Object>> records = new ArrayList<>();
        Map<Long, ArrayDeque<Map<String, Object>>> openRecordsByCounterpart = new HashMap<>();

        for (RelationshipTimelineEvent event : sortedEvents) {
            if (event.start()) {
                Map<String, Object> record = recordFactory.apply(event.counterpartId());
                eventApplier.accept(record, event);
                records.add(record);
                openRecordsByCounterpart
                        .computeIfAbsent(event.counterpartId(), ignored -> new ArrayDeque<>())
                        .addLast(record);
            } else {
                ArrayDeque<Map<String, Object>> openRecords = openRecordsByCounterpart.get(event.counterpartId());
                Map<String, Object> matchedRecord = openRecords != null ? openRecords.pollLast() : null;
                if (matchedRecord == null) {
                    matchedRecord = recordFactory.apply(event.counterpartId());
                    records.add(matchedRecord);
                }
                eventApplier.accept(matchedRecord, event);
            }
        }

        return records;
    }

    private void markCurrentStudentRelationship(
            User student,
            List<Map<String, Object>> records,
            Map<Long, User> teacherMap,
            Map<Long, UserDetail> teacherDetailMap
    ) {
        if (student.getMentorId() == null) {
            return;
        }

        Map<String, Object> currentRecord = records.stream()
                .filter(record -> student.getMentorId().equals(record.get("mentorId")) && record.get("endDate") == null)
                .max(Comparator.comparing(this::resolveHistorySortTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        if (currentRecord == null) {
            currentRecord = buildStudentRelationshipHistoryItem(student.getMentorId(), teacherMap, teacherDetailMap, null, null, null, null, true);
            records.add(currentRecord);
        }

        currentRecord.put("current", true);
        currentRecord.put("statusLabel", "当前导师");
    }

    private void markCurrentTeacherRelationships(
            User teacher,
            List<User> currentStudents,
            Map<Long, User> studentMap,
            Map<Long, UserDetail> studentDetailMap,
            List<Map<String, Object>> records
    ) {
        Set<Long> currentStudentIds = currentStudents.stream().map(User::getUid).collect(Collectors.toSet());

        for (Long currentStudentId : currentStudentIds) {
            Map<String, Object> currentRecord = records.stream()
                    .filter(record -> currentStudentId.equals(record.get("studentId")) && record.get("endDate") == null)
                    .max(Comparator.comparing(this::resolveHistorySortTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);

            if (currentRecord == null) {
                currentRecord = buildTeacherStudentHistoryItem(currentStudentId, studentMap, studentDetailMap, null, null, null, null, true);
                records.add(currentRecord);
            }

            currentRecord.put("current", true);
            currentRecord.put("statusLabel", "当前学生");
            currentRecord.put("mentorId", teacher.getUid());
        }
    }

    private int compareTimelineEvents(RelationshipTimelineEvent left, RelationshipTimelineEvent right) {
        if (left.occurredAt() == null && right.occurredAt() == null) {
            return 0;
        }
        if (left.occurredAt() == null) {
            return -1;
        }
        if (right.occurredAt() == null) {
            return 1;
        }

        int compare = left.occurredAt().compareTo(right.occurredAt());
        if (compare != 0) {
            return compare;
        }

        if (left.start() == right.start()) {
            return 0;
        }
        return left.start() ? 1 : -1;
    }

    private Map<String, Object> buildStudentRelationshipHistoryItem(
            Long mentorId,
            Map<Long, User> teacherMap,
            Map<Long, UserDetail> teacherDetailMap,
            Instant startDate,
            Instant endDate,
            String endReason,
            String operatorName,
            boolean current
    ) {
        User mentor = teacherMap.get(mentorId);
        UserDetail mentorDetail = teacherDetailMap.get(mentorId);

        Map<String, Object> relationship = new HashMap<>();
        relationship.put("mentorId", mentorId);
        relationship.put("mentorName", mentor != null ? mentor.getFullName() : "未知导师");
        relationship.put("mentorEmail", mentor != null ? mentor.getEmail() : null);
        relationship.put("photoUrl", mentorDetail != null ? mentorDetail.getPhotoUrl() : null);
        relationship.put("avatarUrl", mentor != null ? mentor.getAvatarUrl() : null);
        relationship.put("teacherPosition", mentorDetail != null ? mentorDetail.getTeacherPosition() : null);
        relationship.put("researchDirection", mentorDetail != null ? mentorDetail.getResearchDirection() : null);
        relationship.put("resume", mentorDetail != null ? mentorDetail.getResume() : null);
        relationship.put("startDate", startDate);
        relationship.put("endDate", endDate);
        relationship.put("endReason", endReason);
        relationship.put("operatorName", operatorName);
        relationship.put("current", current);
        relationship.put("statusLabel", current ? "当前导师" : "历史导师");
        return relationship;
    }

    private Map<String, Object> buildTeacherStudentHistoryItem(
            Long studentId,
            Map<Long, User> studentMap,
            Map<Long, UserDetail> studentDetailMap,
            Instant startDate,
            Instant endDate,
            String endReason,
            String operatorName,
            boolean current
    ) {
        User student = studentMap.get(studentId);
        UserDetail studentDetail = studentDetailMap.get(studentId);

        Map<String, Object> relationship = new HashMap<>();
        relationship.put("studentId", studentId);
        relationship.put("fullName", student != null ? student.getFullName() : "未知学生");
        relationship.put("email", student != null ? student.getEmail() : null);
        relationship.put("username", student != null ? student.getUsername() : null);
        relationship.put("avatarUrl", student != null ? student.getAvatarUrl() : null);
        relationship.put("photoUrl", studentDetail != null ? studentDetail.getPhotoUrl() : null);
        relationship.put("grade", studentDetail != null ? studentDetail.getStudentGrade() : null);
        relationship.put("class", studentDetail != null ? studentDetail.getStudentClass() : null);
        relationship.put("resume", studentDetail != null ? studentDetail.getResume() : null);
        relationship.put("researchDirection", studentDetail != null ? studentDetail.getResearchDirection() : null);
        relationship.put("netid", studentDetail != null ? studentDetail.getNetid() : null);
        relationship.put("startDate", startDate);
        relationship.put("endDate", endDate);
        relationship.put("endReason", endReason);
        relationship.put("operatorName", operatorName);
        relationship.put("current", current);
        relationship.put("statusLabel", current ? "当前学生" : "历史学生");
        return relationship;
    }

    private void sortRelationshipRecords(List<Map<String, Object>> records) {
        records.sort((left, right) -> {
            boolean leftCurrent = Boolean.TRUE.equals(left.get("current"));
            boolean rightCurrent = Boolean.TRUE.equals(right.get("current"));
            if (leftCurrent != rightCurrent) {
                return leftCurrent ? -1 : 1;
            }

            Instant leftSortTime = resolveHistorySortTime(left);
            Instant rightSortTime = resolveHistorySortTime(right);
            if (leftSortTime == null && rightSortTime == null) {
                return 0;
            }
            if (leftSortTime == null) {
                return 1;
            }
            if (rightSortTime == null) {
                return -1;
            }
            return rightSortTime.compareTo(leftSortTime);
        });
    }

    private Instant resolveHistorySortTime(Map<String, Object> record) {
        Object endDate = record.get("endDate");
        if (endDate instanceof Instant instant) {
            return instant;
        }

        Object startDate = record.get("startDate");
        if (startDate instanceof Instant instant) {
            return instant;
        }

        return null;
    }

    private Instant resolveRecordSortTime(Map<String, Object> record) {
        Object decisionDate = record.get("decisionDate");
        if (decisionDate instanceof Instant instant) {
            return instant;
        }

        Object applicationDate = record.get("applicationDate");
        if (applicationDate instanceof Instant instant) {
            return instant;
        }

        return null;
    }

    private Instant eventTime(Instant preferred, Instant fallback) {
        return preferred != null ? preferred : fallback;
    }

    private boolean isRelationshipStartAction(String actionType) {
        return ACTION_ADMIN_RESTORE_BIND.equals(actionType);
    }

    private boolean isRelationshipEndAction(String actionType) {
        return ACTION_ADMIN_UNBIND.equals(actionType)
                || ACTION_ADMIN_UNBIND_ALL.equals(actionType)
                || ACTION_ADMIN_RESTORE_OVERWRITE_UNBIND.equals(actionType)
                || ACTION_ADMIN_STUDENT_ARCHIVE_UNBIND.equals(actionType)
                || ACTION_ADMIN_TEACHER_ARCHIVE_UNBIND.equals(actionType);
    }

    private String translateStatus(String status) {
        if (STATUS_ACCEPTED.equals(status)) {
            return "已通过";
        }
        if (STATUS_REJECTED.equals(status)) {
            return "已拒绝";
        }
        if (STATUS_PENDING.equals(status)) {
            return "待审核";
        }
        return status;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String message) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);
        Map<String, String> errorData = new HashMap<>();
        errorData.put("error", message);
        responseBody.put("data", errorData);

        HttpStatus status;
        switch (code) {
            case 400:
                status = HttpStatus.BAD_REQUEST;
                break;
            case 401:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case 403:
                status = HttpStatus.FORBIDDEN;
                break;
            case 404:
                status = HttpStatus.NOT_FOUND;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(status).body(responseBody);
    }

    private <T> ResponseEntity<Map<String, Object>> buildSuccessResponse(int code, T data) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", code);
        responseBody.put("data", data);
        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }

    private record RelationshipTimelineEvent(
            Long counterpartId,
            Instant occurredAt,
            boolean start,
            String reason,
            String operatorName
    ) {
    }
}
