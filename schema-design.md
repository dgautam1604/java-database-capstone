## MySQL Database Design
### Table: patients
- **id**: INT, PK, AUTO_INCREMENT
- **first_name**: VARCHAR(100), NOT NULL
- **last_name**: VARCHAR(100), NOT NULL
- **dob**: DATE, NOT NULL
- **email**: VARCHAR(255), UNIQUE, NULL
- **phone**: VARCHAR(32), NULL
- **created_at**: DATETIME, NOT NULL
- **updated_at**: DATETIME, NULL
- _Delete policy_: keep record; do not cascade to appointments.

### Table: doctors
- **id**: INT, PK, AUTO_INCREMENT
- **first_name**: VARCHAR(100), NOT NULL
- **last_name**: VARCHAR(100), NOT NULL
- **email**: VARCHAR(255), UNIQUE, NOT NULL
- **specialty**: VARCHAR(120), NULL
- **phone**: VARCHAR(32), NULL
- **created_at**: DATETIME, NOT NULL
- **updated_at**: DATETIME, NULL

### Table: admin
- **id**: INT, PK, AUTO_INCREMENT
- **email**: VARCHAR(255), UNIQUE, NOT NULL
- **password_hash**: VARCHAR(255), NOT NULL
- **role**: ENUM('admin','staff','billing'), NOT NULL DEFAULT 'staff'
- **created_at**: DATETIME, NOT NULL
- **updated_at**: DATETIME, NULL

### Table: clinic_locations
- **id**: INT, PK, AUTO_INCREMENT
- **name**: VARCHAR(150), NOT NULL
- **address_line1**: VARCHAR(255), NOT NULL
- **address_line2**: VARCHAR(255), NULL
- **city**: VARCHAR(100), NOT NULL
- **state_province**: VARCHAR(100), NOT NULL
- **postal_code**: VARCHAR(20), NOT NULL
- **country_code**: CHAR(2), NOT NULL
- **timezone**: VARCHAR(64), NOT NULL
- **phone**: VARCHAR(32), NULL
- **created_at**: DATETIME, NOT NULL
- **updated_at**: DATETIME, NULL

### Table: appointments
- **id**: INT, PK, AUTO_INCREMENT
- **doctor_id**: INT, FK → doctors(id), NOT NULL
- **patient_id**: INT, FK → patients(id), NOT NULL
- **clinic_id**: INT, FK → clinic_locations(id), NOT NULL
- **appointment_time_start**: DATETIME, NOT NULL
- **appointment_time_end**: DATETIME, NOT NULL
- **status**: ENUM('scheduled','completed','cancelled','no_show'), NOT NULL DEFAULT 'scheduled'
- **visit_type**: ENUM('in_person','telehealth'), NOT NULL DEFAULT 'in_person'
- **reason_for_visit**: VARCHAR(255), NULL
- **created_by_admin_id**: INT, FK → admin(id), NULL
- **created_at**: DATETIME, NOT NULL
- **updated_at**: DATETIME, NULL
- _Constraint note_: prevent overlapping appointments per doctor in app logic.

### Table: payments
- **id**: INT, PK, AUTO_INCREMENT
- **appointment_id**: INT, FK → appointments(id), NOT NULL
- **amount_cents**: INT, NOT NULL
- **currency**: CHAR(3), NOT NULL DEFAULT 'USD'
- **method**: ENUM('card','cash','insurance','bank','other'), NOT NULL
- **status**: ENUM('pending','succeeded','failed','refunded'), NOT NULL DEFAULT 'pending'
- **paid_at**: DATETIME, NULL
- **created_at**: DATETIME, NOT NULL
- **updated_at**: DATETIME, NULL

---

## MongoDB Collection Design

### Collection: prescriptions
```json
{
  "_id": "ObjectId('64abc1234567890abcdef001')",
  "patientId": 1,
  "doctorId": 2,
  "appointmentId": 10,
  "createdAt": "2025-10-21T14:10:00Z",
  "medications": [
    { "name": "Paracetamol", "dosage": "500mg", "frequency": "q6h", "durationDays": 3 }
  ],
  "doctorNotes": "Take with water.",
  "refillCount": 0,
  "pharmacy": { "name": "Main Street Pharmacy", "location": "Downtown" }
}

### Collection: feedback
```json
{
  "_id": "ObjectId('64abc1234567890abcdef002')",
  "appointmentId": 10,
  "patientId": 1,
  "rating": 5,
  "comments": "Great visit, short wait time.",
  "createdAt": "2025-10-21T16:00:00Z",
  "tags": ["wait-time", "staff"]
}

### Collection: logs
```json
{
  "_id": "ObjectId('64abc1234567890abcdef003')",
  "occurredAt": "2025-10-21T13:55:00Z",
  "action": "patient_checked_in",
  "patientId": 1,
  "appointmentId": 10,
  "metadata": { "source": "kiosk", "kioskId": "K-1" }
}

### Collection: messages
```json
{
  "_id": "ObjectId('64abc1234567890abcdef004')",
  "threadId": "APPT-10",
  "participants": { "patientId": 1, "doctorId": 2 },
  "sender": { "role": "patient", "patientId": 1 },
  "sentAt": "2025-10-21T15:00:00Z",
  "content": "Should I keep taking the meds if I feel better?",
  "attachments": []
}

