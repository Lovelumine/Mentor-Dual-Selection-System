package mentordualselectionsystem.repositories;

import mentordualselectionsystem.mysql.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
    List<Announcement> findAllByOrderByLastModifiedDesc();
}
