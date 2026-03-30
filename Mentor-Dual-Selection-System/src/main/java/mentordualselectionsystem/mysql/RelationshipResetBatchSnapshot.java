package mentordualselectionsystem.mysql;

import jakarta.persistence.*;

@Entity
@Table(
        name = "relationship_reset_batch_snapshots",
        schema = "teacherselectionsystem",
        indexes = {
                @Index(name = "idx_relationship_reset_batch_snapshots_batch_id", columnList = "batch_id"),
                @Index(name = "idx_relationship_reset_batch_snapshots_student_id", columnList = "student_id"),
                @Index(name = "idx_relationship_reset_batch_snapshots_mentor_id", columnList = "mentor_id")
        }
)
public class RelationshipResetBatchSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    @Column(name = "student_name_snapshot", nullable = false)
    private String studentNameSnapshot;

    @Column(name = "mentor_name_snapshot", nullable = false)
    private String mentorNameSnapshot;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
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
}
