package com.project.back_end.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AppointmentDTO {

    // Fields
    private Long id;             // Unique identifier for the appointment
    private Long doctorId;       // ID of the doctor
    private String doctorName;   // Full name of the doctor
    private Long patientId;      // ID of the patient
    private String patientName;  // Full name of the patient
    private String patientEmail; // Email of the patient
    private String patientPhone; // Contact number of the patient
    private String patientAddress; // Residential address of the patient
    private LocalDateTime appointmentTime; // Date and time of the appointment
    private int status;          // Status of the appointment (e.g., scheduled, completed)

    // Derived fields
    private LocalDate appointmentDate;  // Extracted date from appointmentTime
    private LocalTime appointmentTimeOnly;  // Extracted time from appointmentTime
    private LocalDateTime endTime;     // Calculated end time (appointmentTime + 1 hour)

    // Constructor to initialize all core fields and calculate derived fields
    public AppointmentDTO(Long id, Long doctorId, String doctorName, Long patientId,
                          String patientName, String patientEmail, String patientPhone,
                          String patientAddress, LocalDateTime appointmentTime, int status) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.patientPhone = patientPhone;
        this.patientAddress = patientAddress;
        this.appointmentTime = appointmentTime;
        this.status = status;

        // Compute the derived fields
        this.appointmentDate = appointmentTime.toLocalDate();    // Extract date from appointmentTime
        this.appointmentTimeOnly = appointmentTime.toLocalTime(); // Extract time from appointmentTime
        this.endTime = appointmentTime.plusHours(1);              // End time is appointment time + 1 hour
    }

    // Getters for all fields
    public Long getId() {
        return id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public int getStatus() {
        return status;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getAppointmentTimeOnly() {
        return appointmentTimeOnly;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
