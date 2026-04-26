# Delma — Online Consulting Platform

> A microservices-based healthcare consultation platform built with Spring Boot, Spring Cloud, Next.js, Redis, Kafka, and AWS. Connects patients with doctors for video consultations, document sharing, and an integrated medical e-store.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-green)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.0-blue)](https://spring.io/projects/spring-cloud)
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
10. [Local Setup](#10-local-setup)
11. [Tech Stack](#11-tech-stack)
12. [Future Roadmap](#12-future-roadmap)

---

## 1. High-Level Overview

### What Delma Does

Delma is an end-to-end online consulting platform with four core capabilities:

1. **Doctor Onboarding & Discovery** — Users register, optionally apply to become doctors, get approved by admins, and appear in a searchable doctor directory.
2. **Appointment & Video Consultation** — Patients book paid appointments based on doctor availability, then join secure video calls via ZEGOCLOUD with AES-256 encryption.
3. **Medical Document Sharing** — Patients upload medical documents to AWS S3 with presigned URLs, accessible only by their assigned doctor.
4. **Medical E-Store** — End-to-end e-commerce for medications with categorized products, cart, payment via Razorpay, and order management.

### Why Microservices

A monolithic approach would couple unrelated concerns — patient bookings shouldn't be impacted by e-store outages. Each domain (users, doctors, appointments, payments, etc.) is isolated:

| Benefit | How Delma Achieves It |
|---------|----------------------|
| **Independent scaling** | Scale `appointmentservice` separately from `productservice` during peak booking hours |
| **Fault isolation** | If `notificationservice` crashes, video calls still work |
| **Independent deployment** | Update the e-store without touching consultation logic |
| **Technology flexibility** | Each service can use what fits — Postgres for users, MongoDB for documents in future |

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
│             │  │   + Redis   │  │              │  │              │  │   + AWS S3  │
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

┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│   productservice     │  │   categoryservice    │  │   orderservice       │
│       :8016          │  │       :8015          │  │       :8013          │
│   E-Store Products   │  │   Product Categories │  │   Order Management   │
└──────────────────────┘  └──────────────────────┘  └──────────────────────┘

External Services:
• ZEGOCLOUD (Video calls with AES-256)   • Razorpay (Payments)
• AWS S3 (Document storage)               • Apache Kafka (Async messaging)
```

### Architectural Patterns Used

| Pattern | Where It's Used | Why |
|---------|----------------|-----|
| **API Gateway** | Spring Cloud Gateway | Single entry point, JWT validation, routing |
| **Service Discovery** | Eureka | Services find each other by name, not IP |
| **Circuit Breaker** | Resilience4j on Feign clients | Prevent cascade failures |
| **Cache-Aside** | Redis on doctor listings | Reduce DB load, improve response times |
| **Event-Driven** | Kafka for notifications | Decouple notification logic from business logic |
| **CQRS-lite** | Separate read/write paths | Optimize each independently |
| **Shared Library** | `common-lib` module | DRY — exception handling, ApiResponse |
| **DTO Layer** | Every service | Never expose JPA entities directly |

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
- Public endpoints whitelist: `/auth/login`, `/auth/signup`, `/auth/refresh`

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
**Database:** PostgreSQL (`delma_user_db`)

**Responsibilities:**
- User registration & authentication
- JWT token generation (access + refresh)
- Role management (USER, DOCTOR, ADMIN)
- Doctor application submission
- Admin operations (approve doctors, manage users)
- Secure refresh token rotation

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

### 3.7 paymentservice

**Port:** 8083
**Database:** PostgreSQL (`delma_payment_db`)

**Responsibilities:**
- Razorpay payment order creation
- Payment verification via webhooks
- Refund processing
- Transaction history

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

### Why Both?

| Need | Use |
|------|-----|
| Need response to continue | Feign (sync) |
| Side effect, can be delayed | Kafka (async) |
| Other service must be available | Feign |
| Other service can be temporarily down | Kafka |

---

## 5. Core User Flows

### 5.1 User Registration & Login

```
┌─────────┐                ┌──────────┐                ┌──────────────┐
│ Browser │                │ Gateway  │                │ userservice  │
└────┬────┘                └────┬─────┘                └──────┬───────┘
     │                          │                             │
     │ POST /auth/signup        │                             │
     ├─────────────────────────>│                             │
     │                          │ POST /auth/signup           │
     │                          ├────────────────────────────>│
     │                          │                             │
     │                          │              Validate input │
     │                          │            Hash password    │
     │                          │           Save to DB        │
     │                          │ ApiResponse<SignupResponse> │
     │                          │<────────────────────────────│
     │ 200 OK + user data       │                             │
     │<─────────────────────────│                             │
     │                          │                             │
     │ POST /auth/login         │                             │
     ├─────────────────────────>│                             │
     │                          │ POST /auth/login            │
     │                          ├────────────────────────────>│
     │                          │              Verify pwd     │
     │                          │           Generate JWT      │
     │                          │           Generate refresh  │
     │                          │           Save refresh DB   │
     │                          │ JWT + refresh in cookie     │
     │                          │<────────────────────────────│
     │ JWT in body,             │                             │
     │ refresh in HttpOnly cookie                             │
     │<─────────────────────────│                             │
     │                          │                             │
```

### 5.2 Doctor Application Workflow

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

### 5.3 Appointment Booking + Video Call

```
1. User browses approved doctors
   GET /api/v1/doctor/all (cached in Redis)

2. User views doctor's available slots
   GET /api/v1/slots/{doctorId}

3. User selects slot, initiates payment
   POST /api/v1/payments/create-order
   → paymentservice creates Razorpay order
   → returns Razorpay order_id

4. User completes Razorpay payment
   Razorpay webhook → paymentservice
   → verifies signature
   → updates payment status

5. paymentservice → appointmentservice (Feign)
   → creates appointment (status: CONFIRMED)
   → publishes notification event

6. Both patient and doctor receive:
   - Email with appointment details
   - In-app notification

7. At appointment time:
   GET /api/v1/appointments/{id}/video-token
   → returns ZEGOCLOUD token (AES-256 encrypted session)

8. Both join video call
   Frontend uses ZEGOCLOUD SDK with token
```

### 5.4 Document Upload & Sharing

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

### 5.5 E-Store Order Flow

```
1. Browse products
   GET /api/v1/product?category=...

2. Add to cart (orderservice)
   POST /api/v1/orders/cart/add

3. View cart
   GET /api/v1/orders/cart/{userId}

4. Checkout — create payment
   POST /api/v1/payments/create-order
   → Razorpay order_id

5. Complete Razorpay payment
   → webhook to paymentservice

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
Create new user account.
```json
Request:  { "email": "user@example.com", "password": "secret123", "username": "John Doe" }
Response: { "success": true, "message": "Signup successful", "data": { "userId": 1, "email": "..." } }
```

#### `POST /auth/login`
Authenticate user, return JWT.
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
```json
Request:  { "specialization": "Cardiology", "experience": 5, ... }
Response: { "success": true, "message": "Doctor application submitted successfully" }
```

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

#### `PUT /api/v1/admin/add-role/doctor/{userId}` 🔒 ADMIN (internal)
Add DOCTOR role to user. Called by doctorservice via Feign.

---

### 6.2 doctorservice

#### `POST /api/v1/doctor/apply` 🔒 Auth required
Submit doctor application (called from userservice).
```json
Request: { "userId": "1", "specialization": "Cardiology", "experience": 5 }
```

#### `GET /api/v1/doctor/all` 🟢 Cached (Redis, 10 min TTL)
Get all approved doctors.
```json
Response: {
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": "1",
      "firstName": "John",
      "lastName": "Doe",
      "specialization": "Cardiology",
      "experience": 5,
      "feesPerConsultation": 500.0,
      "status": "APPROVED"
    }
  ]
}
```

#### `GET /api/v1/doctor/search/{keyword}`
Search approved doctors by name or specialization (DB-level filter).

#### `GET /api/v1/doctor/pending` 🔒 ADMIN, 🟢 Cached
Get pending doctor applications.

#### `PUT /api/v1/doctor/approve/{id}` 🔒 ADMIN, 🔄 Cache evicted
Approve a doctor application. Triggers:
- DB status update
- Cache eviction
- Feign call to userservice (add role)
- Kafka event for notification

#### `PUT /api/v1/doctor/reject/{id}` 🔒 ADMIN, 🔄 Cache evicted
Reject a doctor application.

---

### 6.3 appointmentservice

#### `POST /api/v1/slots/create` 🔒 DOCTOR
Create availability slot.
```json
Request: { "doctorId": 1, "startTime": "2026-05-01T10:00", "endTime": "2026-05-01T10:30" }
```

#### `GET /api/v1/slots/doctor/{doctorId}`
Get all available slots for a doctor.

#### `POST /api/v1/appointments/book` 🔒 USER
Book an appointment after payment.
```json
Request: { "slotId": 5, "patientId": 1, "paymentId": "razorpay_xxx" }
```

#### `GET /api/v1/appointments/user/{userId}` 🔒 Auth
Get user's appointments (as patient or doctor).

#### `GET /api/v1/appointments/{id}/video-token` 🔒 Auth
Generate ZEGOCLOUD video session token (AES-256 encrypted).

---

### 6.4 paymentservice

#### `POST /api/v1/payments/create-order`
Create Razorpay order for appointment or e-store.
```json
Request: { "amount": 50000, "currency": "INR", "type": "APPOINTMENT", "referenceId": 123 }
Response: { "success": true, "data": { "razorpayOrderId": "order_xxx", "amount": 50000 } }
```

#### `POST /api/v1/payments/webhook`
Razorpay webhook for payment status updates.

#### `POST /api/v1/payments/verify`
Verify payment signature client-side.

---

### 6.5 documentservice

#### `POST /api/v1/documents/upload` 🔒 Auth, multipart
Upload document to S3.
```json
Form: file (binary), userId (string)
Response: { "success": true, "data": { "name": "...", "url": "...", "type": "application/pdf" } }
```

#### `GET /api/v1/documents/getall-documents/{userId}` 🔒 Auth
Get user's documents with presigned S3 URLs.

#### `DELETE /api/v1/documents/delete-document/{id}` 🔒 Auth
Delete from both DB and S3.

---

### 6.6 productservice

#### `GET /api/v1/product` — List all products (paginated)
#### `GET /api/v1/product/{id}` — Get product details
#### `POST /api/v1/product` 🔒 ADMIN — Add product
#### `PUT /api/v1/product/{id}` 🔒 ADMIN — Update product
#### `DELETE /api/v1/product/{id}` 🔒 ADMIN — Delete product
#### `GET /api/v1/product/category/{categoryId}` — Filter by category

---

### 6.7 categoryservice

#### `GET /api/v1/category` — List all categories
#### `POST /api/v1/category` 🔒 ADMIN — Create category
#### `PUT /api/v1/category/{id}` 🔒 ADMIN — Update category
#### `DELETE /api/v1/category/{id}` 🔒 ADMIN — Delete category

---

### 6.8 orderservice

#### `POST /api/v1/orders/cart/add` 🔒 USER — Add item to cart
#### `GET /api/v1/orders/cart/{userId}` 🔒 USER — Get cart
#### `DELETE /api/v1/orders/cart/{userId}/item/{productId}` 🔒 USER — Remove item
#### `POST /api/v1/orders/checkout` 🔒 USER — Place order
#### `GET /api/v1/orders/user/{userId}` 🔒 USER — Order history
#### `PUT /api/v1/orders/{id}/status` 🔒 ADMIN — Update order status

---

### 6.9 notificationservice

#### `GET /api/v1/notifications/{userId}` 🔒 Auth — Get user's notifications
#### `PUT /api/v1/notifications/{id}/read` 🔒 Auth — Mark as read

---

## 7. Data Models

### 7.1 User Entity (userservice)
```
User
├── id: Long (PK)
├── username: String
├── email: String (unique)
├── password: String (BCrypt hashed)
├── roles: Set<Role>          [USER, DOCTOR, ADMIN]
├── isDoctor: String
├── isAdmin: String
└── createdAt: LocalDateTime

RefreshToken
├── id: Long (PK)
├── userId: Long (FK)
├── token: String
├── expiresAt: LocalDateTime
└── createdAt: LocalDateTime
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
├── patientId: Long
├── doctorId: Long
├── paymentId: String
├── status: AppointmentStatus [CONFIRMED, COMPLETED, CANCELLED]
├── videoSessionId: String
└── createdAt: LocalDateTime
```

### 7.4 Document Entity (documentservice)
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

### 7.5 Order & Product (e-store)
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

OrderItem
├── id: Long (PK)
├── orderId: Long (FK)
├── productId: Long
├── quantity: Integer
└── priceAtPurchase: BigDecimal
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
(/auth/login, /auth/signup, /auth/refresh)
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

---

## 10. Local Setup

### Prerequisites
- **Java 21** (LTS — required, Java 25 may have Lombok issues)
- **Maven 3.9+**
- **PostgreSQL 16** running on `localhost:5432`
- **Redis** (via Homebrew: `brew install redis`)
- **Apache Kafka** running on `localhost:9092`
- **Node.js 18+** (for frontend)

### Database Setup
Create databases for each service:
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
```

### Build Everything
```bash
mvn clean install -DskipTests
```

### Startup Order (CRITICAL)
```bash
# Terminal 1: Eureka first (services need to register)
cd discovery-server && mvn spring-boot:run

# Terminal 2: Redis (already running via brew services)
brew services start redis

# Terminal 3: Kafka (start zookeeper first if needed)
kafka-server-start /opt/homebrew/etc/kafka/server.properties

# Terminals 4-13: Each microservice
cd userservice && mvn spring-boot:run
cd doctorservice && mvn spring-boot:run
cd appointmentservice && mvn spring-boot:run
cd paymentservice && mvn spring-boot:run
cd documentservice && mvn spring-boot:run
cd productservice && mvn spring-boot:run
cd categoryservice && mvn spring-boot:run
cd orderservice && mvn spring-boot:run
cd notificationservice && mvn spring-boot:run

# Terminal 14: Gateway last
cd gateway && mvn spring-boot:run
```

### Verify Setup
- Eureka Dashboard: http://localhost:8761
- Gateway: http://localhost:8089/auth/login
- Each service: `http://localhost:<port>/actuator/health`

### Environment Variables
```bash
export JWT_SECRET=<your-secret>
export AWS_ACCESS_KEY=<your-key>
export AWS_SECRET_KEY=<your-secret>
export AWS_S3_BUCKET=delma-documents
export RAZORPAY_KEY_ID=<your-key>
export RAZORPAY_KEY_SECRET=<your-secret>
```

---

## 11. Tech Stack

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
| Build | Maven (multi-module) | 3.13 |
| Auth | JJWT (JSON Web Tokens) | 0.12.6 |

### Frontend
| Layer | Technology |
|-------|-----------|
| Framework | Next.js 14 (App Router) |
| Language | TypeScript |
| State | Redux Toolkit |
| Styling | Tailwind CSS + shadcn/ui |
| Auth | Firebase + JWT cookies |
| Hosting | Vercel |

### External Services
| Service | Purpose |
|---------|---------|
| **AWS S3** | Medical document storage with presigned URLs |
| **ZEGOCLOUD** | Video calls with AES-256 encryption |
| **Razorpay** | Payment gateway (orders, webhooks, refunds) |

---

## 12. Future Roadmap

### Short-term Improvements
- [ ] Fix double-booking race condition (optimistic locking on `DoctorSlot`)
- [ ] Apply ApiResponse + exception handling pattern to remaining services
- [ ] Add Redis caching to product listings
- [ ] Add rate limiting on `/auth/login` to prevent brute force

### AI Features (planned)
- [ ] **Symptom Checker** — Claude API suggests doctor specialization from user input
- [ ] **Document Summarizer** — Auto-summarize uploaded medical documents for doctors before video call
- [ ] **Post-Consultation Notes** — AI-generated structured summary after video call
- [ ] **Smart Doctor Recommendations** — Rank doctors based on user history + ratings

### Production Readiness
- [ ] Centralized config (Spring Cloud Config Server)
- [ ] Distributed tracing (Zipkin / OpenTelemetry)
- [ ] Centralized logging (ELK stack)
- [ ] Dockerize all services
- [ ] Kubernetes deployment manifests
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Multi-instance deployment for horizontal scaling

### Resume-Worthy Metrics (to add when production data exists)
- Concurrent users supported
- p99 latency under load
- Cache hit rate
- Number of services running

---

## License

MIT License — see LICENSE file for details.

## Author

**Aakash Tyagi**
Full Stack Engineer | Specialist Programmer at Infosys
[GitHub](https://github.com/AakashTyagi354) · [LinkedIn](https://linkedin.com/in/aakashtyagi354)
