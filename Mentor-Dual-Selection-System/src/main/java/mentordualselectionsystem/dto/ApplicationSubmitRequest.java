package mentordualselectionsystem.dto;

public class ApplicationSubmitRequest {
    private Long mentorId;
    private String reason;

    // Getters and Setters
    public Long getMentorId() {
        return mentorId;
    }

    public void setMentorId(Long mentorId) {
        this.mentorId = mentorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
