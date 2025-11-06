/*
  This script handles the admin dashboard functionality for managing doctors:
  - Loads all doctor cards
  - Filters doctors by name, time, or specialty
  - Adds a new doctor via modal form
*/

// Import required functions
import { openModal } from '../components/modals.js';
import { getDoctors, filterDoctors, saveDoctor } from './services/doctorServices.js';
import { createDoctorCard } from './components/doctorCard.js';

// Add event listener for the "Add Doctor" button
document.getElementById('addDocBtn').addEventListener('click', () => {
  openModal('addDoctor');
});

// Load doctor cards on page load
document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();
});

// Function to load doctor cards
function loadDoctorCards() {
  getDoctors()
    .then(doctors => {
      const contentDiv = document.getElementById("content");
      contentDiv.innerHTML = ""; // Clear existing content

      doctors.forEach(doctor => {
        const card = createDoctorCard(doctor);
        contentDiv.appendChild(card);
      });
    })
    .catch(error => {
      console.error("Failed to load doctors:", error);
    });
}

// Add event listeners to search bar and filter dropdowns
document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);

// Function to handle filtering doctors based on input fields
function filterDoctorsOnChange() {
  const searchBar = document.getElementById("searchBar").value.trim();
  const filterTime = document.getElementById("filterTime").value;
  const filterSpecialty = document.getElementById("filterSpecialty").value;

  const name = searchBar.length > 0 ? searchBar : null;
  const time = filterTime.length > 0 ? filterTime : null;
  const specialty = filterSpecialty.length > 0 ? filterSpecialty : null;

  filterDoctors(name, time, specialty)
    .then(response => {
      const doctors = response.doctors;
      const contentDiv = document.getElementById("content");
      contentDiv.innerHTML = ""; // Clear existing content

      if (doctors.length > 0) {
        doctors.forEach(doctor => {
          const card = createDoctorCard(doctor);
          contentDiv.appendChild(card);
        });
      } else {
        contentDiv.innerHTML = "<p>No doctors found with the given filters.</p>";
      }
    })
    .catch(error => {
      console.error("Failed to filter doctors:", error);
      alert("❌ An error occurred while filtering doctors.");
    });
}

// Function to add a new doctor
window.adminAddDoctor = async function () {
  try {
    const name = document.getElementById("doctorName").value;
    const email = document.getElementById("doctorEmail").value;
    const phone = document.getElementById("doctorPhone").value;
    const password = document.getElementById("doctorPassword").value;
    const specialty = document.getElementById("doctorSpecialty").value;
    const availableTimes = Array.from(document.querySelectorAll('input[name="availability"]:checked')).map(checkbox => checkbox.value);

    // Retrieve authentication token
    const token = localStorage.getItem("token");
    if (!token) {
      alert("❌ You must be logged in to add a doctor.");
      return;
    }

    // Doctor object to save
    const doctor = { name, email, phone, password, specialty, availableTimes };

    // Call the saveDoctor function to save the doctor
    const response = await saveDoctor(doctor, token);
    if (response.success) {
      alert("✅ Doctor added successfully!");
      openModal("close");
      loadDoctorCards(); // Refresh the list of doctors
    } else {
      alert("❌ Failed to add doctor: " + response.message);
    }
  } catch (error) {
    console.error("Error adding doctor:", error);
    alert("❌ An error occurred while adding the doctor.");
  }
};
