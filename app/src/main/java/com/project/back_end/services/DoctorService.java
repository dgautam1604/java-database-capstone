package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service; // @Service is here
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    // 1. **Add @Service Annotation**:
    //    - This class should be annotated with `@Service` to indicate that it is a service layer class.
    //    - The `@Service` annotation marks this class as a Spring-managed bean for business logic.
    //    - Instruction: Add `@Service` above the class declaration.

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    // 2. **Constructor Injection for Dependencies**:
    //    - The `DoctorService` class depends on `DoctorRepository`, `AppointmentRepository`, and `TokenService`.
    //    - These dependencies should be injected via the constructor for proper dependency management.
    //    - Instruction: Ensure constructor injection is used for injecting dependencies into the service.
    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 3. **Add @Transactional Annotation for Methods that Modify or Fetch Database Data**:
    //    - Methods like `getDoctorAvailability`, `getDoctors`, `findDoctorByName`, `filterDoctorsBy*` should be annotated with `@Transactional`.
    //    - The `@Transactional` annotation ensures that database operations are consistent and wrapped in a single transaction.
    //    - Instruction: Add the `@Transactional` annotation above the methods that perform database operations or queries.

    // 4. **getDoctorAvailability Method**:
    //    - Retrieves the available time slots for a specific doctor on a particular date and filters out already booked slots.
    //    - The method fetches all appointments for the doctor on the given date and calculates the availability by comparing against booked slots.
    //    - Instruction: Ensure that the time slots are properly formatted and the available slots are correctly filtered.
    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> docOpt = doctorRepository.findById(doctorId);
        if (docOpt.isEmpty()) return Collections.emptyList();

        Doctor doctor = docOpt.get();
        List<String> definedSlots = doctor.getAvailableTimes() == null
                ? Collections.emptyList()
                : doctor.getAvailableTimes();

        // Appointments on that day
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<Appointment> dayAppts =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);

        // Booked times (we assume each appointment blocks the start time's hour)
        Set<LocalTime> booked = dayAppts.stream()
                .map(a -> a.getAppointmentTime().toLocalTime().withMinute(0).withSecond(0).withNano(0))
                .collect(Collectors.toSet());

        // Filter out booked times. We accept slots either "HH:mm" or strings containing "AM"/"PM".
        return definedSlots.stream()
                .filter(s -> {
                    Optional<LocalTime> parsed = tryParseTime(s);
                    if (parsed.isEmpty()) {
                        // If format is not HH:mm, allow unless an exact textual clash is found later
                        return true;
                    }
                    LocalTime slot = parsed.get().withMinute(0).withSecond(0).withNano(0);
                    return !booked.contains(slot);
                })
                .sorted(Comparator.comparing(s -> tryParseTime(s).orElse(LocalTime.MAX)))
                .collect(Collectors.toList());
    }

    // 5. **saveDoctor Method**:
    //    - Used to save a new doctor record in the database after checking if a doctor with the same email already exists.
    //    - If a doctor with the same email is found, it returns `-1` to indicate conflict; `1` for success, and `0` for internal errors.
    //    - Instruction: Ensure that the method correctly handles conflicts and exceptions when saving a doctor.
    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getEmail() == null) return 0;
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
                return -1; // conflict: email already exists
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // 6. **updateDoctor Method**:
    //    - Updates an existing doctor's details in the database. If the doctor doesn't exist, it returns `-1`.
    //    - Instruction: Make sure that the doctor exists before attempting to save the updated record and handle any errors properly.
    @Transactional
    public int updateDoctor(Doctor doctor) {
        if (doctor == null || doctor.getId() == null) return 0;
        try {
            return doctorRepository.findById(doctor.getId())
                    .map(existing -> {
                        existing.setName(doctor.getName());
                        existing.setSpecialty(doctor.getSpecialty());
                        existing.setEmail(doctor.getEmail());
                        existing.setPassword(doctor.getPassword()); // NOTE: hash in real impl
                        existing.setPhone(doctor.getPhone());
                        existing.setAvailableTimes(doctor.getAvailableTimes());
                        doctorRepository.save(existing);
                        return 1;
                    })
                    .orElse(-1);
        } catch (Exception e) {
            return 0;
        }
    }

    // 7. **getDoctors Method**:
    //    - Fetches all doctors from the database. It is marked with `@Transactional` to ensure that the collection is properly loaded.
    //    - Instruction: Ensure that the collection is eagerly loaded, especially if dealing with lazy-loaded relationships (e.g., available times).
    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    // 8. **deleteDoctor Method**:
    //    - Deletes a doctor from the system along with all appointments associated with that doctor.
    //    - It first checks if the doctor exists. If not, it returns `-1`; otherwise, it deletes the doctor and their appointments.
    //    - Instruction: Ensure the doctor and their appointments are deleted properly, with error handling for internal issues.
    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (!doctorRepository.existsById(id)) return -1;
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // 9. **validateDoctor Method**:
    //    - Validates a doctor's login by checking if the email and password match an existing doctor record.
    //    - It generates a token for the doctor if the login is successful, otherwise returns an error message.
    //    - Instruction: Make sure to handle invalid login attempts and password mismatches properly with error responses.
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> body = new HashMap<>();
        if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
            body.put("message", "Email and password are required");
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }

        Doctor doctor = doctorRepository.findByEmail(login.getIdentifier());
        if (doctor == null) {
            body.put("message", "Invalid credentials");
            return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
        }

        // NOTE: Replace with BCrypt password check in production.
        if (!Objects.equals(doctor.getPassword(), login.getPassword())) {
            body.put("message", "Invalid credentials");
            return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
        }

        String token = tokenService.generateTokenForDoctor(doctor); // e.g., embeds id + role
        body.put("token", token);
        body.put("message", "Login successful");
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    // 10. **findDoctorByName Method**:
    //    - Finds doctors based on partial name matching and returns the list of doctors with their available times.
    //    - This method is annotated with `@Transactional` to ensure that the database query and data retrieval are properly managed within a transaction.
    //    - Instruction: Ensure that available times are eagerly loaded for the doctors.
    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name == null ? "" : name);
        Map<String, Object> out = new HashMap<>();
        out.put("doctors", doctors);
        return out;
    }

    // 11. **filterDoctorsByNameSpecilityandTime Method**:
    //    - Filters doctors based on their name, specialty, and availability during a specific time (AM/PM).
    //    - The method fetches doctors matching the name and specialty criteria, then filters them based on their availability during the specified time period.
    //    - Instruction: Ensure proper filtering based on both the name and specialty as well as the specified time period.
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        List<Doctor> base = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                name == null ? "" : name,
                specialty == null ? "" : specialty
        );
        List<Doctor> filtered = filterDoctorByTime(base, amOrPm);
        Map<String, Object> out = new HashMap<>();
        out.put("doctors", filtered);
        return out;
    }

    // 12. **filterDoctorByTime Method**:
    //    - Filters a list of doctors based on whether their available times match the specified time period (AM/PM).
    //    - This method processes a list of doctors and their available times to return those that fit the time criteria.
    //    - Instruction: Ensure that the time filtering logic correctly handles both AM and PM time slots and edge cases.
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (amOrPm == null || amOrPm.isBlank()) return doctors;
        String period = amOrPm.trim().toUpperCase(Locale.ROOT);

        return doctors.stream()
                .filter(d -> {
                    List<String> times = d.getAvailableTimes();
                    if (times == null || times.isEmpty()) return false;
                    boolean match = false;
                    for (String t : times) {
                        // Accept both "HH:mm" and "h:mm AM/PM" styles
                        Optional<LocalTime> parsed = tryParseTime(t);
                        if (parsed.isPresent()) {
                            boolean isAM = parsed.get().getHour() < 12;
                            if ((isAM && "AM".equals(period)) || (!isAM && "PM".equals(period))) {
                                match = true; break;
                            }
                        } else {
                            String upper = t.toUpperCase(Locale.ROOT);
                            if ((upper.contains("AM") && "AM".equals(period)) ||
                                    (upper.contains("PM") && "PM".equals(period))) {
                                match = true; break;
                            }
                        }
                    }
                    return match;
                })
                .collect(Collectors.toList());
    }

    // 13. **filterDoctorByNameAndTime Method**:
    //    - Filters doctors based on their name and the specified time period (AM/PM).
    //    - Fetches doctors based on partial name matching and filters the results to include only those available during the specified time period.
    //    - Instruction: Ensure that the method correctly filters doctors based on the given name and time of day (AM/PM).
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> base = doctorRepository.findByNameLike(name == null ? "" : name);
        List<Doctor> filtered = filterDoctorByTime(base, amOrPm);
        Map<String, Object> out = new HashMap<>();
        out.put("doctors", filtered);
        return out;
    }

    // 14. **filterDoctorByNameAndSpecility Method**:
    //    - Filters doctors by name and specialty.
    //    - It ensures that the resulting list of doctors matches both the name (case-insensitive) and the specified specialty.
    //    - Instruction: Ensure that both name and specialty are considered when filtering doctors.
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
        List<Doctor> filtered = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                name == null ? "" : name, specilty == null ? "" : specilty
        );
        Map<String, Object> out = new HashMap<>();
        out.put("doctors", filtered);
        return out;
    }

    // 15. **filterDoctorByTimeAndSpecility Method**:
    //    - Filters doctors based on their specialty and availability during a specific time period (AM/PM).
    //    - Fetches doctors based on the specified specialty and filters them based on their available time slots for AM/PM.
    //    - Instruction: Ensure the time filtering is accurately applied based on the given specialty and time period (AM/PM).
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
        List<Doctor> base = doctorRepository.findBySpecialtyIgnoreCase(specilty == null ? "" : specilty);
        List<Doctor> filtered = filterDoctorByTime(base, amOrPm);
        Map<String, Object> out = new HashMap<>();
        out.put("doctors", filtered);
        return out;
    }

    // 16. **filterDoctorBySpecility Method**:
    //    - Filters doctors based on their specialty.
    //    - This method fetches all doctors matching the specified specialty and returns them.
    //    - Instruction: Make sure the filtering logic works for case-insensitive specialty matching.
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specilty) {
        List<Doctor> filtered = doctorRepository.findBySpecialtyIgnoreCase(specilty == null ? "" : specilty);
        Map<String, Object> out = new HashMap<>();
        out.put("doctors", filtered);
        return out;
    }

    // 17. **filterDoctorsByTime Method**:
    //    - Filters all doctors based on their availability during a specific time period (AM/PM).
    //    - The method checks all doctors' available times and returns those available during the specified time period.
    //    - Instruction: Ensure proper filtering logic to handle AM/PM time periods.
    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        List<Doctor> all = doctorRepository.findAll();
        List<Doctor> filtered = filterDoctorByTime(all, amOrPm);
        Map<String, Object> out = new HashMap<>();
        out.put("doctors", filtered);
        return out;
    }

    // ---- helpers ----

    private Optional<LocalTime> tryParseTime(String raw) {
        if (raw == null) return Optional.empty();
        String v = raw.trim();
        try {
            // Try "HH:mm"
            if (v.matches("\\d{1,2}:\\d{2}")) {
                return Optional.of(LocalTime.parse(
                        v.length() == 4 ? "0" + v : v)); // normalize "9:00" -> "09:00"
            }
        } catch (Exception ignored) { }
        // Could add more parsers (e.g., "h:mm a") if you start storing times with AM/PM.
        return Optional.empty();
    }
}
