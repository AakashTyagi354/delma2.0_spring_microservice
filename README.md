# Delma — Online Consulting Platform

> A microservices-based healthcare consultation platform built with Spring Boot, Spring Cloud, Next.js, Redis, Kafka, AWS, and Groq AI. Connects patients with doctors for video consultations, document sharing, AI-powered symptom analysis, RAG-based document summarization, and a conversational MCP booking agent, plus an integrated medical e-store.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-green)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.0-blue)](https://spring.io/projects/spring-cloud)
[![pgvector](https://img.shields.io/badge/pgvector-0.8-336791)](https://github.com/pgvector/pgvector)
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
6. [AI Features Deep Dive](#6-ai-features-deep-dive)
7. [API Reference](#7-api-reference)
8. [Data Models](#8-data-models)
9. [Caching Strategy](#9-caching-strategy)
10. [Authentication & Security](#10-authentication--security)
11. [Engineering Challenges & Solutions](#11-engineering-challenges--solutions)
12. [Deployment (Docker + CI/CD)](#12-deployment-docker--cicd)
13. [Local Setup](#13-local-setup)
14. [Tech Stack](#14-tech-stack)
15. [Future Roadmap](#15-future-roadmap)

---

## 1. High-Level Overview

### What Delma Does

Delma is an end-to-end online consulting platform with seven core capabilities:

1. **Doctor Onboarding & Discovery** — Users register with email OTP verification, optionally apply to become doctors, get approved by admins, and appear in a searchable doctor directory.
2. **AI Symptom Checker** — Patients describe symptoms in plain English; Groq AI recommends the right specialization and auto-filters the doctor listing.
3. **AI Document Summarizer (RAG)** — Patient medical PDFs are indexed in pgvector using Voyage AI embeddings. When a doctor opens a patient profile, top-relevant chunks are retrieved and summarized by Groq LLaMA 3.1 into a structured 3-point clinical summary.
4. **AI Booking Agent (MCP)** — Patients book appointments through natural language conversation. The agent uses the ReAct pattern with tool calling to orchestrate doctor search, slot lookup, and booking across multiple microservices.
5. **Appointment & Video Consultation** — Patients book paid appointments based on doctor availability, then join secure video calls via ZEGOCLOUD with AES-256 encryption.
6. **Medical Document Sharing** — Patients upload medical documents to AWS S3 with presigned URLs, accessible only by their assigned doctor.
7. **Medical E-Store** — End-to-end e-commerce for medications with categorized products, cart, payment via Razorpay, and order management.

### Why Microservices

A monolithic approach would couple unrelated concerns — patient bookings shouldn't be impacted by e-store outages. Each domain (users, doctors, appointments, payments, AI, etc.) is isolated:

| Benefit | How Delma Achieves It |
|---------|----------------------|
| **Independent scaling** | Scale `appointmentservice` separately from `productservice` during peak booking hours |
| **Fault isolation** | If `notificationservice` or `aiservice` crashes, video calls still work |
| **Independent deployment** | Update the e-store without touching consultation logic |
| **Technology flexibility** | Each service can use what fits — Postgres for users, pgvector for AI embeddings |

---

## 2. System Architecture

### Architecture Diagram

```
                         ┌──────────────────────────┐
                         │    Frontend (Next.js)     │
                         │    React + TypeScript     │
                         │    Redux + Tailwind CSS   │
                         │    + AIBookingChat        │
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
│  (OTP)      │  │  (Cache)    │  │              │  │              │  │   + Kafka   │
└──────┬──────┘  └──────┬──────┘  └──────┬───────┘  └──────┬───────┘  └─────┬───────┘
       │                │                │                 │                │
       └────────────────┴────────┬───────┴─────────────────┘                │
                                 │ Apache Kafka                             │
                                 ▼                                          │
                         ┌──────────────────────────┐                       │
                         │   notificationservice     │                       │
                         │        :8017              │                       │
                         │   (Email + Real-time)     │                       │
                         └──────────────────────────┘                       │
                                                                            │
       ┌─────────────────┬────────────────┬────────────────┐                │
       │                 │                │                │                │
       ▼                 ▼                ▼                ▼                │
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  aiservice   │  │productservice│  │categoryservice│ │ orderservice │     │
│    :8095     │  │    :8016     │  │    :8015     │  │    :8013     │     │
│              │  │              │  │              │  │              │     │
│ ┌──────────┐ │  │  PostgreSQL  │  │  PostgreSQL  │  │  PostgreSQL  │     │
│ │ Symptom  │ │  │  (E-Store)   │  │  (Categories)│  │  (Orders)    │     │
│ │ Checker  │ │  └──────────────┘  └──────────────┘  └──────────────┘     │
│ ├──────────┤ │                                                            │
│ │ RAG      │◄┼────── consumes document-uploaded Kafka events ─────────────┘
│ │ Indexer  │ │
│ ├──────────┤ │
│ │ MCP      │ │   Feign → doctorservice, appointmentservice, paymentservice
│ │ Agent    │ │   Redis → slot session storage
│ └──────────┘ │   pgvector + Voyage AI embeddings
│              │   Groq qwen3-32b / llama-3.1-8b
└──────────────┘

External Services:
• ZEGOCLOUD (Video calls with AES-256)   • Razorpay (Payments)
• AWS S3 (Document storage)               • Apache Kafka (Async messaging)
• Groq AI (LLM inference)                 • Voyage AI (Embeddings)
• Gmail SMTP (OTP emails)                 • pgvector (Vector DB extension)
```

### Architectural Patterns Used

| Pattern | Where It's Used | Why |
|---------|----------------|-----|
| **API Gateway** | Spring Cloud Gateway | Single entry point, JWT validation, routing |
| **Service Discovery** | Eureka | Services find each other by name, not IP |
| **Circuit Breaker** | Resilience4j on Feign clients | Prevent cascade failures |
| **Cache-Aside** | Redis on doctor listings, OTP storage | Reduce DB load, ephemeral data with TTL |
| **Event-Driven** | Kafka for notifications + RAG indexing | Decouple notification logic from business logic |
| **CQRS-lite** | Separate read/write paths | Optimize each independently |
| **Shared Library** | `common-lib` module | DRY — exception handling, ApiResponse |
| **DTO Layer** | Every service | Never expose JPA entities directly |
| **ReAct Agent** | aiservice MCP booking agent | LLM reasons → acts → observes → loops |
| **RAG Pattern** | aiservice document summarizer | Retrieve relevant chunks, augment LLM context |
| **Session State** | Redis for MCP agent slot context | Bridge stateless LLM across conversation turns |

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
- **Publishes `document-uploaded` Kafka event** for async RAG indexing by aiservice

### 3.9 productservice / 3.10 categoryservice / 3.11 orderservice

E-store microservices — product catalog, categories, cart, and order management. Standard CRUD with PostgreSQL. Each has its own database following the database-per-service pattern.

### 3.12 notificationservice

**Port:** 8017
**Tech:** Kafka consumer + Gmail SMTP

**Responsibilities:**
- Listen to Kafka `notification-events` topic
- Send emails via SMTP
- Real-time push notifications
- Notification history persistence

### 3.13 aiservice — the heart of Delma's AI capabilities

**Port:** 8095
**Tech:** Spring Boot 4 + Groq API + Voyage AI + pgvector + Redis + Kafka

This single service hosts **three distinct AI features**, each demonstrating a different LLM integration pattern:

| Feature | Pattern | Model | Storage |
|---------|---------|-------|---------|
| Symptom Checker | Simple LLM call | Groq llama-3.1-8b | Stateless |
| Document Summarizer | RAG (Retrieval-Augmented Generation) | Voyage AI + Groq llama-3.1-8b | pgvector |
| MCP Booking Agent | ReAct (tool-calling agent loop) | Groq qwen3-32b | Redis sessions |

See [Section 6 — AI Features Deep Dive](#6-ai-features-deep-dive) for full implementation details.

**Database:** PostgreSQL (`delma_ai_db`) with pgvector extension for document embeddings
**Kafka:** Consumes `document-uploaded` topic for async RAG indexing
**Feign Clients:** `DoctorClient`, `AppointmentClient`, `PaymentClient` — orchestrated by MCP agent
**Redis:** Stores slot session context for the MCP agent (bridges LLM statelessness)

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

**Heavy use in aiservice:** The MCP agent's `ToolExecutor` makes Feign calls to doctorservice, appointmentservice, and paymentservice as the LLM requests tools.

### Asynchronous (Kafka)

Used when the action doesn't need immediate response — fire and forget.

**Example 1 — Notifications:** Sending notifications doesn't block the approval flow.

```
doctorservice → publishes NotificationEvent to Kafka topic
              → returns immediately to client
                    │
                    ▼
notificationservice ← consumes from Kafka
                    → sends email + push notification
```

**Example 2 — RAG Document Indexing:** PDF upload doesn't block on slow embedding generation.

```
documentservice → uploads PDF to S3
                → saves metadata to DB
                → publishes document-uploaded event
                → returns 200 to user immediately
                    │
                    ▼
aiservice ← consumes document-uploaded event
          → downloads PDF from S3
          → extracts text, chunks, embeds via Voyage AI
          → stores vectors in pgvector
          (takes ~4 minutes per 10-page PDF due to Voyage rate limit)
```

**Topics:**
- `notification-events` — for user-facing notifications
- `document-uploaded` — triggers async RAG indexing

### External API Calls (REST)

- **aiservice → Groq API:** LLM inference for all three AI features
- **aiservice → Voyage AI:** Vector embeddings for RAG indexing
- **documentservice → AWS S3:** Document storage
- **paymentservice → Razorpay:** Payment orders and verification

### Why Multiple Communication Patterns?

| Need | Use |
|------|-----|
| Need response to continue | Feign (sync) |
| Side effect, can be delayed | Kafka (async) |
| External third-party API | RestTemplate |
| Long-running task (embedding) | Kafka (don't block user) |
| Real-time chat (agent) | Sync REST + Redis session |

---

## 5. Core User Flows

### 5.1 User Registration with Email OTP

```
Browser → Gateway → userservice → Redis
   POST /auth/signup
   {name, email, password}
       │
       ▼
   • Validate input
   • Save user with is_verified=false
   • Generate 6-digit OTP
   • Store in Redis (TTL: 10 min)
   • Send OTP via Gmail SMTP
       │
       ▼
   "OTP sent to your email"

   POST /auth/verify-otp
   {email, otp}
       │
       ▼
   • GET otp:email from Redis
   • Match → set is_verified=true
   • DELETE otp:email
       │
       ▼
   "Email verified, please login"

   POST /auth/login (now works)
       │
       ▼
   JWT + refresh cookie returned
```

### 5.2 AI Symptom Checker Flow

```
Browser → Gateway → aiservice → Groq API
   POST /api/v1/ai/symptom-check
   {"symptoms": "chest pain, shortness of breath"}
        │
        ▼
   Build structured prompt with strict JSON format
        │
        ▼
   POST /openai/v1/chat/completions
   model: llama-3.1-8b-instant
        │
        ▼
   Parse JSON response
   Fallback to "General Medicine" if parsing fails
        │
        ▼
   {specialization, message, disclaimer}
        │
        ▼
   Frontend calls /api/v1/doctor/search/{specialization}
   → filtered doctor list shown
```

### 5.3 AI Document Summarizer (RAG) Flow

```
INDEXING (async, runs in background)
────────────────────────────────────

Patient uploads PDF
        │
        ▼
documentservice → S3 + DB + Kafka publish
        │
        ▼
aiservice consumes document-uploaded event
        │
        ▼
1. Download PDF from S3
2. Extract text via Apache PDFBox 3.x
3. Split into 500-char overlapping chunks (100 char overlap)
4. For each chunk:
   - Call Voyage AI embeddings API (voyage-3-lite, 512-dim)
   - Sleep 21 seconds between calls (3 RPM rate limit)
5. Store all embeddings in pgvector with user_id filter

QUERYING (real-time, when doctor opens patient profile)
───────────────────────────────────────────────────────

Doctor opens patient profile
        │
        ▼
GET /api/v1/ai/summarize/{userId}
        │
        ▼
1. Embed query: "patient medical health condition report findings"
2. pgvector cosine search WHERE user_id={userId}
   ORDER BY embedding <=> query_vector LIMIT 5
3. Top 5 relevant chunks retrieved
4. Build prompt with chunks as context
5. Call Groq llama-3.1-8b for summarization
        │
        ▼
Returns 3-point clinical summary:
1. Main condition or reason for visit
2. Key symptoms or findings
3. Current medications or treatment plan
```

### 5.4 AI Booking Agent (MCP) Flow

```
Turn 1 ────────────────────────────────────────────────────
User: "Book me an orthopedics doctor for tomorrow"
        │
        ▼
AgentLoop.run() — ReAct pattern, max 10 iterations

Iteration 1:
  LLM decides → call search_doctors("orthopedics")
  Feign → doctorservice → Dr. Manju (doctorId: 3)

Iteration 2:
  LLM decides → call get_available_slots(doctorId=3, date=tomorrow)
  Feign → appointmentservice → 5 real slots
  STORE in Redis: {doctorId: 3, slots: [...]} TTL 10 min

Iteration 3:
  LLM has enough context → returns to user
  "Available slots: 10:30 AM, 11:00 AM, 11:30 AM..."

Turn 2 ────────────────────────────────────────────────────
User: "11:00 AM"
        │
        ▼
AgentLoop.run() — fresh request, no LLM memory of slot IDs

BUT: Redis injection reads {doctorId:3, slots:[...]} from previous turn
     and injects as system message at index 1

Iteration 1:
  LLM has real context from Redis injection
  Calls book_appointment(doctorId=3, slotId=9)  ← real integers
  Feign → appointmentservice → BOOKED ✅
  Clear Redis session

Iteration 2:
  "Appointment confirmed for tomorrow 11:00 AM"
```

### 5.5 Doctor Application Workflow

```
USER FLOW                ADMIN FLOW              POST-APPROVAL
─────────                ──────────              ─────────────
User applies         Admin approves
     │                   │
     ▼                   ▼
userservice          userservice
saves application    Feign → doctorservice
                          │
                          ▼
                     doctorservice:
                     1. Status APPROVED
                     2. Evict Redis cache
                     3. Feign → userservice (add DOCTOR role)
                     4. Publish Kafka event ──► notificationservice
                                                sends email + push
```

### 5.6 Appointment Booking + Video Call

```
1. User browses approved doctors (cached in Redis)
   OR uses AI symptom checker
   OR uses MCP booking agent

2. View slots
   GET /api/v1/appointments/slots?doctorId=X&date=Y

3. Create payment
   POST /api/v1/payments/create → Razorpay order

4. User pays in Razorpay modal

5. Verify payment signature
   POST /api/v1/payments/verify

6. Book appointment
   POST /api/v1/appointments/book
   → Optimistic locking on slot
   → Kafka event → email + push notification

7. At appointment time
   GET /api/v1/appointments/{id}/video-token
   → ZEGOCLOUD AES-256 encrypted session
```

### 5.7 Document Upload & Sharing

```
PATIENT UPLOADS                          DOCTOR VIEWS
───────────────                          ────────────

POST /api/v1/documents/upload
        │
        ▼
documentservice:
  1. Upload to S3              GET /api/v1/documents/getall-documents/{userId}
  2. Save metadata                       │
  3. Publish Kafka event                 ▼
                              documentservice:
                                1. Verify access
                                2. Generate presigned URLs (10 min TTL)
                                3. Return list

                              Doctor also sees AI Summary Card
                              calling /api/v1/ai/summarize/{userId}
```

### 5.8 E-Store Order Flow

```
Browse → Cart → Checkout → Razorpay → Verify → Order created
                                          │
                                          ▼
                                paymentservice → orderservice (Feign)
                                → status PLACED → SHIPPED → DELIVERED
```

---

## 6. AI Features Deep Dive

Delma's `aiservice` integrates three distinct AI patterns into a single Spring Boot service. Each demonstrates a different way of integrating LLMs into a production microservices architecture.

### 6.1 AI Symptom Checker — Simple LLM Call

**Pattern:** Stateless LLM inference with structured output
**Model:** Groq `llama-3.1-8b-instant`
**Why this pattern:** Single-shot classification problem, no context needed across calls.

#### How It Works

```java
// Build a structured prompt with strict JSON format requirement
String prompt = """
    You are a medical triage assistant.
    Given symptoms, return ONLY valid JSON in this exact shape:
    {"specialization": "...", "message": "...", "disclaimer": "..."}
    
    Specialization must be one of: Cardiology, Dermatology, 
    Orthopedics, General Medicine, Pediatrics, ...
    
    Symptoms: %s
    """.formatted(request.symptoms());
```

#### Key Decisions

- **Fixed specialization list** in prompt → output constrained to platform's actual doctor categories
- **JSON parsing with fallback** → if Groq returns malformed JSON, defaults to "General Medicine"
- **Public endpoint** (no JWT) → symptom analysis is non-sensitive, reduces friction

#### Frontend Integration

```typescript
// After symptom check, auto-filter doctor list
const { specialization } = response.data;
router.push(`/doctors?specialty=${specialization}`);
```

---

### 6.2 AI Document Summarizer — RAG Pattern

**Pattern:** Retrieval-Augmented Generation
**Models:** Voyage AI `voyage-3-lite` (embeddings) + Groq `llama-3.1-8b-instant` (summarization)
**Storage:** PostgreSQL with pgvector extension
**Why this pattern:** Medical documents can be 10+ pages; can't fit in LLM context. RAG retrieves only the relevant portions.

#### Indexing Pipeline (Async via Kafka)

```
PDF uploaded → documentservice → Kafka event → aiservice consumer
                                                     │
                                                     ▼
1. Download PDF from S3
2. Extract text with Apache PDFBox 3.x
3. Chunk text: 500 characters with 100-character overlap
   (overlap prevents losing context at chunk boundaries)
4. For each chunk: call Voyage AI → get 512-dim vector
5. Store in pgvector with user_id filter
```

**Why Kafka here?** A 10-page PDF generates ~11 chunks. Each Voyage AI call takes 1s + we sleep 21s between calls (3 RPM rate limit on free tier). Total indexing time: ~4 minutes. Blocking the upload API for 4 minutes would be terrible UX. Kafka decouples upload from indexing.

#### Storage Schema (pgvector)

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id BIGINT NOT NULL,
    user_id VARCHAR NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(512),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX ON document_embeddings
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

#### Query Pipeline (Real-time)

When a doctor opens a patient profile:

```java
// 1. Embed the query
String query = "patient medical health condition report findings";
float[] queryEmbedding = embeddingService.embed(query);

// 2. Cosine similarity search filtered by user_id
@Query(value = """
    SELECT chunk_text FROM document_embeddings
    WHERE user_id = :userId
    ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
    LIMIT 5
    """, nativeQuery = true)
List<String> findTopKChunksByUserId(...);

// 3. Build prompt with top-5 chunks as context
String context = String.join("\n---\n", relevantChunks);
String prompt = """
    Based on the following medical documents, provide a 
    concise 3-point summary covering:
    1. Main condition or reason for visit
    2. Key symptoms or findings
    3. Current medications or treatment plan
    
    Documents: %s
    """.formatted(context);

// 4. Call Groq for summarization
String summary = callGroq(prompt);
```

#### Result: Doctor sees structured summary

```
1. Main condition: Unstable Angina (ICD-10: I20.0)
2. Key symptoms: Chest pain radiating to left arm, SOB on exertion,
                 BP 145/90 mmHg, mild fever 100.2°F
3. Medications: Aspirin 75mg, Atorvastatin 40mg, Metoprolol 25mg,
                Ramipril 5mg, Nitroglycerine PRN
```

---

### 6.3 MCP AI Booking Agent — ReAct Pattern

**Pattern:** Reasoning + Acting (ReAct) with tool calling
**Model:** Groq `qwen/qwen3-32b` (better instruction following than smaller models)
**Storage:** Redis (slot session state)
**Why this pattern:** Booking requires multi-step orchestration across microservices. A single LLM call can't do it — needs to search → fetch → confirm → book.

#### Architecture

```
User chat input
       │
       ▼
AgentLoop (orchestrator, max 10 iterations)
       │
       ├──► System prompt + history + Redis context injection
       │
       ├──► Call Groq with messages + tool definitions
       │
       ├──► If finish_reason = "tool_calls":
       │      ├── Execute tool via Feign
       │      ├── Add tool result to messages
       │      └── Loop back
       │
       └──► If finish_reason = "stop":
              └── Return final answer to user
```

#### The 5 Tools

Each tool maps to a real microservice endpoint:

| Tool | Calls | Purpose |
|------|-------|---------|
| `search_doctors(keyword)` | doctorservice :8010 | Find doctors by specialization |
| `get_available_slots(doctorId, date)` | appointmentservice :8012 | Fetch real slots + cache in Redis |
| `book_appointment(doctorId, slotId)` | appointmentservice :8012 | Create appointment in DB |
| `get_my_appointments()` | appointmentservice :8012 | List user's bookings |
| `create_payment_order(...)` | paymentservice :8083 | Create Razorpay order |

#### Tool Definition Format (OpenAI-compatible)

```java
ToolDefinition.builder()
    .type("function")
    .function(FunctionDef.builder()
        .name("search_doctors")
        .description("Search for doctors by specialization. " +
                     "Returns doctorId (integer) for use in other tools.")
        .parameters(Map.of(
            "type", "object",
            "properties", Map.of(
                "keyword", Map.of(
                    "type", "string",
                    "description", "Specialization e.g. cardiology, orthopedics"
                )
            ),
            "required", List.of("keyword")
        ))
        .build())
    .build();
```

#### The Hardest Problem: LLM Statelessness Between Turns

When the user said "11am please" in a new request, the LLM had no slot IDs from the previous turn — they were only in the response text, which was already lost. The model hallucinated `doctorId: 123, slotId: 456`. Groq returned 400 errors. Bookings failed.

**The fix: Redis Session Storage**

```java
// After get_available_slots executes — store in Redis
slotSessionStore.storeSlots(userId, doctorId, slots);

// Before next LLM call — inject as system message
private void injectSlotContext(List<Map<String, Object>> messages, String userId) {
    Map<String, Object> session = slotSessionStore.getSession(userId);
    if (session == null) return;
    
    String contextMsg = String.format(
        "INJECTED CONTEXT: doctorId=%s, slots=%s. Use these exact integers.",
        session.get("doctorId"), session.get("slots")
    );
    
    Map<String, Object> contextInjection = new HashMap<>();
    contextInjection.put("role", "system");
    contextInjection.put("content", contextMsg);
    messages.add(1, contextInjection);  // right after main system prompt
}
```

Now the LLM always has real integer IDs in context, regardless of which conversation turn it is. Hallucinations eliminated.

#### Error Handling: 400 Correction Loop

When Groq returns 400 (LLM used wrong types), don't crash — inject a correction and retry:

```java
} else if (status == 400) {
    Map<String, Object> correction = new HashMap<>();
    correction.put("role", "user");
    correction.put("content",
        "You must call search_doctors first, then get_available_slots " +
        "to get real integer IDs. Restart from search_doctors.");
    messages.add(correction);
    return null;  // signal caller to retry with updated messages
}
```

#### Frontend Integration

```tsx
// Floating chat button in app/LayoutWrapper.tsx
{user && !user.isAdmin && !user.isDoctor && <AIBookingChat />}

// Component sends message + history each turn
const res = await axiosInstance.post(
  "/api/v1/ai/agent/chat",
  { message: userInput, conversationHistory: historyRef.current },
  { headers: { Authorization: `Bearer ${token}` } }
);

// If actionType === "PAYMENT_REQUIRED", auto-trigger Razorpay popup
```

---

## 7. API Reference

### 7.1 userservice (Public + Auth Required)

#### `POST /auth/signup`
```json
Request:  { "name": "John", "email": "user@example.com", "password": "secret123" }
Response: { "success": true, "message": "OTP sent to your email" }
```

#### `POST /auth/verify-otp`
```json
Request:  { "email": "user@example.com", "otp": "123456" }
Response: { "success": true, "message": "Email verified" }
```

#### `POST /auth/resend-otp`
```json
Request:  { "email": "user@example.com" }
Response: { "success": true, "message": "OTP resent" }
```

#### `POST /auth/login`
```json
Request:  { "email": "user@example.com", "password": "secret123" }
Response: { "success": true, "data": { "jwtToken": "...", "userId": 1, "role": "USER" } }
Headers:  Set-Cookie: refreshToken=...; HttpOnly
```

#### `POST /auth/refresh`
```json
Cookie:   refreshToken=...
Response: { "success": true, "data": { "accessToken": "..." } }
```

Plus: `/auth/logout`, `/auth/admin-login`, `/api/users/{id}`, `/api/users/apply-doctor`, admin endpoints under `/api/v1/admin/**`.

### 7.2 doctorservice

- `GET /api/v1/doctor/all` 🟢 Cached
- `GET /api/v1/doctor/search/{keyword}`
- `GET /api/v1/doctor/pending` 🔒 ADMIN, 🟢 Cached
- `PUT /api/v1/doctor/approve/{id}` 🔒 ADMIN, 🔄 Cache evicted
- `PUT /api/v1/doctor/reject/{id}` 🔒 ADMIN, 🔄 Cache evicted

### 7.3 appointmentservice

- `GET /api/v1/appointments/slots?doctorId=X&date=Y`
- `POST /api/v1/appointments/book?userId=X&doctorId=Y&slotId=Z` 🔒 USER
- `GET /api/v1/appointments/user?userId=X` 🔒 Auth
- `GET /api/v1/appointments/doctor?doctorId=X` 🔒 Auth
- `GET /api/v1/appointments/{id}/video-token` 🔒 Auth

### 7.4 paymentservice

- `POST /api/v1/payments/create` → Razorpay order
- `POST /api/v1/payments/verify` → Signature verification
- `POST /api/v1/payments/webhook` → Razorpay callbacks

### 7.5 documentservice

- `POST /api/v1/documents/upload` 🔒 Auth, multipart → publishes Kafka event
- `GET /api/v1/documents/getall-documents/{userId}` 🔒 Auth
- `DELETE /api/v1/documents/delete-document/{id}` 🔒 Auth

### 7.6 aiservice — Three AI Endpoints

#### `POST /api/v1/ai/symptom-check`
Analyzes symptoms and returns specialization recommendation.
```json
Request:  { "symptoms": "chest pain and shortness of breath" }
Response: {
  "success": true,
  "data": {
    "specialization": "Cardiology",
    "message": "Based on your symptoms, consult a cardiologist.",
    "disclaimer": "This is not medical advice."
  }
}
```

#### `GET /api/v1/ai/summarize/{userId}` 🔒 Auth (Doctor)
Generates RAG-based summary of patient's uploaded medical documents.
```json
Response: {
  "success": true,
  "data": "1. Main condition: Unstable Angina (ICD-10: I20.0). 
           2. Key symptoms: Chest pain, SOB on exertion, BP 145/90.
           3. Medications: Aspirin 75mg, Atorvastatin 40mg, Metoprolol 25mg."
}
```

#### `POST /api/v1/ai/agent/chat` 🔒 Auth (User)
MCP booking agent — multi-turn conversation that books appointments.
```json
Request: {
  "message": "Book me an orthopedics doctor for tomorrow",
  "conversationHistory": [...]
}
Response: {
  "success": true,
  "data": {
    "message": "Available slots for Dr. Manju: 10:30, 11:00, 11:30. Which?",
    "actionTaken": false,
    "actionType": "NONE"
  }
}
```

### 7.7 E-store endpoints

Standard CRUD on `/api/v1/product/**`, `/api/v1/category/**`, `/api/v1/orders/**`.

---

## 8. Data Models

### 8.1 User Entity (userservice)
```
User
├── id, username, email (unique)
├── password (BCrypt)
├── isVerified: Boolean    [false until OTP verified]
├── roles: Set<Role>       [USER, DOCTOR, ADMIN]
└── createdAt: LocalDateTime

OTP (Redis)
├── key: "otp:{email}"
├── value: 6-digit code
└── TTL: 10 minutes
```

### 8.2 Doctor Entity (doctorservice)
```
Doctor
├── id, userId (FK)
├── firstName, lastName, email, phone
├── specialization, experience, feesPerConsultation
└── status: [PENDING, APPROVED, REJECTED]
```

### 8.3 Appointment Entities
```
DoctorSlot
├── id, doctorId, startTime, endTime
├── status: [AVAILABLE, BOOKED, BLOCKED]
└── version: Long          [Optimistic Locking]

Appointment
├── id, slotId, userId, doctorId
├── status: [BOOKED, COMPLETED, CANCELLED]
└── createdAt: LocalDateTime
```

### 8.4 Payment Entity (paymentservice)
```
Payment
├── id, userId, refId, sourceType
├── razorpayOrderId, razorpayPaymentId, amount
├── status: [CREATED, SUCCESS, FAILED]
└── createdAt: LocalDateTime
```

### 8.5 Document Entity (documentservice)
```
Document
├── id, name, type (MIME), userId
├── filePath (S3 key), url (S3 URL)
└── uploadedAt: LocalDateTime
```

### 8.6 AI Service Data Models

```
SymptomRequest
└── symptoms: String

SymptomResponse
├── specialization: String
├── message: String
└── disclaimer: String

DocumentEmbedding (pgvector — delma_ai_db)
├── id: UUID (PK)
├── documentId: Long
├── userId: String
├── chunkIndex: Integer
├── chunkText: TEXT
├── embedding: vector(512)   [Voyage AI voyage-3-lite]
└── createdAt: LocalDateTime

AgentChatRequest
├── message: String
└── conversationHistory: List<Map<String, Object>>

AgentChatResponse
├── message: String
├── actionTaken: boolean
├── actionType: String       [NONE, BOOKED, PAYMENT_REQUIRED]
├── razorpayOrderId, amount, doctorId, slotId (when payment required)

Slot Session (Redis)
├── key: "agent:slots:{userId}"
├── value: { doctorId, slots: [...] }
└── TTL: 10 minutes
```

### 8.7 E-Store Models

```
Product (id, name, price, stock, imageUrl, categoryId)
Category (id, name, description)
Order (id, userId, items, totalAmount, paymentId, status)
OrderItem (orderId, productId, quantity, priceAtPurchase)
```

---

## 9. Caching Strategy

### Where Caching Is Used

| Service | Use Case | Tech | TTL |
|---------|----------|------|-----|
| doctorservice | Doctor listings | Redis @Cacheable | 10 min |
| userservice | OTP storage | Redis | 10 min |
| aiservice | MCP agent slot sessions | Redis | 10 min |

### doctorservice — Listing Cache

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

### MCP Agent Slot Sessions — The Critical Use Case

Unlike the doctor cache (read optimization), the MCP agent uses Redis for **state bridging across LLM stateless requests**. Without this, the LLM hallucinates IDs because conversation history sent from frontend only contains text — no tool call results.

```java
// Key pattern
agent:slots:{userId} → { doctorId: 3, slots: [...] }
TTL: 10 minutes (long enough for user to complete booking flow)
```

This is the architectural pattern that makes the MCP agent reliable.

---

## 10. Authentication & Security

### Account Verification (Email OTP)

Every new user must verify their email before logging in.

**Implementation:**
- 6-digit OTP generated using `SecureRandom`
- Stored in Redis with 10-minute TTL — server-side state
- Delivered via Gmail SMTP (App Password authentication)
- `User.isEnabled()` returns `is_verified` — Spring Security blocks unverified logins
- One-time use — OTP deleted from Redis on successful verification

### JWT Token Architecture

**Access Token (15 minutes)** — `Authorization: Bearer <token>`, validated by gateway
**Refresh Token (7 days)** — HttpOnly Secure cookie, DB-tracked for revocation

### Refresh Token Rotation

1. Frontend access token expires
2. Frontend calls `POST /auth/refresh`
3. Gateway validates refresh token in cookie
4. userservice generates NEW refresh token (rotation), invalidates old one

Stolen refresh tokens become useless after first use.

### Gateway JWT Filter

```
Request → Gateway
   │
   ▼
Is path in whitelist?
(/auth/login, /auth/signup, /auth/refresh, /auth/verify-otp, /api/v1/ai/symptom-check)
   │
   ├── YES → Forward to service
   │
   └── NO  → Validate JWT → Extract userId, roles
            → Add X-User-Id, X-Roles headers → Forward to service
```

### Video Call Security (ZEGOCLOUD)

Custom AES-256 encryption engine — session-specific key, distributed via JWT-validated channel, destroyed after session ends.

### Payment Security (Razorpay)

- Order created server-side (frontend never sees key secret)
- Payment signature verified on backend with HMAC-SHA256
- Failed signature verification rejects payment entirely

### AI Service Security

- **Symptom checker** — public (non-sensitive)
- **Document summarizer** — requires JWT, doctor must have active appointment with patient (enforced by documentservice access control)
- **MCP agent** — requires JWT, userId injected from token, all tool calls authenticated downstream

---

## 11. Engineering Challenges & Solutions

This section documents real engineering problems encountered while building Delma and how they were solved. These are the kinds of problems that come up in system design interviews.

### 11.1 Race Condition: Double Booking Prevention

**Problem:** Two patients clicking "Book" on the same slot at the same moment created two appointments — a classic Time-of-Check to Time-of-Use (TOCTOU) race condition.

**Solution:** Optimistic locking with `@Version`. Hibernate auto-adds version check to UPDATE statements. When two threads attempt to update with the same version, only one succeeds. The other gets `ObjectOptimisticLockingFailureException`, caught and returned as HTTP 409.

```java
@Entity
public class DoctorSlot {
    @Id private Long id;
    @Version private Long version;  // Hibernate handles the rest
}
```

### 11.2 Cache Invalidation Strategy

**Problem:** Cached doctor listings became stale after admin approvals.

**Solution:** Combine `@CacheEvict(allEntries=true)` on state changes with 10-minute TTL as a safety net — defense in depth.

### 11.3 Service Discovery vs Hardcoded URLs

**Problem:** Hardcoded `url = "http://localhost:8010"` in Feign clients broke horizontal scaling.

**Solution:** Removed `url` parameter, use `@FeignClient(name = "doctorservice")` — Eureka resolves at runtime, supports load balancing.

### 11.4 Spring Cloud Gateway 5.0 Configuration Migration

**Problem:** After upgrading to Spring Cloud 2025.x, routes silently stopped working. No errors logged.

**Root cause:** Configuration namespace moved from `spring.cloud.gateway.routes` to `spring.cloud.gateway.server.webflux.routes`. Old keys silently ignored.

**Lesson:** Always read migration guides for major framework upgrades.

### 11.5 Inter-Service Contract Evolution

**Problem:** Changing doctorservice return type from `List<DoctorResponse>` to `ApiResponse<List<DoctorResponse>>` broke all Feign clients.

**Solution:** Update client signatures + add `@JsonCreator` to `ApiResponse` constructor for Jackson deserialization.

### 11.6 OTP Implementation: Why Redis Over Postgres

For ephemeral data with strict TTL, Redis wins on all axes — native EXPIRE, sub-millisecond reads, no migrations, automatic cleanup. Postgres would require a cron job and a TTL column.

### 11.7 Email Password Truncation Bug

Gmail App Password in `.env` with spaces caused shell to truncate at first space. Fix: remove all spaces from password.

### 11.8 Frontend Race Condition: Filter Stacking

**Problem:** Sequential filter applications worked on already-filtered data, producing wrong results.

**Solution:** Separate `originalDocs` state (source of truth) from `docs` (displayed). Filters always operate on `originalDocs`.

### 11.9 Razorpay Frontend Integration: Token Mutation Bug

Axios interceptor was injecting JWT into all request bodies, mutating the Razorpay response object. Fix: keep JWT in headers only, never inject into body.

### 11.10 RAG: pgvector Type Mapping Issue

**Problem:** Hibernate kept trying to save `float[]` as `bytea` instead of `vector`. Hibernate's `@JdbcTypeCode(SqlTypes.VECTOR)` wasn't reliably triggering pgvector serialization.

**Solution:** Bypass Hibernate for vector inserts — use raw JdbcTemplate with explicit `?::vector` cast:

```java
jdbcTemplate.update(
    "INSERT INTO document_embeddings (..., embedding, ...) " +
    "VALUES (..., ?::vector, ...)",
    ..., toVectorString(embedding), ...
);

private String toVectorString(float[] emb) {
    return "[" + Arrays.stream(emb)
        .mapToObj(String::valueOf)
        .collect(joining(",")) + "]";
}
```

**Lesson:** When ORM abstractions fail with niche types, drop to JDBC. Don't fight the framework — go around it.

### 11.11 RAG: Embedding API Rate Limits

**Problem:** Voyage AI free tier has 3 RPM rate limit. Indexing an 11-chunk PDF tried 11 calls in 1 second and immediately hit 429.

**Solution:** Sleep 21 seconds between embedding calls within the same indexing job. Indexing takes ~4 minutes per document, but it's async via Kafka — user upload completes instantly.

**Why not buy a paid plan?** Voyage gives 200M free tokens which is unlimited for a portfolio project. The rate limit only blocks bursts, not total volume.

### 11.12 MCP Agent: LLM Hallucinates IDs Between Turns (the BIG one)

**Problem:** This was the hardest bug in the entire project. When the user picked a time slot in turn 2 of a conversation, the LLM would call `book_appointment(doctorId: 123, slotId: 456)` — completely made-up values. Groq returned 400 errors because the schema required integers and got strings, or the IDs didn't match any real doctor.

**Root cause:** LLMs have no memory between API calls. Each request is fresh. The conversation history sent from frontend only contains text messages — `tool_call` results with real IDs are lost between turns.

```
Turn 1:
  history: [
    user: "Book orthopedics tomorrow",
    assistant tool_call: search_doctors  ← these tool calls
    tool result: [{id:3, name:"Dr. Manju"}]  ← and results
    assistant tool_call: get_slots          ← are NOT preserved
    tool result: [{id:9, time:"11:00"}]    ← in frontend state
    assistant text: "Available slots: 10:30, 11:00..."
  ]

Turn 2 (new request from frontend):
  history: [
    user: "Book orthopedics tomorrow",
    assistant: "Available slots: 10:30, 11:00...",  ← only text
    user: "11:00 AM"
  ]
  
  LLM has NO real IDs in context → hallucinates 123, 456 → 400 error
```

**Solution:** Redis-based slot session storage with system prompt injection.

```java
// After get_available_slots executes — store in Redis
slotSessionStore.storeSlots(userId, doctorId, slots);
// Key: "agent:slots:{userId}", TTL: 10 min

// At start of every new request — inject as system message
private void injectSlotContext(messages, userId) {
    Map session = slotSessionStore.getSession(userId);
    if (session == null) return;
    
    String ctx = String.format(
        "INJECTED CONTEXT: doctorId=%s, slots=%s. " +
        "Use these exact integer values.",
        session.get("doctorId"), session.get("slots")
    );
    
    messages.add(1, Map.of("role", "system", "content", ctx));
}
```

Now real integer IDs are always in the LLM's context, regardless of conversation turn. No more hallucination.

**Lesson:** When integrating stateless LLMs into multi-turn workflows, don't trust them to remember structured data. Store it server-side and inject on every turn.

### 11.13 MCP Agent: Tool Validation 400 Errors

**Problem:** Even with Redis injection, occasionally the LLM would still pass strings instead of integers, triggering Groq's strict 400 validation.

**Solution:** Build a correction loop in `callGroq()` — on 400, inject a correction message and return `null` to signal retry:

```java
if (status == 400) {
    Map correction = Map.of(
        "role", "user",
        "content", "You must call search_doctors first to get real integer IDs."
    );
    messages.add(correction);
    return null;  // caller will loop again with updated messages
}
```

The agent self-corrects within the same request, transparent to the user.

### 11.14 MCP Agent: Field Name Confusion (id vs userId)

**Problem:** Doctor search returned both `id` (doctor profile PK) and `userId` (user account ID). The slots endpoint expected `userId` but LLM used `id`. Result: `get_available_slots(doctorId=1)` returned empty because real `doctorId` was 3.

**Solution:** Transform the tool response in `ToolExecutor` — drop the confusing `id` field and rename `userId` to `doctorId`:

```java
private String searchDoctors(Map<String, Object> args) throws Exception {
    var result = doctorClient.searchDoctors(keyword);
    List<Map<String, Object>> doctors = result.getData();
    
    doctors.forEach(doctor -> {
        Object userId = doctor.get("userId");
        if (userId != null) {
            Integer doctorIdInt = Integer.parseInt(userId.toString());
            doctor.put("doctorId", doctorIdInt);  // add as integer
            doctor.remove("userId");              // remove string version
            doctor.remove("id");                  // remove confusing field
        }
    });
    return objectMapper.writeValueAsString(doctors);
}
```

Now the LLM sees only one ID field (`doctorId`), already an integer, with a clear name. Zero ambiguity.

**Lesson:** When wrapping APIs for LLM consumption, design tool responses for the LLM, not for the underlying service. Remove ambiguity at the boundary.

### 11.15 MCP Agent: Model Selection

**Problem:** `llama-3.1-8b-instant` hallucinated frequently, suggested non-existent doctors. `llama-3.3-70b-versatile` followed instructions better but hit 12K TPM rate limit constantly.

**Solution:** `qwen/qwen3-32b` — sweet spot of instruction following + token throughput. Much better at sticking to tool results without inventing data.

**Lesson:** Model selection matters more than prompt engineering for agent workflows. A well-described tool with a 32B model beats elaborate prompts with an 8B model.

---

## 12. Deployment (Docker + CI/CD)

### Docker Containerization

Every service has its own `Dockerfile` and is published to Docker Hub.

**docker-compose.yml** orchestrates:
- 13 microservices (Spring Boot)
- PostgreSQL 16 with pgvector extension
- Redis 7
- Apache Kafka 3.x + Zookeeper

All services communicate over a shared Docker network (`delma-network`) using service names.

### CI/CD Pipeline (GitHub Actions)

`.github/workflows/ci.yml` triggers on push to `main`:

```
┌────────────────────────────────────────────────────────┐
│  GitHub Actions Workflow                                │
├────────────────────────────────────────────────────────┤
│  1. Checkout code                                       │
│  2. Set up JDK 21                                       │
│  3. Cache Maven dependencies                            │
│  4. Run mvn clean package -DskipTests                   │
│  5. Set up Docker Buildx (multi-platform)               │
│  6. Login to Docker Hub                                 │
│  7. For each service: build + tag + push                │
│     Multi-arch: linux/amd64 + linux/arm64               │
└────────────────────────────────────────────────────────┘
```

**Why multi-arch builds:** Developer machines (Apple Silicon `arm64`) and production servers (`amd64`) use the same images.

### Environment Variables (Production)

```yaml
environment:
  - JWT_SECRET=${JWT_SECRET}
  - AWS_ACCESS_KEY=${AWS_ACCESS_KEY}
  - AWS_SECRET_KEY=${AWS_SECRET_KEY}
  - AWS_S3_BUCKET_DOCUMENTS=${AWS_S3_BUCKET_DOCUMENTS}
  - RAZORPAY_KEY_ID=${RAZORPAY_KEY_ID}
  - RAZORPAY_KEY_SECRET=${RAZORPAY_KEY_SECRET}
  - GROQ_API_KEY=${GROQ_API_KEY}
  - GROQ_API_MODEL=qwen/qwen3-32b
  - VOYAGE_API_KEY=${VOYAGE_API_KEY}
  - MAIL_USERNAME=${MAIL_USERNAME}
  - MAIL_PASSWORD=${MAIL_PASSWORD}
  - REDIS_HOST=redis
```

`.env` is gitignored.

---

## 13. Local Setup

### Option A: Docker Compose (Recommended)

```bash
git clone https://github.com/AakashTyagi354/delma2.0_spring_microservice.git
cd delma2.0_spring_microservice

cp .env.example .env
# Edit .env and fill in your secrets

docker-compose up -d
docker-compose ps
```

Eureka dashboard at http://localhost:8761.

### Option B: Manual

**Prerequisites:**
- Java 21 (LTS)
- Maven 3.9+
- PostgreSQL 16 with pgvector extension
- Redis 7
- Apache Kafka 3.x

**Database Setup:**
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
CREATE DATABASE delma_ai_db;

-- In delma_ai_db:
CREATE EXTENSION IF NOT EXISTS vector;
```

**Startup order:**
1. Eureka (`discovery-server`)
2. All microservices
3. Gateway last

### Verify Setup
- Eureka Dashboard: http://localhost:8761
- Gateway: http://localhost:8089/auth/login
- AI Symptom Check: `POST /api/v1/ai/symptom-check`
- AI Summary: `GET /api/v1/ai/summarize/{userId}`
- AI Agent: `POST /api/v1/ai/agent/chat`

### Required Environment Variables

```bash
export JWT_SECRET=<your-secret>
export AWS_ACCESS_KEY=<your-key>
export AWS_SECRET_KEY=<your-secret>
export AWS_S3_BUCKET_DOCUMENTS=delma-patient-documents
export RAZORPAY_KEY_ID=<your-key>
export RAZORPAY_KEY_SECRET=<your-secret>
export GROQ_API_KEY=<your-groq-key>
export GROQ_API_MODEL=qwen/qwen3-32b
export VOYAGE_API_KEY=<your-voyage-key>
export MAIL_USERNAME=<your-gmail>
export MAIL_PASSWORD=<gmail-app-password-no-spaces>
```

---

## 14. Tech Stack

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
| Database | PostgreSQL + pgvector | 16 + 0.8 |
| Caching | Redis | 7.x |
| Messaging | Apache Kafka | 3.x |
| Auth | JJWT | 0.12.6 |

### AI Stack
| Layer | Technology | Purpose |
|-------|-----------|---------|
| LLM (Symptom + Summary) | Groq `llama-3.1-8b-instant` | Fast, cheap inference |
| LLM (Agent) | Groq `qwen/qwen3-32b` | Better tool-calling accuracy |
| Embeddings | Voyage AI `voyage-3-lite` (512-dim) | RAG indexing |
| Vector DB | PostgreSQL + pgvector | Cosine similarity search |
| PDF Extraction | Apache PDFBox | 3.x |
| Agent Pattern | ReAct (custom Java impl) | Tool-calling loop |
| Session Store | Redis | LLM state bridging |

### Frontend
| Layer | Technology |
|-------|-----------|
| Framework | Next.js 14 (App Router) |
| Language | TypeScript |
| State | Redux Toolkit |
| Styling | Tailwind CSS + shadcn/ui |
| HTTP | Axios with interceptors |
| AI Chat | Custom `AIBookingChat` component |

### DevOps
| Tool | Purpose |
|------|---------|
| Docker / Docker Compose | Containerization |
| Docker Hub | Image registry |
| GitHub Actions | CI/CD pipeline |
| Docker Buildx | Multi-arch builds |

### External Services
| Service | Purpose |
|---------|---------|
| AWS S3 | Document storage |
| ZEGOCLOUD | Video calls (AES-256) |
| Razorpay | Payments |
| Groq AI | LLM inference |
| Voyage AI | Vector embeddings |
| Gmail SMTP | OTP email delivery |

---

## 15. Future Roadmap

### AI Features Roadmap
- [x] ~~**Symptom Checker** — Groq AI suggests doctor specialization~~ ✅ Done
- [x] ~~**RAG Document Summarizer** — pgvector + Voyage AI~~ ✅ Done
- [x] ~~**MCP Booking Agent** — ReAct pattern with tool calling~~ ✅ Done
- [ ] **Razorpay integration in agent** — agent triggers payment popup
- [ ] **Post-Consultation Notes** — AI-generated structured summary after video call
- [ ] **Smart Doctor Recommendations** — Rank doctors based on user history + ratings
- [ ] **Multi-language support** — Hindi, Tamil, regional languages

### Production Readiness
- [x] ~~Dockerize all services~~ ✅ Done
- [x] ~~CI/CD pipeline (GitHub Actions)~~ ✅ Done
- [x] ~~Email OTP verification~~ ✅ Done
- [ ] AWS EC2 deployment with public URL
- [ ] Distributed tracing (Zipkin / OpenTelemetry)
- [ ] Centralized logging (ELK stack)
- [ ] Spring Cloud Config Server
- [ ] Kubernetes deployment manifests
- [ ] Rate limiting on `/auth/login`
- [ ] Load testing with k6 / JMeter

### Resume-Worthy Metrics (when production data exists)
- Concurrent users supported
- p99 latency under load
- Cache hit rate
- AI inference latency (Groq: sub-second)
- RAG retrieval accuracy
- MCP agent task completion rate

---

## License

MIT License — see LICENSE file for details.

## Author

**Aakash Tyagi**
Full Stack Engineer · Specialist Programmer at Infosys

[GitHub](https://github.com/AakashTyagi354) · [LinkedIn](https://linkedin.com/in/aakashtyagi354)

Building production-grade microservices and AI features for healthcare. Open to discussing architecture, agentic AI patterns, and backend systems.
