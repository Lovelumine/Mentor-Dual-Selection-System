package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.Announcement;
import mentordualselectionsystem.repositories.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Autowired
    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    public Announcement createAnnouncement(Announcement announcement) {
        return announcementRepository.save(announcement);
    }

    public Announcement updateAnnouncement(Integer id, Announcement announcement) {
        if (!announcementRepository.existsById(id)) {
            return null; // 公告未找到
        }
        announcement.setId(id); // 设置ID以便更新
        return announcementRepository.save(announcement);
    }

    public boolean deleteAnnouncement(Integer id) {
        if (!announcementRepository.existsById(id)) {
            return false; // 公告未找到
        }
        announcementRepository.deleteById(id);
        return true;
    }
}
