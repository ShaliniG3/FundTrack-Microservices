# FundTrack: Grant Application & Disbursement Management System

[![Architecture: Microservices](https://img.shields.io/badge/Architecture-Microservices-blue)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-green)](#)
[![Uptime: 99.9%](https://img.shields.io/badge/Availability-99.9%25-brightgreen)](#)

**FundTrack** is a microservices-driven solution for government agencies, NGOs, and foundations to streamline the end-to-end management of grant applications, approvals, disbursements, and compliance reporting. The system ensures high levels of transparency and accountability by maintaining immutable audit trails and performance metrics.

---

## ⚡ Core Functionalities

### 📌 Comprehensive Grant Lifecycle
- Supports the full journey from defining grant programs and eligibility rules to final payment reconciliation and compliance checks.

### 🔐 Role-Based Access Control (RBAC)
- Provides dedicated interfaces for diverse actors, including:
  - Administrators  
  - Applicants  
  - Reviewers  
  - Approvers  
  - Finance Officers  
  - Compliance Officers  

### 🤖 Automated Validation & Scoring
- Utilizes an eligibility engine and scoring system to assist reviewers in making objective, data-backed recommendations.

### 💰 Financial Integrity
- Manages complex disbursement schedules and payment tracking to ensure funds reach the correct recipients on time.

### 🚀 High-Performance Architecture
- Built on a microservices-based backend to support up to **20,000 concurrent users** with **99.9% availability**.

---

## 📂 Backend Microservices

The FundTrack backend is organized into modular microservices, each responsible for a specific part of the grant lifecycle.

### 1. Identity Service
- Handles authentication and role-based access control (RBAC).
- Uses JSON Web Tokens (JWT) to securely manage stateless user sessions and identity.

### 2. Program Service
- Manages grant program definitions, budgets, and timelines.
- Configures eligibility rules and required documents.

### 3. Grant Lifecycle Management Service
- Application & Intake: Managing applicant submissions, including multipart document uploads and automated eligibility validation.
- Evaluation & Peer Review: Orchestrating the assignment of applications to reviewers and capturing qualitative feedback, scoring, and internal recommendations.
- Processing final approval or rejection statuses based on review outcomes to finalize the decision-making phase.
- Aggregating data across the entire lifecycle to generate real-time analytics, compliance dashboards, and regulatory reports for stakeholders.

### 4. Grant Disbursement & Compliance Service
- Financial Operations: Managing complex disbursement schedules, tracking fund allocations, and performing automated payment reconciliations.
- Regulatory Oversight: Executing both financial and operational compliance checks to mitigate risk and ensure adherence to grant agreements.
- Providing a dedicated portal for grantees to submit structured periodic reports, financial statements, and operational evidence.

### 5. Notification Service
- Sends in-app and email alerts for application status, disbursements, and compliance reminders.

### 6. AuditLog Service
- Records a permanent, immutable trail of actions, resources, and timestamps.
- Provides the data source for investigating system changes and user activity..

---

## 🛠 Tech Stack

- **Frontend**: React for responsive dashboards  
- **Backend**: REST API-based microservices using Java Spring Boot 
- **Database**: Relational DB (MySQL Server)  
- **Infrastructure**: API Gateway, WAF, Centralized Logging, and Monitoring  

---



## 👥 User Roles & Portals

The system provides tailored interfaces and specialized workflows for the following actors:

* **Applicant**  
  - Submits grant applications and uploads all required supporting documentation.  
  - Tracks the real-time status of submitted applications through a dedicated portal.  

* **Reviewer**  
  - Evaluates assigned applications based on predefined eligibility criteria.  
  - Assigns numerical scores and provides formal recommendations for further action.  

* **Approver**  
  - Reviews the recommendations and scores provided by the Reviewers.  
  - Issues the final decision to either approve or reject an application.  

* **Finance Officer**  
  - Manages complex disbursement schedules and monitors fund utilization.  
  - Reconciles payments and tracks financial references for transparency.  

* **Compliance Officer**  
  - Ensures all applicants and projects strictly adhere to grant conditions.  
  - Monitors post-grant operational reporting and financial compliance.  

* **Administrator**  
  - Configures the core grant programs, eligibility rules, and internal workflows.  
  - Generates comprehensive performance and regulatory reports for stakeholders.  

---

## 🚀 Getting Started

### 1. Clone the Repository
git clone https://github.com/your-org/fundtrack.git

## 🏗 System Architecture

The system utilizes a **stateless microservices architecture** to handle high concurrency (up to 20,000 users) and ensure modular maintainability.  
Each service is modular, independently deployable, and connected through an API Gateway for secure communication.
```mermaid
graph TD
    User((Users)) -->|HTTPS/REST| AGW[API Gateway: 8081]
    
    subgraph "Core Microservices"
    AGW --> Auth[1. Identity Service: 9090]
    AGW --> Prog[2. Program Service: 9091]
    AGW --> GLMS[3. GLMS: 9092]
    AGW --> GDCS[4. GDCS: 9093]
    AGW --> Notify[5. Notification Service: 9094]
    AGW --> Audit[6. AuditLog Service: 9095]
    end

    subgraph "Data & Storage Layer"
    GLMS & GDCS --- S3[(Object Storage: PDFs/Proofs)]
    Auth & Prog & GLMS & GDCS & Notify & Audit --- DB[(PostgreSQL Cluster)]
    end

    subgraph "Messaging Layer"
    GLMS -.->|Events| Notify
    GDCS -.->|Events| Notify
    GLMS & GDCS -.->|Audit Trail| Audit
    end

    subgraph "Data Layer"
    DB[(Relational DB)]
    Auth & Prog & App & Review & Decision & Finance & Comp & Report & Notify & Analytics & Audit --- DB
    end
