package gr.aueb.cf.schoolapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "teachers")
public class Teacher extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToOne(cascade = CascadeType.ALL)     //οτιδήποτε αποθηκεύουμαι στον Teacher αντανακλά στον user
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "personal_info_id")
    private PersonalInfo personalInfo;



    @PrePersist
    public void initializeUUID() {

        if (uuid == null) uuid = UUID.randomUUID().toString();
    }
}
