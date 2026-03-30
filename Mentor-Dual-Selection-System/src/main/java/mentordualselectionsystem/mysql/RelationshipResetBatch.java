package mentordualselectionsystem.mysql;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "relationship_reset_batches",
        schema = "teacherselectionsystem",
        indexes = {
                @Index(name = "idx_relationship_reset_batches_status", columnList = "status"),
                @Index(name = "idx_relationship_reset_batches_created_at", columnList = "created_at")
        }
)
public class RelationshipResetBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Lob
    @Column(name = "reason", columnDefinition = "LONGTEXT")
    private String reason;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_by_name_snapshot", nullable = false)
    private String createdByNameSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "affected_student_count", nullable = false)
    private Integer affectedStudentCount;

    @Column(name = "affected_mentor_count", nullable = false)
    private Integer affectedMentorCount;

    @Column(name = "restore_count", nullable = false)
    private Integer restoreCount;

    @Column(name = "last_restored_at")
    private Instant lastRestoredAt;

    @Column(name = "last_restored_by_id")
    private Long lastRestoredById;

    @Column(name = "last_restored_by_name_snapshot")
    private String lastRestoredByNameSnapshot;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByNameSnapshot() {
        return createdByNameSnapshot;
    }

    public void setCreatedByNameSnapshot(String createdByNameSnapshot) {
        this.createdByNameSnapshot = createdByNameSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getAffectedStudentCount() {
        return affectedStudentCount;
    }

    public void setAffectedStudentCount(Integer affectedStudentCount) {
        this.affectedStudentCount = affectedStudentCount;
    }

    public Integer getAffectedMentorCount() {
        return affectedMentorCount;
    }

    public void setAffectedMentorCount(Integer affectedMentorCount) {
        this.affectedMentorCount = affectedMentorCount;
    }

    public Integer getRestoreCount() {
        return restoreCount;
    }

    public void setRestoreCount(Integer restoreCount) {
        this.restoreCount = restoreCount;
    }

    public Instant getLastRestoredAt() {
        return lastRestoredAt;
    }

    public void setLastRestoredAt(Instant lastRestoredAt) {
        this.lastRestoredAt = lastRestoredAt;
    }

    public Long getLastRestoredById() {
        return lastRestoredById;
    }

    public void setLastRestoredById(Long lastRestoredById) {
        this.lastRestoredById = lastRestoredById;
    }

    public String getLastRestoredByNameSnapshot() {
        return lastRestoredByNameSnapshot;
    }

    public void setLastRestoredByNameSnapshot(String lastRestoredByNameSnapshot) {
        this.lastRestoredByNameSnapshot = lastRestoredByNameSnapshot;
    }
}
