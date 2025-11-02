Section 1: Architecture Summary

This Spring Boot application follows a layered architecture using both MVC and RESTful design patterns. The MVC structure is implemented for the Admin and Doctor dashboards using Thymeleaf templates, providing a dynamic and interactive web interface. The REST controllers handle all other modules, exposing endpoints for patient, appointment, and prescription management.

The application connects to two databases:

MySQL — used for structured relational data such as patient, doctor, appointment, and admin information.

MongoDB — used for document-based storage of prescriptions and medical records.

The controller layer handles incoming requests and delegates business logic to the service layer, which in turn communicates with the repository layer. The repository layer uses Spring Data JPA for MySQL entities and Spring Data MongoDB for document models. This separation of concerns makes the application modular, maintainable, and scalable.

Section 2: Numbered Flow of Data and Control

1. A user (Admin, Doctor, or Patient) accesses the web application through a browser or REST client.

2. The request is routed to either a Thymeleaf controller (for dashboard pages) or a REST controller (for API endpoints).

3. The controller processes the request and delegates business logic to the service layer.

4. The service layer coordinates data retrieval or updates by calling the repository layer.

5. The repository layer queries either MySQL (for relational data) or MongoDB (for prescription documents).

6. Retrieved data is processed by the service layer and returned to the controller.

7. The controller prepares the response — rendering a Thymeleaf view for web clients or returning a JSON response for REST clients.
