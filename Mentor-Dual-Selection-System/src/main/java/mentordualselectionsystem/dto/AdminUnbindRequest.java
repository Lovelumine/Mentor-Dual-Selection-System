package mentordualselectionsystem.dto;

import java.util.List;

public class AdminUnbindRequest {
    private List<Long> studentIds;
    private String reason;

    public List<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<Long> studentIds) {
        this.studentIds = studentIds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
