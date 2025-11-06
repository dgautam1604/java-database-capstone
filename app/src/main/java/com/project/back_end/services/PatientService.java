package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service; // @Service annotation lives here
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatientService {
// 1. **Add @Service Annotation**:
//    - The `@Service` annotation is used to mark this class as a Spring service component.
//    - It will be managed by Spring's container and used for business logic related to patients and appointments.
//    - Instruction: Ensure that the `@Service` annotation is applied above the class declaration.

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    // 2. **Constructor Injection for Dependencies**:
    //    - The `PatientService` class has dependencies on `PatientRepository`, `AppointmentRepository`, and `TokenService`.
    //    - These dependencies are injected via the constructor to maintain good practices of dependency injection and testing.
    //    - Instruction: Ensure constructor injection is used for all the required dependencies.
    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 3. **createPatient Method**:
    //    - Creates a new patient in the database. It saves the patient object using the `PatientRepository`.
    //    - If the patient is successfully saved, the method returns `1`; otherwise, it logs the error and returns `0`.
    //    - Instruction: Ensure that error handling is done properly and exceptions are caught and logged appropriately.
    @Transactional
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            // log if you have a logger
            return 0;
        }
    }

    // 4. **getPatientAppointment Method**:
    //    - Retrieves a list of appointments for a specific patient, based on their ID.
    //    - The appointments are then converted into `AppointmentDTO` objects for easier consumption by the API client.
    //    - This method is marked as `@Transactional` to ensure database consistency during the transaction.
    //    - Instruction: Ensure that appointment data is properly converted into DTOs and the method handles errors gracefully.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        Map<String, Object> body = new HashMap<>();
        try {
            String emailFromToken = tokenService.extractEmail(token);
            if (emailFromToken == null) {
                body.put("message", "Invalid token");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }

            Patient authed = patientRepository.findByEmail(emailFromToken);
            if (authed == null || !Objects.equals(authed.getId(), id)) {
                body.put("message", "Unauthorized");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }

            List<Appointment> appts = appointmentRepository.findByPatientId(id);
            List<AppointmentDTO> dto = appts.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            body.put("appointments", dto);
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Internal error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 5. **filterByCondition Method**:
    //    - Filters appointments for a patient based on the condition (e.g., "past" or "future").
    //    - Retrieves appointments with a specific status (0 for future, 1 for past) for the patient.
    //    - Converts the appointments into `AppointmentDTO` and returns them in the response.
    //    - Instruction: Ensure the method correctly handles "past" and "future" conditions, and that invalid conditions are caught and returned as errors.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Map<String, Object> body = new HashMap<>();
        try {
            if (condition == null) {
                body.put("message", "Condition required");
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
            }
            int status;
            String c = condition.trim().toLowerCase(Locale.ROOT);
            if ("past".equals(c)) status = 1;
            else if ("future".equals(c)) status = 0;
            else {
                body.put("message", "Condition must be 'past' or 'future'");
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
            }

            List<Appointment> appts =
                    appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status);
            List<AppointmentDTO> dto = appts.stream().map(this::toDTO).collect(Collectors.toList());

            body.put("appointments", dto);
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Internal error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 6. **filterByDoctor Method**:
    //    - Filters appointments for a patient based on the doctor's name.
    //    - It retrieves appointments where the doctor’s name matches the given value, and the patient ID matches the provided ID.
    //    - Instruction: Ensure that the method correctly filters by doctor's name and patient ID and handles any errors or invalid cases.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        Map<String, Object> body = new HashMap<>();
        try {
            List<Appointment> appts =
                    appointmentRepository.filterByDoctorNameAndPatientId(name == null ? "" : name, patientId);
            List<AppointmentDTO> dto = appts.stream().map(this::toDTO).collect(Collectors.toList());

            body.put("appointments", dto);
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Internal error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 7. **filterByDoctorAndCondition Method**:
    //    - Filters appointments based on both the doctor's name and the condition (past or future) for a specific patient.
    //    - This method combines filtering by doctor name and appointment status (past or future).
    //    - Converts the appointments into `AppointmentDTO` objects and returns them in the response.
    //    - Instruction: Ensure that the filter handles both doctor name and condition properly, and catches errors for invalid input.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        Map<String, Object> body = new HashMap<>();
        try {
            if (condition == null) {
                body.put("message", "Condition required");
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
            }
            int status;
            String c = condition.trim().toLowerCase(Locale.ROOT);
            if ("past".equals(c)) status = 1;
            else if ("future".equals(c)) status = 0;
            else {
                body.put("message", "Condition must be 'past' or 'future'");
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
            }

            List<Appointment> appts =
                    appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(
                            name == null ? "" : name, patientId, status);

            List<AppointmentDTO> dto = appts.stream().map(this::toDTO).collect(Collectors.toList());

            body.put("appointments", dto);
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Internal error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 8. **getPatientDetails Method**:
    //    - Retrieves patient details using the `tokenService` to extract the patient's email from the provided token.
    //    - Once the email is extracted, it fetches the corresponding patient from the `patientRepository`.
    //    - It returns the patient's information in the response body.
    //    - Instruction: Make sure that the token extraction process works correctly and patient details are fetched properly based on the extracted email.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        Map<String, Object> body = new HashMap<>();
        try {
            String email = tokenService.extractEmail(token);
            if (email == null) {
                body.put("message", "Invalid token");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }
            Patient p = patientRepository.findByEmail(email);
            if (p == null) {
                body.put("message", "Patient not found");
                return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
            }

            Map<String, Object> patient = new HashMap<>();
            patient.put("id", p.getId());
            patient.put("name", p.getName());
            patient.put("email", p.getEmail());
            patient.put("phone", p.getPhone());
            patient.put("address", p.getAddress());

            body.put("patient", patient);
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Internal error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 9. **Handling Exceptions and Errors**:
    //    - The service methods handle exceptions using try-catch blocks and log any issues that occur. If an error occurs during database operations, the service responds with appropriate HTTP status codes (e.g., `500 Internal Server Error`).
    //    - Instruction: Ensure that error handling is consistent across the service, with proper logging and meaningful error messages returned to the client.

    // 10. **Use of DTOs (Data Transfer Objects)**:
    //    - The service uses `AppointmentDTO` to transfer appointment-related data between layers. This ensures that sensitive or unnecessary data (e.g., password or private patient information) is not exposed in the response.
    //    - Instruction: Ensure that DTOs are used appropriately to limit the exposure of internal data and only send the relevant fields to the client.

    // ---- helper mapping ----
    private AppointmentDTO toDTO(Appointment a) {
        return new AppointmentDTO(
                a.getId(),
                a.getDoctor().getId(),
                a.getDoctor().getName(),
                a.getPatient().getId(),
                a.getPatient().getName(),
                a.getPatient().getEmail(),
                a.getPatient().getPhone(),
                a.getPatient().getAddress(),
                a.getAppointmentTime(),
                a.getStatus()
        );
    }
}
