package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service; // central business/validation service
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    // 1. Set Up the Controller Class:
    //    - Annotate the class with `@RestController` to define it as a REST API controller.
    //    - Use `@RequestMapping("/appointments")` to set a base path for all appointment-related endpoints.
    //    - This centralizes all routes that deal with booking, updating, retrieving, and canceling appointments.

    private final AppointmentService appointmentService;
    private final Service service;

    // 2. Autowire Dependencies:
    //    - Inject `AppointmentService` for handling the business logic specific to appointments.
    //    - Inject the general `Service` class, which provides shared functionality like token validation and appointment checks.
    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    // 3. Define the `getAppointments` Method:
    //    - Handles HTTP GET requests to fetch appointments based on date and patient name.
    //    - Takes the appointment date, patient name, and token as path variables.
    //    - First validates the token for role `"doctor"` using the `Service`.
    //    - If the token is valid, returns appointments for the given patient on the specified date.
    //    - If the token is invalid or expired, responds with the appropriate message and status code.
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(@PathVariable String date,
                                                               @PathVariable String patientName,
                                                               @PathVariable String token) {
        // Validate token for doctor role
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            // Bubble up the same error structure/status
            Map<String, Object> body = new HashMap<>(validation.getBody() == null ? Map.of() : validation.getBody());
            return new ResponseEntity<>(body, validation.getStatusCode());
        }

        try {
            LocalDate day = LocalDate.parse(date); // expect ISO-8601 "yyyy-MM-dd"
            String pname = ("null".equalsIgnoreCase(patientName) || "-".equals(patientName)) ? "" : patientName;

            Map<String, Object> result = appointmentService.getAppointment(pname, day, token);
            HttpStatus status = result.containsKey("message") && "Not authorized".equalsIgnoreCase(String.valueOf(result.get("message")))
                    ? HttpStatus.FORBIDDEN : HttpStatus.OK;
            return new ResponseEntity<>(result, status);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "Invalid date format. Use yyyy-MM-dd.");
            return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
        }
    }

    // 4. Define the `bookAppointment` Method:
    //    - Handles HTTP POST requests to create a new appointment.
    //    - Accepts a validated `Appointment` object in the request body and a token as a path variable.
    //    - Validates the token for the `"patient"` role.
    //    - Uses service logic to validate the appointment data (e.g., check for doctor availability and time conflicts).
    //    - Returns success if booked, or appropriate error messages if the doctor ID is invalid or the slot is already taken.
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(@PathVariable String token,
                                                               @RequestBody Appointment appointment) {
        // Validate token for patient role
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        // Validate appointment slot/doctor existence
        int valid = service.validateAppointment(appointment);
        Map<String, String> body = new HashMap<>();
        if (valid == -1) {
            body.put("message", "Doctor not found");
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        } else if (valid == 0) {
            body.put("message", "Requested time is unavailable");
            return new ResponseEntity<>(body, HttpStatus.CONFLICT);
        }

        int saved = appointmentService.bookAppointment(appointment);
        if (saved == 1) {
            body.put("message", "Appointment booked");
            return new ResponseEntity<>(body, HttpStatus.CREATED);
        } else {
            body.put("message", "Failed to book appointment");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 5. Define the `updateAppointment` Method:
    //    - Handles HTTP PUT requests to modify an existing appointment.
    //    - Accepts a validated `Appointment` object and a token as input.
    //    - Validates the token for `"patient"` role.
    //    - Delegates the update logic to the `AppointmentService`.
    //    - Returns an appropriate success or failure response based on the update result.
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(@PathVariable String token,
                                                                 @RequestBody Appointment appointment) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }
        // AppointmentService already returns a ResponseEntity with proper status/messages
        return appointmentService.updateAppointment(appointment);
    }

    // 6. Define the `cancelAppointment` Method:
    //    - Handles HTTP DELETE requests to cancel a specific appointment.
    //    - Accepts the appointment ID and a token as path variables.
    //    - Validates the token for `"patient"` role to ensure the user is authorized to cancel the appointment.
    //    - Calls `AppointmentService` to handle the cancellation process and returns the result.
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(@PathVariable long id,
                                                                 @PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }
        return appointmentService.cancelAppointment(id, token);
    }
}
