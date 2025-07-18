package gr.aueb.cf.schoolapp.service;

import gr.aueb.cf.schoolapp.core.exceptions.AppObjectAlreadyExists;
import gr.aueb.cf.schoolapp.core.exceptions.AppObjectInvalidArgumentException;
import gr.aueb.cf.schoolapp.core.filters.Paginated;
import gr.aueb.cf.schoolapp.core.filters.TeacherFilters;
import gr.aueb.cf.schoolapp.core.specification.TeacherSpecification;
import gr.aueb.cf.schoolapp.dto.TeacherInsertDTO;
import gr.aueb.cf.schoolapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.schoolapp.mapper.Mapper;
import gr.aueb.cf.schoolapp.model.Attachment;
import gr.aueb.cf.schoolapp.model.PersonalInfo;
import gr.aueb.cf.schoolapp.model.Teacher;
import gr.aueb.cf.schoolapp.repository.AttachmentRepository;
import gr.aueb.cf.schoolapp.repository.PersonalInfoRepository;
import gr.aueb.cf.schoolapp.repository.TeacherRepository;
import gr.aueb.cf.schoolapp.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Service
@RequiredArgsConstructor

public class TeacherService {

    private final static Logger LOGGER = LoggerFactory.getLogger(TeacherService.class);
    private final TeacherRepository teacherRepository;
    private final Mapper mapper;
    private final UserRepository userRepository;
    private final PersonalInfoRepository personalInfoRepository;
    private final AttachmentRepository attachmentRepository;

    @Transactional(rollbackOn = {AppObjectAlreadyExists.class, IOException.class})
    public TeacherReadOnlyDTO saveTeacher(TeacherInsertDTO teacherInsertDTO, MultipartFile amkaFile)
            throws AppObjectAlreadyExists, AppObjectInvalidArgumentException, IOException {

        // 1. Έλεγχοι για ύπαρξη χρήστη
        if (userRepository.findByVat(teacherInsertDTO.getUser().getVat()).isPresent()) {
            throw new AppObjectAlreadyExists("User", "User with vat=" + teacherInsertDTO.getUser().getVat() + " already exists.");
        }
        if (userRepository.findByUsername(teacherInsertDTO.getUser().getUsername()).isPresent()) {
            throw new AppObjectAlreadyExists("User", "User with username=" + teacherInsertDTO.getUser().getUsername() + " already exists.");
        }

        // 2. Μετατροπή DTO σε entity
        Teacher teacher = mapper.mapToTeacherEntity(teacherInsertDTO);

        // 3. Αποθήκευση αρχείου και απόδοση Attachment (αν υπάρχει)
        Attachment attachment = saveAmkaFile(amkaFile);
        if (attachment != null) {
            // 4. Αποθήκευση attachment ΜΟΝΟ του αρχείου ΠΡΩΤΑ
            Attachment savedAttachment = attachmentRepository.save(attachment);

            // 5. Set attachment στο PersonalInfo του teacher
            PersonalInfo personalInfo = teacher.getPersonalInfo();
            personalInfo.setAmkaFile(savedAttachment);

            // 6. Αποθήκευση PersonalInfo (σβήνουμε αυτό το βήμα αν έχεις cascade ALL στο Teacher)
            personalInfo = personalInfoRepository.save(personalInfo);

            // 7. Ενημέρωση Teacher με updated PersonalInfo
            teacher.setPersonalInfo(personalInfo);
        }

        // 8. Αποθήκευση Teacher (cascade αποθηκεύει και personalInfo)
        Teacher savedTeacher = teacherRepository.saveAndFlush(teacher);

        // 9. Επιστροφή του DTO
        return mapper.mapToTeacherReadOnlyDTO(savedTeacher);
    }



    public Attachment saveAmkaFile(MultipartFile amkaFile) throws IOException {
        if (amkaFile == null || amkaFile.isEmpty()) return null;

        String originalFilename = amkaFile.getOriginalFilename();
        String savedName = UUID.randomUUID().toString() + getFileExtension(originalFilename);

        String uploadDirectory = "uploads/";
        Path filePath = Paths.get(uploadDirectory + savedName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, amkaFile.getBytes());

        Attachment attachment = new Attachment();
        attachment.setFilename(originalFilename);
        attachment.setSavedName(savedName);
        attachment.setFilePath(filePath.toString());
        attachment.setContentType(amkaFile.getContentType());
        attachment.setExtension(getFileExtension(originalFilename));

        LOGGER.info("File saved successfully");
        return attachment;
    }


    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    @Transactional
    public Page<TeacherReadOnlyDTO> getPaginatedTeachers(int page, int size) {
        String defaultSort = "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(defaultSort).ascending());
        return teacherRepository.findAll(pageable).map(mapper::mapToTeacherReadOnlyDTO);
    }


    @Transactional
    public Page<TeacherReadOnlyDTO> getPaginatedSortedTeachers(int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return teacherRepository.findAll(pageable).map(mapper::mapToTeacherReadOnlyDTO);
    }

    public List<TeacherReadOnlyDTO> getTeachersFiltered(TeacherFilters filters){
        return teacherRepository.findAll(getSpecsFromFilters(filters))
                .stream().map(mapper::mapToTeacherReadOnlyDTO).collect(Collectors.toList());
    }

    @Transactional
    public Paginated<TeacherReadOnlyDTO> getTeachersFilteredPaginated(TeacherFilters filters) {
        var filtered = teacherRepository.findAll(getSpecsFromFilters(filters), filters.getPageable());
        return new Paginated<>(filtered.map(mapper::mapToTeacherReadOnlyDTO));
    }

    private Specification<Teacher> getSpecsFromFilters(TeacherFilters filters) {
        return Specification
                .where(TeacherSpecification.teacherStringFieldLike("uuid", filters.getUuid()))
                .and(TeacherSpecification.teacherUserAfmIs(filters.getUserAfm()))
                .and(TeacherSpecification.teacherPersonalInfoAmkaIs(filters.getUserAmka()))
                .and(TeacherSpecification.teacherIsActive(filters.getIsActive()));
    }



}
