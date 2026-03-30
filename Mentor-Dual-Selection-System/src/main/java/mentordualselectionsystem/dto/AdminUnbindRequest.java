package mentordualselectionsystem.dto;

import java.util.List;

public class AdminUnbindRequest {
    private List<Long> studentIds;

    public List<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<Long> studentIds) {
        this.studentIds = studentIds;
    }
}
