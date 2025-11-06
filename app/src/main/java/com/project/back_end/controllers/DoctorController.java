package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service; // shared validation/filtering service
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.path}doctor")
public class DoctorController {

    // 1. Set Up the Controller Class:
    //    - Annotate the class with `@RestController` to define it as a REST controller that serves JSON responses.
    //    - Use `@RequestMapping("${api.path}doctor")` to prefix all endpoints with a configurable API path followed by "doctor".
    //    - This class manages doctor-related functionalities such as registration, login, updates, and availability.

    private final DoctorService doctorService;
    private final Service service;

    // 2. Autowire Dependencies:
    //    - Inject `DoctorService` for handling the core logic related to doctors (e.g., CRUD operations, authentication).
    //    - Inject the shared `Service` class for general-purpose features like token validation and filtering.
    public DoctorController(DoctorService doctorService, Service service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    // 3. Define the `getDoctorAvailability` Method:
    //    - Handles HTTP GET requests to check a specific doctor’s availability on a given date.
    //    - Requires `user` type, `doctorId`, `date`, and `token` as path variables.
    //    - First validates the token against the user type.
    //    - If the token is invalid, returns an error response; otherwise, returns the availability status for the doctor.
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String, Object>> getDoctorAvailability(@PathVariable String user,
                                                                     @PathVariable Long doctorId,
                                                                     @PathVariable String date,
                                                                     @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, user);
        if (!validation.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = new HashMap<>(validation.getBody() == null ? Map.of() : validation.getBody());
            return new ResponseEntity<>(body, validation.getStatusCode());
        }

        Map<String, Object> out = new HashMap<>();
        try {
            LocalDate d = LocalDate.parse(date); // expects yyyy-MM-dd
            List<String> slots = doctorService.getDoctorAvailability(doctorId, d);
            out.put("availability", slots);
            return new ResponseEntity<>(out, HttpStatus.OK);
        } catch (Exception e) {
            out.put("message", "Invalid date format. Use yyyy-MM-dd.");
            return new ResponseEntity<>(out, HttpStatus.BAD_REQUEST);
        }
    }

    // 4. Define the `getDoctor` Method:
    //    - Handles HTTP GET requests to retrieve a list of all doctors.
    //    - Returns the list within a response map under the key `"doctors"` with HTTP 200 OK status.
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoctor() {
        Map<String, Object> out = new HashMap<>();
        out.put("doctors", doctorService.getDoctors());
        return new ResponseEntity<>(out, HttpStatus.OK);
    }

    // 5. Define the `saveDoctor` Method:
    //    - Handles HTTP POST requests to register a new doctor.
    //    - Accepts a validated `Doctor` object in the request body and a token for authorization.
    //    - Validates the token for the `"admin"` role before proceeding.
    //    - If the doctor already exists, returns a conflict response; otherwise, adds the doctor and returns a success message.
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> saveDoctor(@PathVariable String token,
                                                          @RequestBody Doctor doctor) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        Map<String, String> body = new HashMap<>();
        int res = doctorService.saveDoctor(doctor);
        if (res == 1) {
            body.put("message", "Doctor added to db");
            return new ResponseEntity<>(body, HttpStatus.CREATED);
        } else if (res == -1) {
            body.put("message", "Doctor already exists");
            return new ResponseEntity<>(body, HttpStatus.CONFLICT);
        } else {
            body.put("message", "Some internal error occurred");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 6. Define the `doctorLogin` Method:
    //    - Handles HTTP POST requests for doctor login.
    //    - Accepts a validated `Login` DTO containing credentials.
    //    - Delegates authentication to the `DoctorService` and returns login status and token information.
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@RequestBody Login login) {
        return doctorService.validateDoctor(login);
    }

    // 7. Define the `updateDoctor` Method:
    //    - Handles HTTP PUT requests to update an existing doctor's information.
    //    - Accepts a validated `Doctor` object and a token for authorization.
    //    - Token must belong to an `"admin"`.
    //    - If the doctor exists, updates the record and returns success; otherwise, returns not found or error messages.
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(@PathVariable String token,
                                                            @RequestBody Doctor doctor) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        Map<String, String> body = new HashMap<>();
        int res = doctorService.updateDoctor(doctor);
        if (res == 1) {
            body.put("message", "Doctor updated");
            return new ResponseEntity<>(body, HttpStatus.OK);
        } else if (res == -1) {
            body.put("message", "Doctor not found");
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        } else {
            body.put("message", "Some internal error occurred");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 8. Define the `deleteDoctor` Method:
    //    - Handles HTTP DELETE requests to remove a doctor by ID.
    //    - Requires both doctor ID and an admin token as path variables.
    //    - If the doctor exists, deletes the record and returns a success message; otherwise, responds with a not found or error message.
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(@PathVariable long id,
                                                            @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        Map<String, String> body = new HashMap<>();
        int res = doctorService.deleteDoctor(id);
        if (res == 1) {
            body.put("message", "Doctor deleted successfully");
            return new ResponseEntity<>(body, HttpStatus.OK);
        } else if (res == -1) {
            body.put("message", "Doctor not found with id");
            return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
        } else {
            body.put("message", "Some internal error occurred");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 9. Define the `filter` Method:
    //    - Handles HTTP GET requests to filter doctors based on name, time, and specialty.
    //    - Accepts `name`, `time`, and `speciality` as path variables.
    //    - Calls the shared `Service` to perform filtering logic and returns matching doctors in the response.
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, Object>> filter(@PathVariable String name,
                                                      @PathVariable String time,
                                                      @PathVariable String speciality) {
        Map<String, Object> result = service.filterDoctor(name, speciality, time);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
