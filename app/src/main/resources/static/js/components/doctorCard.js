export function createDoctorCard(doctor) {
    const card = document.createElement("div");
    card.classList.add("doctor-card");

    const role = localStorage.getItem("userRole");

    // Create doctor info section
    const infoDiv = document.createElement("div");
    infoDiv.classList.add("doctor-info");

    const name = document.createElement("h3");
    name.textContent = doctor.name;
    const specialization = document.createElement("p");
    specialization.textContent = `Specialization: ${doctor.specialization}`;
    const email = document.createElement("p");
    email.textContent = `Email: ${doctor.email}`;
    const availability = document.createElement("p");
    availability.textContent = `Availability: ${doctor.availability.join(", ")}`;

    infoDiv.appendChild(name);
    infoDiv.appendChild(specialization);
    infoDiv.appendChild(email);
    infoDiv.appendChild(availability);

    // Create actions section
    const actionsDiv = document.createElement("div");
    actionsDiv.classList.add("card-actions");

    if (role === "admin") {
        const removeBtn = document.createElement("button");
        removeBtn.textContent = "Delete";
        removeBtn.addEventListener("click", async () => {
            const token = localStorage.getItem("token");
            const result = await deleteDoctor(doctor.id, token);
            if (result.success) {
                card.remove();
            }
        });
        actionsDiv.appendChild(removeBtn);
    } else if (role === "patient") {
        const bookNow = document.createElement("button");
        bookNow.textContent = "Book Now";
        bookNow.addEventListener("click", () => {
            alert("Please log in to book an appointment.");
        });
        actionsDiv.appendChild(bookNow);
    } else if (role === "loggedPatient") {
        const bookNow = document.createElement("button");
        bookNow.textContent = "Book Now";
        bookNow.addEventListener("click", async () => {
            const token = localStorage.getItem("token");
            if (!token) {
                alert("Please log in first.");
                return;
            }
            const patientData = await getPatientData(token);
            showBookingOverlay(doctor, patientData);
        });
        actionsDiv.appendChild(bookNow);
    }

    card.appendChild(infoDiv);
    card.appendChild(actionsDiv);

    return card;
}
