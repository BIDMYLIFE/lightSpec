# Project Specification and Architectural Guidelines

This document outlines the foundational architectural norms, technology stack, and structural guidelines for the Spring Boot MVC website and Web API project.

## 1. Overall Architecture Philosophy
The system will follow a decoupled, layered architecture to ensure maintainability, scalability, and separation of concerns between the backend services and the frontend presentation layer.

## 2. Backend Specification (Spring Boot)
The backend will be implemented using Spring Boot, adhering to a clear, multi-layered structure:

*   **Layered Architecture:** The application will be strictly divided into distinct layers:
    *   **Repository Layer:** Responsible for data access and persistence (e.g., Spring Data JPA).
    *   **Service Layer:** Contains the core business logic and transactional operations.
    *   **MVC Controller Layer:** Handles request mapping, input validation, and delegates tasks to the Service layer.
    *   **Web API Controller Layer:** Dedicated controllers for exposing RESTful endpoints (API layer).

*   **Technology Stack:** Spring Boot will be the primary framework.

## 3. Frontend Specification (Offline Architecture)
The frontend will follow an offline-first design principle, meaning it must function effectively without reliance on external Content Delivery Networks (CDN) for core assets.

*   **Framework & Styling:**
    *   **Main Framework:** React, utilizing the Model-View-ViewModel (MVVM) pattern for state management and component structure.
    *   **Styling:** Tailwind CSS will be used for all styling, enforcing a Responsive Web Design (RWD) approach.
*   **Communication & UI:**
    *   **Asynchronous Calls:** All client-side asynchronous communication (Ajax) will be handled using the **Axios** library.
    *   **UI Feedback:** **SweetAlert2** will be integrated for providing modern, engaging user feedback modals and alerts.

## 4. Security and Authentication
Security is a critical component of this application:

*   **Authentication Mechanism:** User authentication will be managed using **JSON Web Tokens (JWT)**.
*   **Authorization Flow:** JWTs will be used to secure API endpoints, ensuring that only authenticated users can access protected resources.

## 5. Style Guide
The overall aesthetic and user experience of the application must adhere to a **Modern Design Style**, emphasizing clean layouts, intuitive navigation, and responsive behavior derived from Tailwind CSS utility classes.