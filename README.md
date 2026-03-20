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
- Maintains audit logs for all user actions.

### 2. Program Service
- Manages grant program definitions, budgets, and timelines.
- Configures eligibility rules and required documents.

### 3. Application Service
- Supports application submission and validation.
- Handles document uploads and eligibility checks.

### 4. Review Service
- Assigns reviewers to applications.
- Captures scores, comments, and recommendations.

### 5. Decision Service
- Processes reviewer recommendations.
- Records final approval or rejection decisions.

### 6. Finance Service
- Manages disbursement schedules and fund allocations.
- Tracks payments and reconciliations.

### 7. Compliance Service
- Performs compliance checks (financial/operational).
- Collects post-grant reports and monitors adherence.

### 8. Reporting Service
- Enables applicants, grantees, and project teams to submit periodic and final reports.  
- Supports uploading structured documents, financial statements, and operational updates.

### 9. Notification Service
- Sends in-app and email alerts for application status, disbursements, and compliance reminders.

### 10. Analytics Service
- Generates analytics dashboards and regulatory reports.
- Enables administrators and stakeholders to make data-driven decisions through real-time insights.

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
    User((Users)) -->|HTTPS| AGW[API Gateway: 8081]
    
    subgraph "Core Microservices"
    AGW --> Auth[Identity & Access: 9090]
    AGW --> Prog[Grant Programs: 9091]
    AGW --> App[Application Mgmt: 9092]
    AGW --> Review[Review & Scoring: 9093]
    AGW --> Decision[Decision Engine: 9094]
    AGW --> Finance[Finance/Payments: 9095]
    AGW --> Comp[Compliance/Reporting: 9096]
    AGW --> Report[Compliance/Reporting: 9097]
    AGW --> Notify[Notifications: 9098]
    AGW --> Analytics[Analytics: 9099]
    end

    subgraph "Data Layer"
    DB[(Relational DB)]
    Auth & Prog & App & Review & Decision & Finance & Comp & Report & Notify & Analytics --- DB
    end
