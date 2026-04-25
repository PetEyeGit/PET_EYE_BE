# PET EYE PROJECT - Backend

## Project Overview
This is the backend system for the **PET EYE** application, a platform for pet care services including clinic, spa, and boarding with live camera monitoring.

## Technology Stack
- **Framework**: Spring Boot 3.5.x
- **Database**: MySQL
- **ORM**: Spring Data JPA (Hibernate)
- **Mapping**: MapStruct
- **Security**: Spring Security + JWT
- **Lombok**: Reduced boilerplate code

## ERD Diagram
```mermaid
erDiagram
    ACCOUNT ||--o{ PET : owns
    ACCOUNT ||--o{ SHOP : owns_business
    ACCOUNT ||--o{ BOOKING : makes
    ACCOUNT ||--o{ REVIEW : writes
    ACCOUNT ||--o{ NOTIFICATION : receives

    ACCOUNT {
        int id PK
        string email UK
        string password
        string role "ADMIN, USER, SHOP_OWNER"
        string full_name
        string phone
        string address
        datetime created_at
    }

    PET {
        int id PK
        int owner_id FK
        string name
        string species
        string breed
        float weight
        date dob
        string health_note
    }

    NOTIFICATION {
        int id PK
        int user_id FK
        string title
        string content
        boolean is_read
        datetime created_at
    }

    SHOP ||--o{ SERVICE : offers
    SHOP ||--o{ CAGE : has
    SHOP ||--o{ WORKING_HOUR : operates
    SHOP ||--o{ SHOP_IMAGE : displays
    SHOP ||--o{ STAFF : employs
    SHOP ||--o{ REVIEW : receives
    SHOP ||--o{ BOOKING : receives

    SHOP {
        int id PK
        int owner_id FK
        string shop_name
        string address
        string city
        string description
        string license_number
        float rating_avg
        boolean is_verified
    }

    SERVICE {
        int id PK
        int shop_id FK
        string service_name
        string category "CLINIC, SPA, BOARDING"
        decimal price
        int duration_minutes
    }

    STAFF {
        int id PK
        int shop_id FK
        string full_name
        string role
        string phone
        string specialization
        boolean is_active
    }

    BOOKING }o--|| SERVICE : selects
    BOOKING }o--|| PET : for
    BOOKING }o--|| STAFF : assigned_to

    BOOKING ||--o{ TRANSACTION : pays_via
    BOOKING ||--o{ BOOKING_HISTORY : tracks
    BOOKING ||--o{ BOARDING_DETAIL : includes

    BOOKING {
        int id PK
        int user_id FK
        int shop_id FK
        int service_id FK
        int pet_id FK
        int staff_id FK
        datetime appointment_datetime
        string status "PENDING, CONFIRMED, COMPLETED, CANCELLED"
        string note
    }

    BOOKING_HISTORY {
        int id PK
        int booking_id FK
        string old_status
        string new_status
        datetime changed_at
        string changed_by
    }

    TRANSACTION {
        int id PK
        int booking_id FK
        decimal amount
        string payment_method "VNPAY, MOMO, CASH"
        string transaction_status
        datetime payment_date
    }

    CAGE ||--o| CAMERA : monitors
    CAGE ||--o{ BOARDING_DETAIL : stays_in

    CAGE {
        int id PK
        int shop_id FK
        string cage_code
        string type "VIP, NORMAL"
        boolean is_available
    }

    CAMERA {
        int id PK
        int cage_id FK
        string model_type
        string stream_url
        string access_token
        string status "ONLINE, OFFLINE"
    }

    BOARDING_DETAIL {
        int id PK
        int booking_id FK
        int cage_id FK
        datetime check_in
        datetime check_out
    }

    PET ||--o{ PET_MEDICAL_RECORD : has

    PET_MEDICAL_RECORD {
        int id PK
        int pet_id FK
        string diagnosis
        string treatment
        string prescription
        datetime visit_date
        string veterinarian_note
    }

    REVIEW {
        int id PK
        int shop_id FK
        int user_id FK
        int rating
        string comment
        datetime created_at
    }

    PAYMENT {
        int id PK
        int booking_id FK
        decimal amount
        string method
        string status
        string gateway_transaction_id
        datetime payment_time
    }
```

## How to Run
1.  Ensure MySQL is running and database `PET_EYE` is created.
2.  Update `src/main/resources/application-local.yml` with your database credentials.
3.  Run the application using `./mvnw spring-boot:run`.
4.  Access **Swagger UI** at: `http://localhost:8080/api/swagger-ui/index.html` (Note: context-path is `/api`)

## Main API Endpoints
- `/users`: User registration and management
- `/auth`: Authentication and JWT token generation
- `swagger-ui.html`: Documentation for all CRUD APIs
