package mentordualselectionsystem.dto;

import java.util.List;

public class TeacherArchiveRequest {

    private List<Long> teacherIds;
    private String reason;

    public List<Long> getTeacherIds() {
        return teacherIds;
    }

    public void setTeacherIds(List<Long> teacherIds) {
        this.teacherIds = teacherIds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
