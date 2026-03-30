package mentordualselectionsystem.mysql;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "admin_relationship_action_logs",
        schema = "teacherselectionsystem",
        indexes = {
                @Index(name = "idx_admin_relationship_action_logs_student_id", columnList = "student_id"),
                @Index(name = "idx_admin_relationship_action_logs_mentor_id", columnList = "mentor_id"),
                @Index(name = "idx_admin_relationship_action_logs_action_date", columnList = "action_date"),
                @Index(name = "idx_admin_relationship_action_logs_batch_id", columnList = "batch_id")
        }
)
public class AdminRelationshipActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "student_name_snapshot", nullable = false)
    private String studentNameSnapshot;

    @Column(name = "mentor_name_snapshot", nullable = false)
    private String mentorNameSnapshot;

    @Column(name = "admin_name_snapshot", nullable = false)
    private String adminNameSnapshot;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "batch_id")
    private Long batchId;

    @Lob
    @Column(name = "action_reason", columnDefinition = "LONGTEXT")
    private String actionReason;

    @Column(name = "action_date", nullable = false, updatable = false)
    private Instant actionDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getMentorId() {
        return mentorId;
    }

    public void setMentorId(Long mentorId) {
        this.mentorId = mentorId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getStudentNameSnapshot() {
        return studentNameSnapshot;
    }

    public void setStudentNameSnapshot(String studentNameSnapshot) {
        this.studentNameSnapshot = studentNameSnapshot;
    }

    public String getMentorNameSnapshot() {
        return mentorNameSnapshot;
    }

    public void setMentorNameSnapshot(String mentorNameSnapshot) {
        this.mentorNameSnapshot = mentorNameSnapshot;
    }

    public String getAdminNameSnapshot() {
        return adminNameSnapshot;
    }

    public void setAdminNameSnapshot(String adminNameSnapshot) {
        this.adminNameSnapshot = adminNameSnapshot;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getActionReason() {
        return actionReason;
    }

    public void setActionReason(String actionReason) {
        this.actionReason = actionReason;
    }

    public Instant getActionDate() {
        return actionDate;
    }

    public void setActionDate(Instant actionDate) {
        this.actionDate = actionDate;
    }
}
