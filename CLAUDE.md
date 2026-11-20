# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 1. High-Level Architecture
The application follows a decoupled, layered architecture:
*   **Backend (Spring Boot):** Layered structure including Repository, Service, MVC Controller, and Web API Controller layers for separation of concerns.
*   **Frontend (React):** Offline-first design using MVVM pattern, styled with Tailwind CSS, communicating via Axios, and providing user feedback with SweetAlert2.
*   **Security:** Implemented via JSON Web Tokens (JWT) for authentication and authorization across API endpoints.

## 2. Common Development Commands
The following commands are commonly used for development within this project:
*   **Build:** Use standard Maven/Gradle commands for building the Spring Boot backend.
*   **Linting & Formatting:** Use standard IDE tools or project-specific linters for code quality checks.
*   **Testing:** To run a single unit test, use the appropriate testing framework command (e.g., `mvn test` or equivalent).
*   **Running Tests:** To execute all tests, run the full test suite (e.g., `mvn test`).

## 3. Project Guidelines
*   **Focus on Layered Design:** Always consider the separation between Repository, Service, Controller, and API layers when implementing new features.
*   **Frontend Flow:** When dealing with user interaction, prioritize the React/Axios/SweetAlert2 flow for client-side state and feedback.
*   **Security First:** Ensure all API interactions respect the JWT-based authorization scheme.

## 4. External Rules
No specific Cursor or Copilot rules were found to include here; adhere to the project specification above.