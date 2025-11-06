/*
  This script handles the doctor dashboard functionality for managing appointments:
  - Displays today's appointments
  - Allows filtering by patient name or date
  - Handles error cases and no appointments found
*/

// Import required functions
import { getAllAppointments } from './services/appointmentRecordService.js';
import { createPatientRow } from './components/patientRows.js';

// Initialize global variables
const patientTableBody = document.getElementById("patientTableBody");
let selectedDate = new Date().toISOString().split('T')[0]; // Today's date
let token = localStorage.getItem("token");
let patientName = null;

// Load appointments on page load
document.addEventListener("DOMContentLoaded", () => {
  loadAppointments();
});

// Handle patient search input
document.getElementById("searchBar").addEventListener("input", filterAppointments);

// Handle "Today" button click
document.getElementById("todayButton").addEventListener("click", () => {
  selectedDate = new Date().toISOString().split('T')[0];
  document.getElementById("datePicker").value = selectedDate; // Update date picker
  loadAppointments();
});

// Handle date picker change
document.getElementById("datePicker").addEventListener("change", (event) => {
  selectedDate = event.target.value;
  loadAppointments();
});

// Function to load appointments based on selected date and patient name filter
function loadAppointments() {
  getAllAppointments(selectedDate, patientName, token)
    .then(appointments => {
      patientTableBody.innerHTML = ""; // Clear previous rows

      if (appointments.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `<td colspan="5">No appointments found for ${selectedDate}.</td>`;
        patientTableBody.appendChild(row);
      } else {
        appointments.forEach(appointment => {
          const row = createPatientRow(appointment);
          patientTableBody.appendChild(row);
        });
      }
    })
    .catch(error => {
      console.error("Error loading appointments:", error);
      const row = document.createElement('tr');
      row.innerHTML = `<td colspan="5">Error loading appointments. Please try again later.</td>`;
      patientTableBody.appendChild(row);
    });
}
