package gr.aueb.cf.schoolapp.core.filters;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class TeacherFilters extends GenericFilters {

    @Nullable
    private String uuid;

    @Nullable
    private String userAfm;

    @Nullable
    private String userAmka;

    @Nullable
    private Boolean isActive;
}
