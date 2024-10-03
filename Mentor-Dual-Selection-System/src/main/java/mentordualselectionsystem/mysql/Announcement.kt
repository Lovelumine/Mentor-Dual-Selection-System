package mentordualselectionsystem.mysql;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;
import java.time.Instant;

@Entity
@Table(name = "announcements")
open class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "is_published", nullable = false)
    open var isPublished: Boolean? = false;

    @NotNull
    @Column(name = "last_modified", nullable = false)
    open var lastModified: Instant? = null; // 默认不设置

    @Size(max = 255)
    @NotNull
    @Column(name = "title", nullable = false)
    open var title: String? = null;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false)
    open var content: String? = null;

    @Size(max = 255)
    @Column(name = "attachment_url")
    open var attachmentUrl: String? = null;
}
