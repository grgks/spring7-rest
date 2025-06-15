package gr.aueb.cf.schoolapp.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aueb.cf.schoolapp.core.exceptions.*;
import gr.aueb.cf.schoolapp.core.filters.Paginated;
import gr.aueb.cf.schoolapp.core.filters.TeacherFilters;
import gr.aueb.cf.schoolapp.dto.TeacherInsertDTO;
import gr.aueb.cf.schoolapp.dto.TeacherReadOnlyDTO;
import gr.aueb.cf.schoolapp.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TeacherRestController {




    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherRestController.class);
    private final TeacherService teacherService;
    private final ObjectMapper objectMapper;

    @Operation(
            summary = "Get all teachers paginated",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Teachers Found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TeacherReadOnlyDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access Denied",
                            content = @Content
                    )
            }
    )
    @GetMapping("/teachers")
    public ResponseEntity<Page<TeacherReadOnlyDTO>> getPaginatedTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1") int size) {

        Page<TeacherReadOnlyDTO> teachersPage = teacherService.getPaginatedTeachers(page, size);
        return new ResponseEntity<>(teachersPage, HttpStatus.OK);
    }

    @Operation(
            summary = "Save a teacher",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Teacher inserted",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TeacherReadOnlyDTO.class)
                            )
                    )
            }
    )

    @PostMapping(value = "/teachers/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TeacherReadOnlyDTO> saveTeacher(
            @Valid @RequestPart("teacher") String teacherJson,
            @Nullable @RequestPart("amkaFile") MultipartFile amkaFile)
            throws AppObjectInvalidArgumentException, ValidationException, AppObjectAlreadyExists, AppServerException {

        LOGGER.info("Raw teacher JSON received: {}", teacherJson);
        if (amkaFile != null) {
            LOGGER.info("File name: {}", amkaFile.getOriginalFilename());
        }

        try {
            // Deserialize JSON string to DTO

            TeacherInsertDTO teacherInsertDTO = objectMapper.readValue(teacherJson, TeacherInsertDTO.class);

            // (Προαιρετικά) έλεγχος validation χειροκίνητα
            // validate(teacherInsertDTO);

            TeacherReadOnlyDTO teacherReadOnlyDTO = teacherService.saveTeacher(teacherInsertDTO, amkaFile);
            return ResponseEntity.ok(teacherReadOnlyDTO);
        } catch (IOException e) {
            LOGGER.error("Failed to parse teacher JSON", e);
            throw new AppServerException("Teacher", "Invalid teacher data");
        }
    }

    @Operation(
            summary = "Get all teachers filtered",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Teachers Found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TeacherReadOnlyDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access Denied",
                            content = @Content
                    )
            }
    )
    @PostMapping("/teachers/all")
    public ResponseEntity<List<TeacherReadOnlyDTO>> getTeachers(@Nullable @RequestBody TeacherFilters filters,
                                                                Principal principal)
            throws AppObjectNotFoundException, AppObjectNotAuthorizedException {
        try {
            if (filters == null) filters = TeacherFilters.builder().build();
            return ResponseEntity.ok(teacherService.getTeachersFiltered(filters));
        } catch (Exception e) {
            LOGGER.warn("Could not get teachers.", e);
            throw e;
        }
    }

    @Operation(
            summary = "Get all teachers filtered",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Teachers Found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TeacherReadOnlyDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access Denied",
                            content = @Content
                    )
            }
    )
    @PostMapping("/teachers/all/paginated")
    public ResponseEntity<Paginated<TeacherReadOnlyDTO>> getTeachersFilteredPaginated(@Nullable @RequestBody TeacherFilters filters,
                                                                                      Principal principal)
            throws AppObjectNotFoundException, AppObjectNotAuthorizedException {
        try {
            if (filters == null) filters = TeacherFilters.builder().build();
            return ResponseEntity.ok(teacherService.getTeachersFilteredPaginated(filters));
        } catch (Exception e) {
            LOGGER.warn("Could not get teachers.", e);
            throw e;
        }
    }
}

