package gr.aueb.cf.schoolapp.model;

import gr.aueb.cf.schoolapp.model.static_data.EducationalUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToMany
    @JoinTable(
            name = "employess_edu_units"
    )
    private Set<EducationalUnit> eduUnits = new HashSet<>();

    public void addEducationalUnit(EducationalUnit educationalUnit) {
        if (eduUnits == null) eduUnits = new HashSet<>();
        eduUnits.add(educationalUnit);
    }

    public boolean hasEducationalUnits() {
        return eduUnits != null && !eduUnits.isEmpty();
    }

    @PrePersist
    public void initializeUUID() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
    }
}
