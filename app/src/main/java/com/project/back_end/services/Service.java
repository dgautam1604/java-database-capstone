package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class Service {
// 1. **@Service Annotation**
// The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
// and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    // 2. **Constructor Injection for Dependencies**
// The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
// and ensures that all required dependencies are provided at object creation time.
    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    // 3. **validateToken Method**
// This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
// If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
// unauthorized access to protected resources.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> body = new HashMap<>();
        try {
            boolean valid = tokenService.validateToken(token);
            if (!valid) {
                body.put("message", "Invalid or expired token");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }
            // Optional subject match if you encode email/username in token:
            String subject = tokenService.extractSubject(token); // e.g., email or username
            if (user != null && subject != null && !subject.equalsIgnoreCase(user)) {
                body.put("message", "Token subject mismatch");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }
            body.put("message", "Token valid");
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Token validation error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 4. **validateAdmin Method**
// This method validates the login credentials for an admin user.
// - It first searches the admin repository using the provided username.
// - If an admin is found, it checks if the password matches.
// - If the password is correct, it generates and returns a JWT token (using the admin’s username) with a 200 OK status.
// - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
// - If no admin is found, it also returns a 401 Unauthorized.
// - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
// This method ensures that only valid admin users can access secured parts of the system.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> body = new HashMap<>();
        try {
            if (receivedAdmin == null || receivedAdmin.getUsername() == null || receivedAdmin.getPassword() == null) {
                body.put("message", "Username and password required");
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
            }
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null || !Objects.equals(admin.getPassword(), receivedAdmin.getPassword())) {
                body.put("message", "Invalid credentials");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }
            String token = tokenService.generateToken(admin.getUsername(), "ADMIN");
            body.put("token", token);
            body.put("message", "Login successful");
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Internal error during admin validation");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 5. **filterDoctor Method**
// This method provides filtering functionality for doctors based on name, specialty, and available time slots.
// - It supports various combinations of the three filters.
// - If none of the filters are provided, it returns all available doctors.
// This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        // Normalize inputs
        String nm = name == null ? "" : name.trim();
        String sp = specialty == null ? "" : specialty.trim();
        String tm = time == null ? "" : time.trim();

        if (!nm.isEmpty() && !sp.isEmpty() && !tm.isEmpty()) {
            return doctorService.filterDoctorsByNameSpecilityandTime(nm, sp, tm);
        } else if (!nm.isEmpty() && !sp.isEmpty()) {
            return doctorService.filterDoctorByNameAndSpecility(nm, sp);
        } else if (!nm.isEmpty() && !tm.isEmpty()) {
            return doctorService.filterDoctorByNameAndTime(nm, tm);
        } else if (!sp.isEmpty() && !tm.isEmpty()) {
            return doctorService.filterDoctorByTimeAndSpecility(sp, tm);
        } else if (!nm.isEmpty()) {
            return doctorService.findDoctorByName(nm);
        } else if (!sp.isEmpty()) {
            return doctorService.filterDoctorBySpecility(sp);
        } else if (!tm.isEmpty()) {
            return doctorService.filterDoctorsByTime(tm);
        } else {
            Map<String, Object> out = new HashMap<>();
            out.put("doctors", doctorService.getDoctors());
            return out;
        }
    }

    // 6. **validateAppointment Method**
// This method validates if the requested appointment time for a doctor is available.
// - It first checks if the doctor exists in the repository.
// - Then, it retrieves the list of available time slots for the doctor on the specified date.
// - It compares the requested appointment time with the start times of these slots.
// - If a match is found, it returns 1 (valid appointment time).
// - If no matching time slot is found, it returns 0 (invalid).
// - If the doctor doesn’t exist, it returns -1.
// This logic prevents overlapping or invalid appointment bookings.
    @Transactional(readOnly = true)
    public int validateAppointment(Appointment appointment) {
        try {
            if (appointment == null || appointment.getDoctor() == null || appointment.getDoctor().getId() == null ||
                    appointment.getAppointmentTime() == null) {
                return 0;
            }
            Long doctorId = appointment.getDoctor().getId();
            Optional<Doctor> docOpt = doctorRepository.findById(doctorId);
            if (docOpt.isEmpty()) return -1;

            LocalTime requested = appointment.getAppointmentTime().toLocalTime().withSecond(0).withNano(0);
            LocalDate date = appointment.getAppointmentTime().toLocalDate();
            List<String> available = doctorService.getDoctorAvailability(doctorId, date);

            // Parse available slots like "09:00" (and tolerant to "9:00", "10:30 AM" strings).
            boolean match = available.stream().anyMatch(s -> {
                Optional<LocalTime> parsed = tryParseTime(s);
                if (parsed.isPresent()) {
                    return parsed.get().withSecond(0).withNano(0).equals(requested.withMinute(parsed.get().getMinute()));
                }
                // If non-parseable text with AM/PM, fallback contains check:
                String up = s.toUpperCase(Locale.ROOT);
                String r = toAmPm(requested);
                return up.contains(r);
            });

            return match ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // 7. **validatePatient Method**
// This method checks whether a patient with the same email or phone number already exists in the system.
// - If a match is found, it returns false (indicating the patient is not valid for new registration).
// - If no match is found, it returns true.
// This helps enforce uniqueness constraints on patient records and prevent duplicate entries.
    @Transactional(readOnly = true)
    public boolean validatePatient(Patient patient) {
        if (patient == null) return false;
        String email = patient.getEmail();
        String phone = patient.getPhone();
        Patient existing = patientRepository.findByEmailOrPhone(email, phone);
        return existing == null;
    }

    // 8. **validatePatientLogin Method**
// This method handles login validation for patient users.
// - It looks up the patient by email.
// - If found, it checks whether the provided password matches the stored one.
// - On successful validation, it generates a JWT token and returns it with a 200 OK status.
// - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
// - If an exception occurs, it returns a 500 Internal Server Error.
// This method ensures only legitimate patients can log in and access their data securely.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> body = new HashMap<>();
        try {
            if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
                body.put("message", "Email and password required");
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
            }
            Patient patient = patientRepository.findByEmail(login.getIdentifier());
            if (patient == null || !Objects.equals(patient.getPassword(), login.getPassword())) {
                body.put("message", "Invalid credentials");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }
            String token = tokenService.generateToken(patient.getEmail(), "PATIENT");
            body.put("token", token);
            body.put("message", "Login successful");
            return new ResponseEntity<>(body, HttpStatus.OK);
        } catch (Exception e) {
            body.put("message", "Internal error during login");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 9. **filterPatient Method**
// This method filters a patient's appointment history based on condition and doctor name.
// - It extracts the email from the JWT token to identify the patient.
// - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
// - If no filters are provided, it retrieves all appointments for the patient.
// This flexible method supports patient-specific querying and enhances user experience on the client side.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        try {
            String email = tokenService.extractEmail(token);
            if (email == null) {
                Map<String, Object> body = new HashMap<>();
                body.put("message", "Invalid token");
                return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
            }
            Patient p = patientRepository.findByEmail(email);
            if (p == null) {
                Map<String, Object> body = new HashMap<>();
                body.put("message", "Patient not found");
                return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
            }

            boolean hasCondition = condition != null && !condition.isBlank();
            boolean hasName = name != null && !name.isBlank();

            if (hasCondition && hasName) {
                return patientService.filterByDoctorAndCondition(condition, name, p.getId());
            } else if (hasCondition) {
                return patientService.filterByCondition(condition, p.getId());
            } else if (hasName) {
                return patientService.filterByDoctor(name, p.getId());
            } else {
                return patientService.getPatientAppointment(p.getId(), token);
            }
        } catch (Exception e) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Internal error");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ---- helpers ----
    private Optional<LocalTime> tryParseTime(String raw) {
        if (raw == null) return Optional.empty();
        String v = raw.trim();
        try {
            // Accept "H:mm" or "HH:mm"
            if (v.matches("\\d{1,2}:\\d{2}")) {
                String norm = v.length() == 4 ? "0" + v : v; // "9:00" -> "09:00"
                return Optional.of(LocalTime.parse(norm));
            }
        } catch (Exception ignored) {}
        // You can extend this to parse "h:mm a" if you later store AM/PM strings.
        return Optional.empty();
    }

    private String toAmPm(LocalTime t) {
        int hour = t.getHour();
        int display = (hour % 12 == 0) ? 12 : (hour % 12);
        String minute = String.format("%02d", t.getMinute());
        String suffix = hour < 12 ? "AM" : "PM";
        return (display + ":" + minute + " " + suffix).toUpperCase(Locale.ROOT);
    }
}
