# Delma — Online Consulting Platform

> A microservices-based healthcare consultation platform built with Spring Boot, Spring Cloud, Next.js, Redis, Kafka, AWS, and Groq AI. Connects patients with doctors for video consultations, document sharing, AI symptom analysis, and an integrated medical e-store.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-green)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.0-blue)](https://spring.io/projects/spring-cloud)
[![Docker](https://img.shields.io/badge/Docker-Multi--arch-2496ED)](https://www.docker.com/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF)](https://github.com/features/actions)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## Table of Contents

1. [High-Level Overview](#1-high-level-overview)
2. [System Architecture](#2-system-architecture)
3. [Module Breakdown](#3-module-breakdown)
4. [Service Communication](#4-service-communication)
5. [Core User Flows](#5-core-user-flows)
6. [API Reference](#6-api-reference)
7. [Data Models](#7-data-models)
8. [Caching Strategy](#8-caching-strategy)
9. [Authentication & Security](#9-authentication--security)
10. [Engineering Challenges & Solutions](#10-engineering-challenges--solutions)
11. [Deployment (Docker + CI/CD)](#11-deployment-docker--cicd)
12. [Local Setup](#12-local-setup)
13. [Tech Stack](#13-tech-stack)
14. [Future Roadmap](#14-future-roadmap)

---

## 1. High-Level Overview

### What Delma Does

Delma is an end-to-end online consulting platform with five core capabilities:

1. **Doctor Onboarding & Discovery** — Users register with email OTP verification, optionally apply to become doctors, get approved by admins, and appear in a searchable doctor directory.
2. **AI Symptom Checker** — Patients describe symptoms in plain English; Groq AI (Llama 3.1) recommends the right specialization and auto-filters the doctor listing.
3. **Appointment & Video Consultation** — Patients book paid appointments based on doctor availability, then join secure video calls via ZEGOCLOUD with AES-256 encryption.
4. **Medical Document Sharing** — Patients upload medical documents to AWS S3 with presigned URLs, accessible only by their assigned doctor.
5. **Medical E-Store** — End-to-end e-commerce for medications with categorized products, cart, payment via Razorpay, and order management.

### Why Microservices

A monolithic approach would couple unrelated concerns — patient bookings shouldn't be impacted by e-store outages. Each domain (users, doctors, appointments, payments, AI, etc.) is isolated:

| Benefit | How Delma Achieves It |
|---------|----------------------|
| **Independent scaling** | Scale `appointmentservice` separately from `productservice` during peak booking hours |
| **Fault isolation** | If `notificationservice` or `aiservice` crashes, video calls still work |
| **Independent deployment** | Update the e-store without touching consultation logic |
| **Technology flexibility** | Each service can use what fits — Postgres for users, external AI for symptom analysis |

---

## 2. System Architecture

### Architecture Diagram

```
                         ┌──────────────────────────┐
                         │    Frontend (Next.js)     │
                         │    React + TypeScript     │
                         │    Redux + Tailwind CSS   │
                         └────────────┬─────────────┘
                                      │ HTTPS
                                      ▼
                         ┌──────────────────────────┐
                         │   Spring Cloud Gateway    │
                         │   (Port 8089 - WebFlux)   │
                         │  • JWT Auth Filter        │
                         │  • CORS Handling          │
                         │  • Route to Services      │
                         └────────────┬─────────────┘
                                      │ lb://service-name
                                      ▼
                         ┌──────────────────────────┐
                         │   Eureka Server (8761)    │
                         │   Service Discovery       │
                         └────────────┬─────────────┘
                                      │
       ┌──────────────────────────────┼──────────────────────────────┐
       │                              │                              │
       ▼                              ▼                              ▼

┌─────────────┐  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐
│ userservice │  │doctorservice│  │appointmentsvc│  │paymentservice│  │ documentsvc │
│   :8111     │  │    :8010    │  │    :8012     │  │    :8083     │  │    :8091    │
│             │  │             │  │              │  │              │  │             │
│  PostgreSQL │  │  PostgreSQL │  │  PostgreSQL  │  │  PostgreSQL  │  │  PostgreSQL │
│  + Redis    │  │  + Redis    │  │              │  │              │  │   + AWS S3  │
│  (OTP)      │  │  (Cache)    │  │              │  │              │  │             │
└──────┬──────┘  └──────┬──────┘  └──────┬───────┘  └──────┬───────┘  └─────────────┘
       │                │                │                 │
       └────────────────┴────────┬───────┴─────────────────┘
                                 │ Apache Kafka
                                 ▼
                         ┌──────────────────────────┐
                         │   notificationservice     │
                         │        :8017              │
                         │   (Email + Real-time)     │
                         └──────────────────────────┘

┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  aiservice   │  │productservice│  │categoryservice│ │ orderservice │
│    :8095     │  │    :8016     │  │    :8015     │  │    :8013     │
│              │  │              │  │              │  │              │
│  Groq API    │  │  PostgreSQL  │  │  PostgreSQL  │  │  PostgreSQL  │
│  (Llama 3.1) │  │  (E-Store)   │  │  (Categories)│  │  (Orders)    │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘

External Services:
• ZEGOCLOUD (Video calls with AES-256)   • Razorpay (Payments)
• AWS S3 (Document storage)               • Apache Kafka (Async messaging)
• Groq AI (Symptom analysis)              • Gmail SMTP (OTP emails)
```

### Architectural Patterns Used

| Pattern | Where It's Used | Why |
|---------|----------------|-----|
| **API Gateway** | Spring Cloud Gateway | Single entry point, JWT validation, routing |
| **Service Discovery** | Eureka | Services find each other by name, not IP |
| **Circuit Breaker** | Resilience4j on Feign clients | Prevent cascade failures |
| **Cache-Aside** | Redis on doctor listings, OTP storage | Reduce DB load, ephemeral data with TTL |
| **Event-Driven** | Kafka for notifications | Decouple notification logic from business logic |
| **CQRS-lite** | Separate read/write paths | Optimize each independently |
| **Shared Library** | `common-lib` module | DRY — exception handling, ApiResponse |
| **DTO Layer** | Every service | Never expose JPA entities directly |
| **AI as a Service** | aiservice → Groq API | Externalized inference, no model hosting required |

---

## 3. Module Breakdown

### 3.1 common-lib (Shared Library)

**Purpose:** Code shared by all microservices to avoid duplication.

**Contents:**
- `ApiResponse<T>` — standard response wrapper for all endpoints
- `GlobalExceptionHandler` — centralized error handling
- Custom exceptions: `ResourceNotFoundException`, `ConflictException`, `BadRequestException`, `UnauthorizedException`

**Standard Response Shape:**
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* response payload */ },
  "errorCode": null
}
```

### 3.2 gateway (API Gateway)

**Port:** 8089
**Tech:** Spring Cloud Gateway 5.0 (WebFlux/Reactive)

**Responsibilities:**
- Single entry point for all client requests
- JWT token validation via `JwtAuthGatewayFilter`
- Routing requests to appropriate microservices via `lb://service-name`
- CORS handling for frontend
- Public endpoints whitelist: `/auth/login`, `/auth/signup`, `/auth/refresh`, `/auth/verify-otp`, `/auth/resend-otp`

**Routing Rules:**
| Path Pattern | Target Service |
|--------------|----------------|
| `/auth/**`, `/api/users/**`, `/api/v1/admin/**` | userservice |
| `/api/v1/doctor/**` | doctorservice |
| `/api/v1/slots/**`, `/api/v1/appointments/**` | appointmentservice |
| `/api/v1/payments/**` | paymentservice |
| `/api/v1/documents/**` | documentservice |
| `/api/v1/product/**` | productservice |
| `/api/v1/category/**` | categoryservice |
| `/api/v1/orders/**` | orderservice |
| `/api/v1/notifications/**` | notificationservice |
| `/api/v1/ai/**` | aiservice |

### 3.3 discovery-server (Eureka)

**Port:** 8761

**Responsibilities:**
- Service registry — every service registers itself on startup
- Health checks via heartbeat every 30 seconds
- Provides live service addresses to gateway and Feign clients
- Removes unhealthy instances automatically

**Dashboard:** http://localhost:8761

### 3.4 userservice

**Port:** 8111
**Database:** PostgreSQL (`delma_user_db`) + Redis (for OTP)

**Responsibilities:**
- User registration with email OTP verification
- JWT token generation (access + refresh)
- Role management (USER, DOCTOR, ADMIN)
- Doctor application submission
- Admin operations (approve doctors, manage users)
- Secure refresh token rotation
- Gmail SMTP integration for OTP delivery

**OTP Flow:**
- 6-digit OTP generated and stored in Redis (key: `otp:{email}`)
- 10-minute TTL on Redis key — auto-expires
- One-time use — deleted on successful verification
- User account stored with `is_verified=false` until OTP confirmed
- Unverified users cannot login (blocked at authentication)

**Feign Clients:** `DoctorClient` (calls doctorservice for admin operations)

### 3.5 doctorservice

**Port:** 8010
**Database:** PostgreSQL (`delma_doctor_db`) + Redis cache

**Responsibilities:**
- Doctor profile management
- Application status (PENDING, APPROVED, REJECTED)
- Doctor search and listing (with caching)
- Doctor approval workflow

**Caching:** Redis with `@Cacheable` on listings, `@CacheEvict` on status changes
**Feign Clients:** `UserServiceClient` (to add DOCTOR role on approval)
**Kafka:** Publishes `NotificationEvent` on approval

### 3.6 appointmentservice

**Port:** 8012
**Database:** PostgreSQL (`delma_appointment_db`)

**Responsibilities:**
- Doctor slot management (availability time slots)
- Appointment booking with payment integration
- Video call session creation (ZEGOCLOUD)
- Appointment status tracking
- Optimistic locking on slots to prevent double-booking

### 3.7 paymentservice

**Port:** 8083
**Database:** PostgreSQL (`delma_payment_db`)

**Responsibilities:**
- Razorpay payment order creation
- Payment signature verification
- Refund processing
- Transaction history
- Supports multiple sources: APPOINTMENT, ORDER (e-store)

### 3.8 documentservice

**Port:** 8091
**Database:** PostgreSQL (`delma_document_db`) + AWS S3

**Responsibilities:**
- Medical document upload to S3
- Generate presigned URLs (10 min TTL)
- Document access control (only owning patient + assigned doctor)
- Delete from both DB and S3

### 3.9 productservice

**Port:** 8016
**Database:** PostgreSQL (`delma_product_db`)

**Responsibilities:**
- E-store product catalog
- Product CRUD (admin only)
- Product search and filtering

### 3.10 categoryservice

**Port:** 8015
**Database:** PostgreSQL (`delma_category_db`)

**Responsibilities:**
- Product category management
- Category-based product grouping

### 3.11 orderservice

**Port:** 8013
**Database:** PostgreSQL (`delma_order_db`)

**Responsibilities:**
- Cart management
- Order placement
- Order status (PLACED, SHIPPED, DELIVERED, CANCELLED)
- Order history per user

### 3.12 notificationservice

**Port:** 8017
**Tech:** Kafka consumer

**Responsibilities:**
- Listen to Kafka `notification-events` topic
- Send emails via SMTP
- Real-time push notifications
- Notification history persistence

### 3.13 aiservice (NEW)

**Port:** 8095
**Tech:** Spring Boot + Groq API (no local DB)

**Responsibilities:**
- Accept natural language symptom descriptions from patients
- Call Groq API (Llama 3.1 `llama-3.1-8b-instant`)
- Parse AI response with strict JSON contract
- Return doctor specialization recommendation
- Provide medical disclaimer in every response

**Why Groq:** Sub-second inference latency at OpenAI-compatible API; free tier sufficient for development.

**Prompt Engineering:** Carefully constructed system prompt restricts AI output to a fixed list of specializations matching the platform's doctor categories. If JSON parsing fails, falls back to `General Medicine` to ensure the endpoint never returns 500 errors.

**Response Shape:**
```json
{
  "specialization": "Cardiology",
  "message": "Based on chest pain symptoms, consult a cardiologist...",
  "disclaimer": "This is not medical advice. Please consult a qualified doctor."
}
```

---

## 4. Service Communication

### Synchronous (Feign + Eureka)

Used when one service needs immediate response from another.

**Example:** When a doctor application is approved, `doctorservice` needs `userservice` to add the DOCTOR role.

```
doctorservice → DoctorClient.addDoctorRole(userId, token)
              → Eureka resolves "userservice"
              → HTTP PUT to user
              → userservice updates role
              → returns ApiResponse<Void>
```

**Resilience:** Circuit Breaker (Resilience4j) — if `userservice` is down, opens circuit and fails fast instead of timing out repeatedly.

### Asynchronous (Kafka)

Used when the action doesn't need immediate response — fire and forget.

**Example:** Sending notifications doesn't block the approval flow.

```
doctorservice → publishes NotificationEvent to Kafka topic
              → returns immediately to client
                    │
                    ▼
notificationservice ← consumes from Kafka
                    → sends email + push notification
```

**Topics:**
- `notification-events` — for all user-facing notifications
- `payment-events` — for payment status changes (future)

### External API Calls (REST)

**aiservice → Groq API:** Synchronous REST call via `RestTemplate` with Bearer token authentication. Response is parsed using Jackson `ObjectMapper`. Timeouts configured to 30 seconds.

### Why Multiple Communication Patterns?

| Need | Use |
|------|-----|
| Need response to continue | Feign (sync) |
| Side effect, can be delayed | Kafka (async) |
| External third-party API | RestTemplate |
| Other service must be available | Feign |
| Other service can be temporarily down | Kafka |

---

## 5. Core User Flows

### 5.1 User Registration with Email OTP

```
┌─────────┐                ┌──────────┐                ┌──────────────┐                ┌────────┐
│ Browser │                │ Gateway  │                │ userservice  │                │ Redis  │
└────┬────┘                └────┬─────┘                └──────┬───────┘                └───┬────┘
     │                          │                             │                            │
     │ POST /auth/signup        │                             │                            │
     │ {name, email, password}  │                             │                            │
     ├─────────────────────────>│                             │                            │
     │                          │ POST /auth/signup           │                            │
     │                          ├────────────────────────────>│                            │
     │                          │                             │                            │
     │                          │      Validate input         │                            │
     │                          │      Check duplicate email  │                            │
     │                          │      Save user is_verified=false                         │
     │                          │      Generate 6-digit OTP   │                            │
     │                          │      Store OTP in Redis     │                            │
     │                          │      (key: otp:email,       │                            │
     │                          │       TTL: 10 min)          │                            │
     │                          │                             ├───────────────────────────>│
     │                          │      Send OTP via Gmail SMTP│                            │
     │                          │ ApiResponse(success=true)   │                            │
     │                          │<────────────────────────────│                            │
     │ "OTP sent to your email" │                             │                            │
     │<─────────────────────────│                             │                            │
     │                          │                             │                            │
     │ User checks email,       │                             │                            │
     │ receives 6-digit OTP     │                             │                            │
     │                          │                             │                            │
     │ POST /auth/verify-otp    │                             │                            │
     │ {email, otp}             │                             │                            │
     ├─────────────────────────>│                             │                            │
     │                          ├────────────────────────────>│                            │
     │                          │      GET otp:email from Redis                            │
     │                          │                             ├───────────────────────────>│
     │                          │      Match → set is_verified=true                        │
     │                          │      DELETE otp:email       │                            │
     │                          │                             ├───────────────────────────>│
     │                          │ "Email verified, please login"                           │
     │<─────────────────────────│<────────────────────────────│                            │
     │                          │                             │                            │
     │ POST /auth/login         │ (now works — is_verified=true)                           │
     ├─────────────────────────>│                             │                            │
     │ JWT + refresh cookie     │                             │                            │
     │<─────────────────────────│                             │                            │
```

**Resend OTP flow:** If user doesn't receive email or OTP expires, `POST /auth/resend-otp` regenerates OTP and re-sends. Resets TTL to fresh 10 minutes.

### 5.2 AI Symptom Checker Flow

```
┌─────────┐         ┌──────────┐         ┌────────────┐         ┌──────────┐
│ Browser │         │ Gateway  │         │ aiservice  │         │ Groq API │
└────┬────┘         └────┬─────┘         └─────┬──────┘         └────┬─────┘
     │                   │                     │                     │
     │ POST /api/v1/ai/  │                     │                     │
     │   symptom-check   │                     │                     │
     │ {symptoms: "chest │                     │                     │
     │   pain, fever"}   │                     │                     │
     ├──────────────────>│                     │                     │
     │                   ├────────────────────>│                     │
     │                   │                     │                     │
     │                   │       Build prompt with strict JSON format│
     │                   │                     │                     │
     │                   │                     ├────────────────────>│
     │                   │       POST /openai/v1/chat/completions    │
     │                   │       Bearer GROQ_API_KEY                 │
     │                   │       model: llama-3.1-8b-instant         │
     │                   │                     │                     │
     │                   │                     │<────────────────────│
     │                   │       Parse JSON from response            │
     │                   │       Fallback to "General Medicine"      │
     │                   │       if JSON parsing fails               │
     │                   │                     │                     │
     │                   │ ApiResponse<SymptomResponse>              │
     │                   │<────────────────────│                     │
     │ {specialization,  │                     │                     │
     │  message,         │                     │                     │
     │  disclaimer}      │                     │                     │
     │<──────────────────│                     │                     │
     │                   │                     │                     │
     │ Frontend then calls /api/v1/doctor/search/{specialization}    │
     │ to filter doctors automatically                               │
```

### 5.3 Doctor Application Workflow

```
USER FLOW                     ADMIN FLOW                   POST-APPROVAL
─────────                     ──────────                   ─────────────

User logs in            Admin logs in
     │                        │
     ▼                        ▼
POST /api/users/        GET /api/v1/admin/
  apply-doctor           pending-doctors
     │                        │
     ▼                        ▼
userservice              userservice
saves application        Feign → doctorservice
                          (returns pending list)
                              │
                              ▼
                        Admin clicks approve
                              │
                              ▼
                        PUT /api/v1/admin/
                          approve-doctors/{id}
                              │
                              ▼
                        userservice
                        Feign → doctorservice
                                  │
                                  ▼
                            doctorservice:
                            1. Update status APPROVED
                            2. Evict Redis cache
                            3. Feign → userservice
                               (add DOCTOR role)
                            4. Publish Kafka event ──┐
                                                     │
                                                     ▼
                                          notificationservice
                                          consumes event
                                          sends email + push
```

### 5.4 Appointment Booking + Video Call

```
1. User browses approved doctors
   GET /api/v1/doctor/all (cached in Redis)

   OR uses AI symptom checker to get filtered list:
   POST /api/v1/ai/symptom-check → returns specialization
   GET /api/v1/doctor/search/{specialization} → filtered doctors

2. User views doctor's available slots
   GET /api/v1/appointments/slots?doctorId={id}&date={YYYY-MM-DD}

3. User selects slot, initiates payment
   POST /api/v1/payments/create
   { amount, refId: "SLOT_{slotId}", sourceType: "APPOINTMENT" }
   → paymentservice creates Razorpay order
   → returns rzpOrderId

4. Frontend opens Razorpay modal with rzpOrderId
   User completes payment in modal

5. On payment success, frontend calls:
   POST /api/v1/payments/verify
   { orderId, paymentId, signature }
   → paymentservice verifies signature with Razorpay secret
   → returns 200 on success

6. Frontend then books the appointment:
   POST /api/v1/appointments/book
   ?userId=...&doctorId=...&slotId=...
   → appointmentservice creates appointment with optimistic lock
   → publishes notification event

7. Both patient and doctor receive:
   - Email with appointment details
   - In-app notification

8. At appointment time:
   GET /api/v1/appointments/{id}/video-token
   → returns ZEGOCLOUD token (AES-256 encrypted session)

9. Both join video call
   Frontend uses ZEGOCLOUD SDK with token
```

### 5.5 Document Upload & Sharing

```
PATIENT UPLOADS                          DOCTOR VIEWS
───────────────                          ────────────

POST /api/v1/documents/upload
  + multipart file + userId
        │
        ▼
documentservice:
  1. Generate unique filename
  2. Upload to S3 bucket
  3. Save metadata in DB           GET /api/v1/documents/
        │                            getall-documents/{userId}
        │                                  │
        │                                  ▼
        │                          documentservice:
        │                            1. Verify doctor has
        │                               appointment with patient
        │                            2. Generate presigned URLs
        │                               (TTL: 10 min)
        ▼                            3. Return list with URLs
  Return doc metadata                      │
                                           ▼
                                    Doctor's browser:
                                    GET <presigned S3 URL>
                                    → S3 streams document
                                    → URL expires in 10 min
```

### 5.6 E-Store Order Flow

```
1. Browse products
   GET /api/v1/product?category=...

2. Add to cart (orderservice)
   POST /api/v1/orders/cart/add

3. View cart
   GET /api/v1/orders/cart/{userId}

4. Checkout — create payment
   POST /api/v1/payments/create
   → Razorpay order_id

5. Complete Razorpay payment
   POST /api/v1/payments/verify
   → signature verified

6. paymentservice → orderservice (Feign)
   → creates Order with status PLACED
   → clears cart

7. Order confirmation email
   (via Kafka → notificationservice)

8. Status updates as order progresses:
   PLACED → SHIPPED → DELIVERED
```

---

## 6. API Reference

### 6.1 userservice (Public + Auth Required)

#### `POST /auth/signup`
Create new user account. Sends OTP email. User stored as unverified.
```json
Request:  { "name": "John Doe", "email": "user@example.com", "password": "secret123" }
Response: { "success": true, "message": "OTP sent to your email. Please verify." }
```

#### `POST /auth/verify-otp` (NEW)
Verify the OTP sent during signup. Marks user as verified.
```json
Request:  { "email": "user@example.com", "otp": "123456" }
Response: { "success": true, "message": "Email verified successfully" }
```

#### `POST /auth/resend-otp` (NEW)
Regenerate and resend OTP. Useful when OTP expires or email is missed.
```json
Request:  { "email": "user@example.com" }
Response: { "success": true, "message": "OTP resent successfully" }
```

#### `POST /auth/login`
Authenticate user, return JWT. Blocks login if `is_verified=false`.
```json
Request:  { "email": "user@example.com", "password": "secret123" }
Response: { "success": true, "data": { "jwtToken": "eyJ...", "userId": 1, "role": "USER", "username": "John" } }
Headers:  Set-Cookie: refreshToken=...; HttpOnly; SameSite=Strict
```

#### `POST /auth/refresh`
Refresh access token using cookie.
```json
Cookie:   refreshToken=...
Response: { "success": true, "data": { "accessToken": "eyJ..." } }
```

#### `POST /auth/logout`
Invalidate refresh tokens.
```json
Response: { "success": true, "message": "Logged out successfully" }
```

#### `POST /auth/admin-login`
Admin-specific login endpoint.
```json
Request:  { "email": "admin@delma.com", "password": "..." }
Response: { "success": true, "data": { "jwtToken": "...", "isAdmin": true } }
```

#### `GET /api/users/{id}` 🔒 Auth required
Get user by ID.

#### `POST /api/users/apply-doctor` 🔒 USER role
Submit doctor application.

#### `GET /api/users/doctors` 🔒 Auth required
Get all approved doctors (proxied to doctorservice).

#### `PUT /api/v1/admin/approve-doctors/{doctorId}` 🔒 ADMIN
Approve a doctor application.

#### `PUT /api/v1/admin/reject-doctors/{doctorId}` 🔒 ADMIN
Reject a doctor application.

#### `GET /api/v1/admin/pending-doctors` 🔒 ADMIN
Get all pending doctor applications.

#### `GET /api/v1/admin/getall-users` 🔒 ADMIN
Get all registered users.

---

### 6.2 doctorservice

#### `POST /api/v1/doctor/apply` 🔒 Auth required
Submit doctor application (called from userservice).

#### `GET /api/v1/doctor/all` 🟢 Cached (Redis, 10 min TTL)
Get all approved doctors.

#### `GET /api/v1/doctor/search/{keyword}`
Search approved doctors by name or specialization (used by AI symptom checker).

#### `GET /api/v1/doctor/pending` 🔒 ADMIN, 🟢 Cached
Get pending doctor applications.

#### `PUT /api/v1/doctor/approve/{id}` 🔒 ADMIN, 🔄 Cache evicted
Approve a doctor application.

#### `PUT /api/v1/doctor/reject/{id}` 🔒 ADMIN, 🔄 Cache evicted
Reject a doctor application.

---

### 6.3 appointmentservice

#### `GET /api/v1/appointments/slots`
Get available slots for a doctor on a date.
```
Query: ?doctorId=1&date=2026-05-15
```

#### `POST /api/v1/appointments/book` 🔒 USER
Book an appointment after payment.
```
Query: ?userId=1&doctorId=2&slotId=5
```

#### `GET /api/v1/appointments/user` 🔒 Auth
Get user's appointments (as patient).
```
Query: ?userId=1
```

#### `GET /api/v1/appointments/doctor` 🔒 Auth
Get doctor's appointments.
```
Query: ?doctorId=2
```

---

### 6.4 paymentservice

#### `POST /api/v1/payments/create`
Create Razorpay order for appointment or e-store.
```json
Request: { "amount": 50000, "refId": "SLOT_5", "sourceType": "APPOINTMENT" }
Response: { "success": true, "data": "order_xxx" }
```

#### `POST /api/v1/payments/verify`
Verify Razorpay payment signature.
```json
Request: { "orderId": "order_xxx", "paymentId": "pay_yyy", "signature": "..." }
Response: { "success": true, "message": "Payment verified" }
```

#### `POST /api/v1/payments/webhook`
Razorpay webhook for asynchronous payment status updates.

---

### 6.5 documentservice

#### `POST /api/v1/documents/upload` 🔒 Auth, multipart
Upload document to S3.

#### `GET /api/v1/documents/getall-documents/{userId}` 🔒 Auth
Get user's documents with presigned S3 URLs.

#### `DELETE /api/v1/documents/delete-document/{id}` 🔒 Auth
Delete from both DB and S3.

---

### 6.6 aiservice (NEW)

#### `POST /api/v1/ai/symptom-check`
Analyze symptoms with Groq AI and recommend a specialization.
```json
Request:  { "symptoms": "I have chest pain and shortness of breath" }
Response: {
  "success": true,
  "data": {
    "specialization": "Cardiology",
    "message": "Based on your symptoms, consulting a cardiologist would be appropriate.",
    "disclaimer": "This is not medical advice. Please consult a qualified doctor."
  }
}
```

**Notes:**
- Endpoint is public — no JWT required (AI analysis is non-sensitive)
- If Groq API fails or returns malformed JSON, falls back to `General Medicine`
- Specialization is constrained to a fixed list matching platform's doctor categories

---

### 6.7 productservice / categoryservice / orderservice / notificationservice

See section 3 for module responsibilities. Standard REST CRUD endpoints with `ApiResponse<T>` wrapper.

---

## 7. Data Models

### 7.1 User Entity (userservice)
```
User
├── id: Long (PK)
├── username: String
├── email: String (unique)
├── password: String (BCrypt hashed)
├── isVerified: Boolean   [false until OTP verified]
├── roles: Set<Role>      [USER, DOCTOR, ADMIN]
├── isDoctor: String
├── isAdmin: String
└── createdAt: LocalDateTime

RefreshToken
├── id: Long (PK)
├── userId: Long (FK)
├── token: String
├── expiresAt: LocalDateTime
└── createdAt: LocalDateTime

OTP (stored in Redis, not Postgres)
├── key: "otp:{email}"
├── value: "123456"      [6-digit code]
└── TTL: 600 seconds     [10 minutes]
```

### 7.2 Doctor Entity (doctorservice)
```
Doctor
├── id: Long (PK)
├── userId: String (FK to User)
├── firstName, lastName, email, phone
├── address, website, gender
├── specialization: String
├── experience: Integer
├── feesPerConsultation: Double
└── status: ApplicationStatus  [PENDING, APPROVED, REJECTED]
```

### 7.3 Appointment Entity (appointmentservice)
```
DoctorSlot
├── id: Long (PK)
├── doctorId: Long
├── startTime: LocalDateTime
├── endTime: LocalDateTime
├── status: SlotStatus   [AVAILABLE, BOOKED, BLOCKED]
└── version: Long        [Optimistic Locking]

Appointment
├── id: Long (PK)
├── slotId: Long (FK)
├── userId: Long (patient)
├── doctorId: Long
├── status: AppointmentStatus [BOOKED, COMPLETED, CANCELLED]
└── createdAt: LocalDateTime
```

### 7.4 Payment Entity (paymentservice)
```
Payment
├── id: Long (PK)
├── userId: Long
├── refId: String         [e.g. "SLOT_5" or "ORDER_12"]
├── sourceType: String    [APPOINTMENT, ORDER]
├── razorpayOrderId: String
├── razorpayPaymentId: String
├── amount: Long
├── status: PaymentStatus [CREATED, SUCCESS, FAILED]
└── createdAt: LocalDateTime
```

### 7.5 Document Entity (documentservice)
```
Document
├── id: Long (PK)
├── name: String
├── type: String (MIME)
├── userId: String
├── filePath: String (S3 key)
├── url: String (S3 URL)
└── uploadedAt: LocalDateTime
```

### 7.6 AI Service DTOs (aiservice)
```
SymptomRequest
└── symptoms: String       [user's plain English description]

SymptomResponse
├── specialization: String [recommended doctor specialization]
├── message: String        [explanation from AI]
└── disclaimer: String     [medical disclaimer]
```

### 7.7 Order & Product (e-store)
```
Product
├── id: Long (PK)
├── name, description
├── price: BigDecimal
├── stock: Integer
├── imageUrl: String
└── categoryId: Long (FK)

Category
├── id: Long (PK)
├── name: String
└── description: String

Order
├── id: Long (PK)
├── userId: Long
├── items: List<OrderItem>
├── totalAmount: BigDecimal
├── paymentId: String
├── status: OrderStatus [PLACED, SHIPPED, DELIVERED, CANCELLED]
├── shippingAddress: String
└── createdAt: LocalDateTime
```

---

## 8. Caching Strategy

### Where Caching Is Used

**doctorservice — Redis cache on doctor listings.**

| Method | Annotation | Cache Key | TTL |
|--------|-----------|-----------|-----|
| `getAllDoctors()` | `@Cacheable` | `doctors::all` | 10 min |
| `getPendingApplications()` | `@Cacheable` | `doctors::pending` | 10 min |
| `approveApplication()` | `@CacheEvict allEntries=true` | (clears all) | — |
| `rejectApplication()` | `@CacheEvict allEntries=true` | (clears all) | — |

**userservice — Redis for OTP storage.**

| Use Case | Key Pattern | Value | TTL |
|----------|-------------|-------|-----|
| OTP during signup | `otp:{email}` | 6-digit code | 10 min |

OTPs are deleted on successful verification (one-time use). TTL provides automatic cleanup of unverified attempts.

### Why TTL Even With Cache Eviction?

**Defense in depth.** Two independent freshness mechanisms:
1. `@CacheEvict` — primary, immediate cache deletion when data changes
2. TTL 10 min — safety net for edge cases (Redis blip, direct DB updates)

This guarantees stale data has a maximum lifetime of 10 minutes even if eviction fails.

### Cache Hit/Miss Flow
```
GET /api/v1/doctor/all
        │
        ▼
   Check Redis: "doctors::all"
        │
   ┌────┴────┐
   │         │
HIT        MISS
   │         │
   │         ▼
   │    Query DB → store in Redis with 10 min TTL
   │         │
   ▼         ▼
   Return data to client
```

---

## 9. Authentication & Security

### Account Verification (Email OTP)

Every new user must verify their email before logging in.

**Why:**
- Prevents bot signups
- Confirms valid contact channel for password resets, appointment notifications
- Industry standard for healthcare platforms (where contact accuracy matters)

**Implementation:**
- 6-digit OTP generated using `SecureRandom`
- Stored in Redis with 10-minute TTL — server-side state, not client-controlled
- Delivered via Gmail SMTP (App Password authentication)
- User table has `is_verified` boolean column
- `User.isEnabled()` returns `is_verified` — Spring Security blocks unverified logins
- One-time use — OTP deleted from Redis on successful verification

### JWT Token Architecture

**Access Token (15 minutes)**
- Sent in `Authorization: Bearer <token>` header
- Validated by gateway on every request
- Contains: userId, roles, expiry

**Refresh Token (7 days)**
- Stored in HttpOnly Secure cookie
- Saved in DB for revocation tracking
- Used to get new access tokens

### Refresh Token Rotation
1. Frontend access token expires
2. Frontend calls `POST /auth/refresh`
3. Gateway validates refresh token in cookie
4. userservice:
   - Validates token in DB
   - Generates NEW refresh token (rotation)
   - Invalidates OLD refresh token
   - Returns new access token

**Why rotation?** Stolen refresh tokens become useless after first use.

### Gateway JWT Filter Logic
```
Request arrives at gateway
        │
        ▼
Is path in whitelist?
(/auth/login, /auth/signup, /auth/refresh,
 /auth/verify-otp, /auth/resend-otp, /api/v1/ai/**)
        │
   ┌────┴────┐
   │         │
   YES       NO
   │         │
   │         ▼
   │   Extract Authorization header
   │         │
   │   Validate JWT
   │         │
   │   Extract userId, roles
   │         │
   │   Add X-User-Id, X-Roles headers
   │         │
   ▼         ▼
   Forward to service
```

### Video Call Security (ZEGOCLOUD)

Custom AES-256 encryption engine on top of ZEGOCLOUD:
1. Server generates session-specific encryption key
2. Both participants get the key via secure JWT-validated channel
3. Audio/video stream encrypted end-to-end
4. Key destroyed after session ends

### Payment Security (Razorpay)

- Razorpay order is created server-side — frontend never sees the key secret
- Payment signature verified on backend after Razorpay returns success
- HMAC-SHA256 verification using Razorpay's webhook secret
- Failed signature verification rejects the payment entirely

---

## 10. Engineering Challenges & Solutions

This section documents real engineering problems encountered while building Delma and how they were solved. These are the kinds of problems that come up in system design interviews.

### 10.1 Race Condition: Double Booking Prevention

#### The Problem

The appointment booking flow contains a classic concurrency bug. The naive implementation has three steps separated in time:

```java
// Step 1 — READ
DoctorSlot slot = slotRepository.findById(slotId).orElseThrow(...);

// Step 2 — CHECK
if (slot.getStatus().equals(SlotStatus.BOOKED)) {
    throw new ConflictException("Slot already booked");
}

// Step 3 — WRITE
slot.setStatus(SlotStatus.BOOKED);
slotRepository.save(slot);
```

Between Step 1 and Step 3, there is a time gap — even if it is only milliseconds. When two patients click "Book" on the same slot at the same moment:

```
TIME →

Patient A thread                    Patient B thread
────────────────                    ────────────────
1. findById(slotId)
   → status = AVAILABLE ✅
                                    2. findById(slotId)
                                       → status = AVAILABLE ✅
                                       (DB hasn't changed yet!)

3. Status check passes ✅
                                    4. Status check passes ✅

5. save(slot) → BOOKED in DB ✅
   appointment created ✅
                                    6. save(slot) → BOOKED in DB 🔥
                                       appointment created 🔥

RESULT: Two appointments for the same slot!
```

This is called a **Time-of-Check to Time-of-Use (TOCTOU)** race condition.

#### The Solution: Optimistic Locking with @Version

```java
@Entity
public class DoctorSlot {
    @Id private Long id;
    @Enumerated(EnumType.STRING) private SlotStatus status;
    @Version private Long version;
}
```

Hibernate automatically adds version check to UPDATE statements:
```sql
UPDATE doctor_slot SET status='BOOKED', version=2
WHERE id=5 AND version=1
```

When two threads attempt to update with the same version, only one succeeds. The other gets `ObjectOptimisticLockingFailureException`, which is caught and returned as HTTP 409 to the client.

---

### 10.2 Cache Invalidation Strategy

When admin approves a doctor, the cached "all approved doctors" list must update. Solution combines `@CacheEvict` (immediate eviction on state change) with TTL of 10 minutes (safety net) — defense in depth.

---

### 10.3 Service Discovery vs Hardcoded URLs

Initial Feign clients had hardcoded `url = "http://localhost:8010"`. Removed in favor of Eureka-based discovery via `@FeignClient(name = "doctorservice")` — services find each other dynamically, supporting horizontal scaling and zero-downtime deploys.

---

### 10.4 Spring Cloud Gateway 5.0 Configuration Migration

After upgrading to Spring Cloud 2025.x (Gateway 5.0), routes silently stopped working. Root cause: configuration namespace moved from `spring.cloud.gateway.routes` to `spring.cloud.gateway.server.webflux.routes`. Old config keys are silently ignored — no warning, no error.

**Lesson:** Always read migration guides for major framework upgrades.

---

### 10.5 Inter-Service Contract Evolution

When doctorservice changed return type from `List<DoctorResponse>` to `ApiResponse<List<DoctorResponse>>`, all Feign clients broke with deserialization errors. Fix: update client signatures + add `@JsonCreator` to `ApiResponse` constructor for Jackson deserialization.

---

### 10.6 OTP Implementation: Why Redis Over Postgres

#### The Problem

OTPs need to be:
1. Short-lived (auto-delete after 10 minutes)
2. Read-heavy during verification window
3. Deleted after one use

#### Why Redis Won

| Aspect | Postgres | Redis |
|--------|----------|-------|
| **TTL** | Manual cleanup job needed | Native `EXPIRE` command |
| **Read speed** | ~5ms with index | <1ms |
| **Schema overhead** | Need table, indexes, migrations | Just key-value |
| **Cleanup** | Cron job to delete expired rows | Automatic |

For ephemeral data with strict TTL, Redis is the natural fit. Code is simpler:
```java
redisTemplate.opsForValue().set("otp:" + email, otp, 10, TimeUnit.MINUTES);
```

When Postgres would be wrong: persistent data, complex queries, transactional consistency with other tables. None of those apply to OTPs.

---

### 10.7 Email Password Truncation Bug

#### The Problem

Gmail App Password was failing authentication intermittently. SMTP returned `Username and Password not accepted` despite correct credentials.

#### Root Cause

Gmail App Password copied with spaces (`juzb iuao ofjz ztsb`). In `.env` file, the variable was being read by shell which stripped at the first space — only `juzb` was being used as the password.

#### The Fix

Removed all spaces from the App Password in `.env`:
```
# Wrong
MAIL_PASSWORD=juzb iuao ofjz ztsb

# Right
MAIL_PASSWORD=juzbiuaoofjzztsb
```

**Lesson:** Environment variables in `.env` files don't handle spaces gracefully. Always validate variable values inside the container with `docker exec <container> env | grep MAIL`.

---

### 10.8 Frontend Race Condition: Filter Stacking

When users applied multiple filters in sequence (e.g. Cardiology → then Female), the second filter was applied on already-filtered results instead of the original list, producing incorrect results.

#### Root Cause

```typescript
const handleFilter = () => {
  let filtered = [...memoizedDocs]; // ← Already filtered!
  // Apply filters...
};
```

#### The Fix

Introduced `originalDocs` state that always holds the unfiltered list:
```typescript
const [docs, setDocs] = useState<DoctorInputProps[]>([]);
const [originalDocs, setOriginalDocs] = useState<DoctorInputProps[]>([]);

const handleFilter = () => {
  let filtered = [...originalDocs]; // ← Always full list
  // Apply filters...
  setDocs(filtered);
};
```

**Lesson:** In React, distinguish between "source of truth" state and "derived/displayed" state. Filters should always be a pure function of the source.

---

### 10.9 Razorpay Frontend Integration: Token Mutation Bug

#### The Problem

After Razorpay payment succeeded, `verify` endpoint returned 500. Frontend was sending `orderId: undefined`.

#### Root Cause

Two bugs combined:

1. **Wrong response field access:**
   ```typescript
   // Wrong — backend returns ApiResponse<String>
   order_id: data.rzpOrderId
   
   // Right — actual orderId is at .data.data
   order_id: response.data.data
   ```

2. **Axios interceptor mutating Razorpay response:**
   ```typescript
   // Interceptor was adding token to ALL request bodies
   config.data = { ...config.data, token: jwt };
   // This mutated the Razorpay response object passed to verify
   ```

#### The Fix

- Removed token-injection in axios interceptor (JWT is already in Authorization header)
- Fixed response field access to `response.data.data`

**Lesson:** Be very careful with axios interceptors that mutate request data. JWT belongs in headers, not body.

---

## 11. Deployment (Docker + CI/CD)

### Docker Containerization

Every service has its own `Dockerfile` and is published to Docker Hub. The full stack runs via `docker-compose up`.

**docker-compose.yml** orchestrates:
- 13 microservices (Spring Boot)
- PostgreSQL 16
- Redis 7
- Apache Kafka 3.x + Zookeeper

All services communicate over a shared Docker network using service names (no IP addresses).

### CI/CD Pipeline (GitHub Actions)

`.github/workflows/ci.yml` triggers on push to `main`:

```
┌────────────────────────────────────────────────────────┐
│  GitHub Actions Workflow                                │
├────────────────────────────────────────────────────────┤
│  1. Checkout code                                       │
│  2. Set up JDK 21                                       │
│  3. Cache Maven dependencies                            │
│  4. Run `mvn clean package -DskipTests`                 │
│     → Builds all services in dependency order           │
│                                                         │
│  5. Set up Docker Buildx (multi-platform support)       │
│  6. Login to Docker Hub                                 │
│                                                         │
│  7. For each service (parallel jobs):                   │
│     ├─ Build Docker image                               │
│     ├─ Tag as aakash354/delma-<service>:latest          │
│     ├─ Build for linux/amd64,linux/arm64                │
│     └─ Push to Docker Hub                               │
└────────────────────────────────────────────────────────┘
```

**Why multi-arch builds:** Developer machines (Apple Silicon `arm64`) and production servers (typically `amd64`) need the same images. Buildx creates a manifest that auto-selects the right architecture.

### Environment Variables (Production)

All secrets are injected via Docker Compose environment variables, never committed:

```yaml
environment:
  - JWT_SECRET=${JWT_SECRET}
  - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
  - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
  - AWS_S3_BUCKET=${AWS_S3_BUCKET}
  - RAZORPAY_KEY_ID=${RAZORPAY_KEY_ID}
  - RAZORPAY_KEY_SECRET=${RAZORPAY_KEY_SECRET}
  - GROQ_API_KEY=${GROQ_API_KEY}
  - MAIL_USERNAME=${MAIL_USERNAME}
  - MAIL_PASSWORD=${MAIL_PASSWORD}
```

`.env` is gitignored. `application.yml` files for services are also gitignored except for selected ones needed by CI (`!aiservice/src/main/resources/application.yml`, `!gateway/...`, `!paymentservice/...`).

---

## 12. Local Setup

### Option A: Docker Compose (Recommended)

```bash
# 1. Clone repo
git clone https://github.com/AakashTyagi354/delma2.0_spring_microservice.git
cd delma2.0_spring_microservice

# 2. Create .env file with required secrets
cp .env.example .env
# Edit .env and fill in your secrets

# 3. Start everything
docker-compose up -d

# 4. Verify
docker-compose ps
# All 13 services + Postgres + Redis + Kafka should be "Up"
```

Eureka dashboard at http://localhost:8761 confirms all services registered.

### Option B: Manual (For Development)

#### Prerequisites
- **Java 21** (LTS — required, Java 25 may have Lombok issues)
- **Maven 3.9+**
- **PostgreSQL 16** running on `localhost:5432`
- **Redis** (via Homebrew: `brew install redis`)
- **Apache Kafka** running on `localhost:9092`
- **Node.js 18+** (for frontend)

#### Database Setup
```sql
CREATE DATABASE delma_user_db;
CREATE DATABASE delma_doctor_db;
CREATE DATABASE delma_appointment_db;
CREATE DATABASE delma_payment_db;
CREATE DATABASE delma_document_db;
CREATE DATABASE delma_product_db;
CREATE DATABASE delma_category_db;
CREATE DATABASE delma_order_db;
CREATE DATABASE delma_notification_db;
-- Note: aiservice does not need a database
```

#### Build
```bash
mvn clean install -DskipTests
```

#### Startup Order (CRITICAL)
```bash
# 1. Discovery first
cd discovery-server && mvn spring-boot:run

# 2. Each microservice (separate terminals)
cd userservice && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd doctorservice && mvn spring-boot:run -Dspring-boot.run.profiles=local
# ... etc for each service
cd aiservice && mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. Gateway last
cd gateway && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Verify Setup
- Eureka Dashboard: http://localhost:8761
- Gateway: http://localhost:8089/auth/login
- AI Service: `POST http://localhost:8089/api/v1/ai/symptom-check` with `{"symptoms":"headache"}`

### Required Environment Variables
```bash
export JWT_SECRET=<your-secret>
export AWS_ACCESS_KEY_ID=<your-key>
export AWS_SECRET_ACCESS_KEY=<your-secret>
export AWS_S3_BUCKET=delma-patient-documents
export RAZORPAY_KEY_ID=<your-key>
export RAZORPAY_KEY_SECRET=<your-secret>
export RAZORPAY_WEBHOOK_SECRET=<your-secret>
export GROQ_API_KEY=<your-groq-key>
export MAIL_USERNAME=<your-gmail>
export MAIL_PASSWORD=<gmail-app-password-no-spaces>
```

---

## 13. Tech Stack

### Backend
| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 4.0.0 |
| Cloud | Spring Cloud | 2025.1.0 |
| API Gateway | Spring Cloud Gateway (WebFlux) | 5.0.0 |
| Service Discovery | Netflix Eureka | 2025.1.0 |
| Inter-Service | OpenFeign + Resilience4j | — |
| ORM | Hibernate | 7.1.x |
| Database | PostgreSQL | 16 |
| Caching | Redis | 7.x |
| Messaging | Apache Kafka | 3.x |
| Email | Spring Mail + Gmail SMTP | — |
| Build | Maven (multi-module) | 3.13 |
| Auth | JJWT (JSON Web Tokens) | 0.12.6 |

### Frontend
| Layer | Technology |
|-------|-----------|
| Framework | Next.js 14 (App Router) |
| Language | TypeScript |
| State | Redux Toolkit |
| Styling | Tailwind CSS + shadcn/ui |
| HTTP | Axios with interceptors |
| Hosting | Vercel (planned) |

### DevOps
| Tool | Purpose |
|------|---------|
| **Docker** | Containerization |
| **Docker Compose** | Local orchestration |
| **Docker Hub** | Image registry |
| **GitHub Actions** | CI/CD pipeline |
| **Docker Buildx** | Multi-arch builds (amd64 + arm64) |

### External Services
| Service | Purpose |
|---------|---------|
| **AWS S3** | Medical document storage with presigned URLs |
| **ZEGOCLOUD** | Video calls with AES-256 encryption |
| **Razorpay** | Payment gateway (orders, signature verification, refunds) |
| **Groq AI** | LLM inference (Llama 3.1) for symptom checker |
| **Gmail SMTP** | OTP email delivery |

---

## 14. Future Roadmap

### Short-term Improvements
- [x] ~~Fix double-booking race condition (optimistic locking on `DoctorSlot`)~~ ✅ Done
- [x] ~~AI Symptom Checker (Groq + Llama 3.1)~~ ✅ Done
- [x] ~~Email OTP verification~~ ✅ Done
- [x] ~~Dockerize all services~~ ✅ Done
- [x] ~~CI/CD pipeline (GitHub Actions)~~ ✅ Done
- [ ] Scheduled cleanup of unverified users (delete `is_verified=false` users older than 24h)
- [ ] Google OAuth integration
- [ ] Apply ApiResponse + exception handling pattern to remaining services
- [ ] Add Redis caching to product listings
- [ ] Add rate limiting on `/auth/login` to prevent brute force

### AI Features (planned)
- [x] ~~**Symptom Checker** — Groq AI suggests doctor specialization from user input~~ ✅ Done
- [ ] **Document Summarizer** — Auto-summarize uploaded medical documents for doctors before video call
- [ ] **Post-Consultation Notes** — AI-generated structured summary after video call
- [ ] **Smart Doctor Recommendations** — Rank doctors based on user history + ratings

### Production Readiness
- [x] ~~Dockerize all services~~ ✅ Done
- [x] ~~CI/CD pipeline (GitHub Actions)~~ ✅ Done
- [ ] AWS EC2 deployment with public URL
- [ ] Centralized config (Spring Cloud Config Server)
- [ ] Distributed tracing (Zipkin / OpenTelemetry)
- [ ] Centralized logging (ELK stack)
- [ ] Kubernetes deployment manifests
- [ ] Multi-instance deployment for horizontal scaling
- [ ] Architecture diagram with live URL in README

### Resume-Worthy Metrics (to add when production data exists)
- Concurrent users supported
- p99 latency under load
- Cache hit rate
- Number of services running
- AI inference latency (Groq currently sub-second)

---

## License

MIT License — see LICENSE file for details.

## Author

**Aakash Tyagi**
Full Stack Engineer | Specialist Programmer at Infosys
[GitHub](https://github.com/AakashTyagi354) · [LinkedIn](https://linkedin.com/in/aakashtyagi354)
