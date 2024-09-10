package mentordualselectionsystem.mysql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "user_details", schema = "teacherselectionsystem")
public class UserDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    @Column(name = "uid", nullable = false)
    private Long uid;

    @Size(max = 255)
    @Column(name = "photo_url")
    private String photoUrl;

    @Size(max = 255)
    @Column(name = "NetID")
    private String NetID;

    @Size(max = 255)
    @Column(name = "teacher_position")
    private String position;

    @Lob
    @Column(name = "research_direction")
    private String researchDirection;

    @Lob
    @Column(name = "professional_direction")
    private String professionalDirection;

    @Lob
    @Column(name = "resume")
    private String resume;

    @Size(max = 50)
    @Column(name = "student_grade", length = 50)
    private String student_grade;

    @Column(name = "student_class")
    private Integer studentClass;

    @Size(max = 50)
    @Column(name = "grade", length = 50)
    private String grade;

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Integer getStudentClass() {
        return studentClass;
    }

    public void setStudentClass(Integer studentClass) {
        this.studentClass = studentClass;
    }

    public Long getId() {
        return uid;
    }

    public void setId(Long id) {
        this.uid = id;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getNetID() {
        return NetID;
    }

    public void setNetID(String NetID) {
        this.NetID = NetID;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getResearchDirection() {
        return researchDirection;
    }

    public void setResearchDirection(String researchDirection) {
        this.researchDirection = researchDirection;
    }

    public String getProfessionalDirection() {
        return professionalDirection;
    }

    public void setProfessionalDirection(String professionalDirection) {
        this.professionalDirection = professionalDirection;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    public String getStudentGrade() {
        return student_grade;
    }

    public void setStudentGrade(String StudentGrade) {
        this.student_grade = StudentGrade;
    }

}