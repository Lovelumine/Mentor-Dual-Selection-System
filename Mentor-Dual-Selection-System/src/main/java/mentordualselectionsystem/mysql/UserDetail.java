package mentordualselectionsystem.mysql;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "user_details")
public class UserDetail {

    @Id
    @Column(name = "uid", nullable = false)
    private Long uid;

    @Size(max = 255)
    @Column(name = "photo_url")
    private String photoUrl; // 头像 URL

    @Size(max = 255)
    @Column(name = "teacher_position")
    private String teacherPosition; // 教师职位

    @Lob
    @Column(name = "research_direction")
    private String researchDirection; // 研究方向

    @Lob
    @Column(name = "professional_direction")
    private String professionalDirection; // 专业方向

    @Lob
    @Column(name = "resume")
    private String resume; // 个人简历

    @Size(max = 255)
    @Column(name = "netid")
    private String netid; // 教师编号或网络 ID

    @Column(name = "student_class")
    private Integer studentClass; // 学生班级

    @Size(max = 50)
    @Column(name = "student_grade", length = 50)
    private String studentGrade; // 学生年级


    // Getter 和 Setter 方法
    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getTeacherPosition() {
        return teacherPosition;
    }

    public void setTeacherPosition(String teacherPosition) {
        this.teacherPosition = teacherPosition;
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

    public String getNetid() {
        return netid;
    }

    public void setNetid(String netid) {
        this.netid = netid;
    }

    public Integer getStudentClass() {
        return studentClass;
    }

    public void setStudentClass(Integer studentClass) {
        this.studentClass = studentClass;
    }

    public String getStudentGrade() {
        return studentGrade;
    }

    public void setStudentGrade(String studentGrade) {
        this.studentGrade = studentGrade;
    }

}
