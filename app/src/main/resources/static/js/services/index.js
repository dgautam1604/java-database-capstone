// Importing necessary modules
import { openModal } from '../components/modals.js';  // Modal handling function
import { API_BASE_URL } from '../config/config.js';    // Base URL for API

// Define API Endpoints
const ADMIN_API = API_BASE_URL + '/admin';
const DOCTOR_API = API_BASE_URL + '/doctor/login';

// Setup button event listeners after the window is loaded
window.onload = function () {
    // Select buttons by their ID attributes
    const adminBtn = document.getElementById('adminLogin');
    const doctorBtn = document.getElementById('doctorLogin');

    // If the admin login button exists, attach a click event to show the admin login modal
    if (adminBtn) {
        adminBtn.addEventListener('click', () => {
            openModal('adminLogin');  // Open the admin login modal
        });
    }

    // If the doctor login button exists, attach a click event to show the doctor login modal
    if (doctorBtn) {
        doctorBtn.addEventListener('click', () => {
            openModal('doctorLogin');  // Open the doctor login modal
        });
    }
};

// Admin Login Handler - Asynchronous function to handle Admin login logic
window.adminLoginHandler = async function () {
    // Get the values entered for username and password
    const username = document.getElementById('adminUsername').value;
    const password = document.getElementById('adminPassword').value;

    // Create an admin object with the provided credentials
    const admin = { username, password };

    try {
        // Sending POST request to the Admin API
        const response = await fetch(ADMIN_API, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(admin),
        });

        // Check if the login was successful
        if (response.ok) {
            // Parse the response to get the token
            const data = await response.json();

            // Store the token in localStorage
            localStorage.setItem('token', data.token);

            // Call selectRole function to save the selected role in localStorage and render the appropriate page
            selectRole('admin');
        } else {
            // If login fails, show an alert with an error message
            alert('Invalid credentials!');
        }
    } catch (error) {
        // Handle any unexpected issues or network errors
        console.error('Error during admin login:', error);
        alert('Something went wrong! Please try again later.');
    }
};

// Doctor Login Handler - Asynchronous function to handle Doctor login logic
window.doctorLoginHandler = async function () {
    // Get the values entered for email and password
    const email = document.getElementById('doctorEmail').value;
    const password = document.getElementById('doctorPassword').value;

    // Create a doctor object with the provided credentials
    const doctor = { email, password };

    try {
        // Sending POST request to the Doctor API
        const response = await fetch(DOCTOR_API, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(doctor),
        });

        // Check if the login was successful
        if (response.ok) {
            // Parse the response to get the token
            const data = await response.json();

            // Store the token in localStorage
            localStorage.setItem('token', data.token);

            // Call selectRole function to save the selected role in localStorage and render the appropriate page
            selectRole('doctor');
        } else {
            // If login fails, show an alert with an error message
            alert('Invalid credentials!');
        }
    } catch (error) {
        // Handle any unexpected issues or network errors
        console.error('Error during doctor login:', error);
        alert('Something went wrong! Please try again later.');
    }
};

// The selectRole function is assumed to be in another script like render.js to handle page rendering
function selectRole(role) {
    // Store the selected role in localStorage
    localStorage.setItem('role', role);

    // Render the appropriate page or redirect based on the role
    if (role === 'admin') {
        window.location.href = '/admin-dashboard'; // Navigate to admin dashboard
    } else if (role === 'doctor') {
        window.location.href = '/doctor-dashboard'; // Navigate to doctor dashboard
    }
}
