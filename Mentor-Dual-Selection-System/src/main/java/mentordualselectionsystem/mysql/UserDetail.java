package mentordualselectionsystem.mysql;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_details", schema = "teacherselectionsystem")
public class UserDetail {
    @Id
    @Column(name = "uid", nullable = false)
    private Long uid;
    @Size(max = 255)
    @Column(name = "photo_url")
    private String photoUrl;

    @Size(max = 255)
    @Column(name = "teacher_position")
    private String teacherPosition;

    @Lob
    @Column(name = "research_direction")
    private String researchDirection;

    @Lob
    @Column(name = "professional_direction")
    private String professionalDirection;

    @Lob
    @Column(name = "resume")
    private String resume;

    @Size(max = 255)
    @Column(name = "netid")
    private String netid;

    @Column(name = "student_class")
    private Integer studentClass;

    @Size(max = 50)
    @Column(name = "student_grade", length = 50)
    private String studentGrade;

    @Size(max = 50)
    @Column(name = "grade", length = 50)
    private String grade;
}
