package mentordualselectionsystem.mysql;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long role_id;

    private String roleName;

    // Getters and Setters
    public Long getId() {
        return role_id;
    }

    public void setId(Long id) {
        this.role_id= id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
