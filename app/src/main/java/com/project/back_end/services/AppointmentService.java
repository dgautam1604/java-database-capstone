package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service; // Hint: @Service lives here
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    // 1. **Add @Service Annotation**:
    //    - To indicate that this class is a service layer class for handling business logic.
    //    - The `@Service` annotation should be added before the class declaration to mark it as a Spring service component.
    //    - Instruction: Add `@Service` above the class definition.

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService; // used to extract user/role from token

    // 2. **Constructor Injection for Dependencies**:
    //    - The `AppointmentService` class requires several dependencies like `AppointmentRepository`, `Service`, `TokenService`, `PatientRepository`, and `DoctorRepository`.
    //    - These dependencies should be injected through the constructor.
    //    - Instruction: Ensure constructor injection is used for proper dependency management in Spring.
    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              TokenService tokenService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
    }

    // 3. **Add @Transactional Annotation for Methods that Modify Database**:
    //    - The methods that modify or update the database should be annotated with `@Transactional` to ensure atomicity and consistency of the operations.
    //    - Instruction: Add the `@Transactional` annotation above methods that interact with the database, especially those modifying data.

    // 4. **Book Appointment Method**:
    //    - Responsible for saving the new appointment to the database.
    //    - If the save operation fails, it returns `0`; otherwise, it returns `1`.
    //    - Instruction: Ensure that the method handles any exceptions and returns an appropriate result code.
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            // basic referential checks
            if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) return 0;
            if (appointment.getPatient() == null || appointment.getPatient().getId() == null) return 0;

            Optional<Doctor> doc = doctorRepository.findById(appointment.getDoctor().getId());
            Optional<Patient> pat = patientRepository.findById(appointment.getPatient().getId());
            if (doc.isEmpty() || pat.isEmpty()) return 0;

            // validate slot (simple overlap guard using 1h duration)
            String validationError = validateAppointment(appointment, appointment.getId());
            if (validationError != null) return 0;

            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // 5. **Update Appointment Method**:
    //    - This method is used to update an existing appointment based on its ID.
    //    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
    //    - If the update is successful, it saves the appointment; otherwise, it returns an appropriate error message.
    //    - Instruction: Ensure proper validation and error handling is included for appointment updates.
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> body = new HashMap<>();
        if (appointment.getId() == null) {
            body.put("message", "Appointment id is required");
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }

        return appointmentRepository.findById(appointment.getId())
                .map(existing -> {
                    // ensure patient/doctor exist
                    Long doctorId = appointment.getDoctor() != null ? appointment.getDoctor().getId() : null;
                    Long patientId = appointment.getPatient() != null ? appointment.getPatient().getId() : null;
                    if (doctorId == null || patientId == null ||
                            doctorRepository.findById(doctorId).isEmpty() ||
                            patientRepository.findById(patientId).isEmpty()) {
                        body.put("message", "Invalid doctor or patient");
                        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
                    }

                    // validate slot and business rules
                    String validationError = validateAppointment(appointment, appointment.getId());
                    if (validationError != null) {
                        body.put("message", validationError);
                        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
                    }

                    existing.setDoctor(appointment.getDoctor());
                    existing.setPatient(appointment.getPatient());
                    existing.setAppointmentTime(appointment.getAppointmentTime());
                    existing.setStatus(appointment.getStatus());
                    appointmentRepository.save(existing);

                    body.put("message", "Appointment updated");
                    return new ResponseEntity<>(body, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    body.put("message", "Appointment not found");
                    return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
                });
    }

    // 6. **Cancel Appointment Method**:
    //    - This method cancels an appointment by deleting it from the database.
    //    - It ensures the patient who owns the appointment is trying to cancel it and handles possible errors.
    //    - Instruction: Make sure that the method checks for the patient ID match before deleting the appointment.
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> body = new HashMap<>();
        Optional<Appointment> apptOpt = appointmentRepository.findById(id);
        if (apptOpt.isEmpty()) {
            body.put("message", "Appointment not found");
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }

        Appointment appt = apptOpt.get();

        // Authorization: only the owning patient can cancel
        Long requesterId = tokenService.extractUserId(token);
        String role = tokenService.extractRole(token); // "PATIENT", "DOCTOR", "ADMIN", etc.
        if (!"PATIENT".equalsIgnoreCase(role) || requesterId == null ||
                !Objects.equals(appt.getPatient().getId(), requesterId)) {
            body.put("message", "Not authorized to cancel this appointment");
            return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
        }

        appointmentRepository.delete(appt);
        body.put("message", "Appointment cancelled");
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    // 7. **Get Appointments Method**:
    //    - This method retrieves a list of appointments for a specific doctor on a particular day, optionally filtered by the patient's name.
    //    - It uses `@Transactional` to ensure that database operations are consistent and handled in a single transaction.
    //    - Instruction: Ensure the correct use of transaction boundaries, especially when querying the database for appointments.
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> out = new HashMap<>();

        // doctorId comes from token (doctor viewing their day)
        Long doctorId = tokenService.extractUserId(token);
        String role = tokenService.extractRole(token);
        if (!"DOCTOR".equalsIgnoreCase(role) || doctorId == null) {
            out.put("message", "Not authorized");
            return out;
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<Appointment> appts;
        if (pname != null && !pname.isBlank()) {
            appts = appointmentRepository
                    .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(doctorId, pname, start, end);
        } else {
            appts = appointmentRepository
                    .findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
        }

        List<AppointmentDTO> dtoList = appts.stream()
                .map(a -> new AppointmentDTO(
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
                ))
                .collect(Collectors.toList());

        out.put("appointments", dtoList);
        return out;
    }

    // 8. **Change Status Method**:
    //    - This method updates the status of an appointment by changing its value in the database.
    //    - It should be annotated with `@Transactional` to ensure the operation is executed in a single transaction.
    //    - Instruction: Add `@Transactional` before this method to ensure atomicity when updating appointment status.
    @Transactional
    public ResponseEntity<Map<String, String>> changeStatus(long id, int status) {
        Map<String, String> body = new HashMap<>();

        int updated = appointmentRepository.updateStatus(status, id);
        if (updated == 0) {
            body.put("message", "Appointment not found");
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        }
        body.put("message", "Status updated");
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    // ---- Internal validation helpers ----
    /**
     * Validates an appointment for:
     *  - Non-null time in the future
     *  - Doctor exists and is not double-booked for the hour window
     *  - Patient exists
     *  - Allows the same record when updating (ignore conflict with same id)
     * @param candidate the appointment to validate
     * @param currentId if updating, the existing id to ignore; otherwise null
     * @return null if OK, otherwise an error message
     */
    private String validateAppointment(Appointment candidate, Long currentId) {
        if (candidate.getAppointmentTime() == null) {
            return "Appointment time is required";
        }
        if (candidate.getAppointmentTime().isBefore(LocalDateTime.now())) {
            return "Appointment time must be in the future";
        }
        Long doctorId = candidate.getDoctor() != null ? candidate.getDoctor().getId() : null;
        Long patientId = candidate.getPatient() != null ? candidate.getPatient().getId() : null;
        if (doctorId == null || patientId == null) {
            return "Doctor and patient are required";
        }

        // Check overlap using a 1-hour slot window
        LocalDateTime slotStart = candidate.getAppointmentTime();
        LocalDateTime slotEnd = slotStart.plusHours(1);
        List<Appointment> conflicts =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, slotStart, slotEnd);

        boolean hasOtherConflict = conflicts.stream()
                .anyMatch(a -> !Objects.equals(a.getId(), currentId));

        if (hasOtherConflict) {
            return "Doctor is already booked for that time";
        }
        return null;
    }
}
