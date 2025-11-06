// Importing the API base URL from the configuration file
import { API_BASE_URL } from "../config/config.js";

// Define the doctor-related base API endpoint
const DOCTOR_API = API_BASE_URL + '/doctor';

// Function to get all doctors
export const getDoctors = async () => {
    try {
        const response = await fetch(DOCTOR_API); // Send a GET request to fetch doctors
        if (response.ok) {
            const data = await response.json(); // Parse the response JSON
            return data.doctors; // Return the list of doctors
        } else {
            console.error('Failed to fetch doctors');
            return []; // Return an empty list if the request fails
        }
    } catch (error) {
        console.error('Error fetching doctors:', error);
        return []; // Return an empty list in case of error
    }
};

// Function to delete a doctor
export const deleteDoctor = async (id, token) => {
    try {
        const response = await fetch(`${DOCTOR_API}/${id}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
        });

        if (response.ok) {
            const data = await response.json();
            return { success: true, message: data.message };
        } else {
            console.error('Failed to delete doctor');
            return { success: false, message: 'Failed to delete doctor' };
        }
    } catch (error) {
        console.error('Error deleting doctor:', error);
        return { success: false, message: 'An error occurred' };
    }
};

// Function to save (add) a new doctor
export const saveDoctor = async (doctor, token) => {
    try {
        const response = await fetch(DOCTOR_API, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
            body: JSON.stringify(doctor),
        });

        if (response.ok) {
            const data = await response.json();
            return { success: true, message: 'Doctor added successfully', doctor: data.doctor };
        } else {
            console.error('Failed to add doctor');
            return { success: false, message: 'Failed to add doctor' };
        }
    } catch (error) {
        console.error('Error adding doctor:', error);
        return { success: false, message: 'An error occurred' };
    }
};

// Function to filter doctors based on search criteria
export const filterDoctors = async (name, time, specialty) => {
    try {
        let query = '';
        if (name) query += `name=${name}&`;
        if (time) query += `time=${time}&`;
        if (specialty) query += `specialty=${specialty}`;

        const response = await fetch(`${DOCTOR_API}/filter?${query}`);
        if (response.ok) {
            const data = await response.json();
            return data.doctors;
        } else {
            console.error('Failed to filter doctors');
            return [];
        }
    } catch (error) {
        console.error('Error filtering doctors:', error);
        return [];
    }
};
