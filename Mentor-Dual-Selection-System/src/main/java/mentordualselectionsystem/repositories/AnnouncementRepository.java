package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
}
