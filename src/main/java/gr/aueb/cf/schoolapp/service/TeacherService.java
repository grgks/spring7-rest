package gr.aueb.cf.schoolapp.service;

import gr.aueb.cf.schoolapp.core.exceptions.AppObjectAlreadyExists;
import gr.aueb.cf.schoolapp.core.exceptions.AppObjectInvalidArgumentException;
import gr.aueb.cf.schoolapp.dto.TeacherInsertDTO;
import gr.aueb.cf.schoolapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.schoolapp.mapper.Mapper;
import gr.aueb.cf.schoolapp.model.Attachment;
import gr.aueb.cf.schoolapp.model.PersonalInfo;
import gr.aueb.cf.schoolapp.model.Teacher;
import gr.aueb.cf.schoolapp.repository.PersonalInfoRepository;
import gr.aueb.cf.schoolapp.repository.TeacherRepository;
import gr.aueb.cf.schoolapp.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final static Logger LOGGER = LoggerFactory.getLogger(TeacherService.class);
    private final TeacherRepository teacherRepository;
    private final Mapper mapper;
    private final UserRepository userRepository;
    private final PersonalInfoRepository personalInfoRepository;

    @Transactional(rollbackOn = {AppObjectAlreadyExists.class, IOException.class})
    public TeacherReadOnlyDTO saveTeacher(TeacherInsertDTO teacherInsertDTO, MultipartFile amkaFile)
            throws AppObjectAlreadyExists, AppObjectInvalidArgumentException, IOException {

        if (userRepository.findByVat(teacherInsertDTO.getUser().getVat()).isPresent()) {
            throw new AppObjectAlreadyExists("User", "User with vat=" + teacherInsertDTO.getUser().getVat() + " already exists.");
        }

        if (userRepository.findByUsername(teacherInsertDTO.getUser().getUsername()).isPresent()) {
            throw new AppObjectAlreadyExists("User", "User with username=" + teacherInsertDTO.getUser().getUsername() + " already exists.");
        }

        // Saving the teaches cascades user & personal info
        Teacher teacher = mapper.mapToTeacherEntity(teacherInsertDTO);

        saveAmkaFile(teacher.getPersonalInfo(), amkaFile);
        Teacher savedTeacher = teacherRepository.save(teacher);

        return mapper.mapToTeacherReadOnlyDTO(savedTeacher);
    }

    public void saveAmkaFile(PersonalInfo personalInfo, MultipartFile amkaFile)
            throws IOException {

        if (amkaFile == null || amkaFile.isEmpty()) return;

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

        personalInfo.setAmkaFile(attachment);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

}
