# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

This is a full-stack banking platform with Spring Boot backend and Next.js frontend:

- **Backend**: Spring Boot 3.5.5 application in `/src/main/java/com/example/bankingplatform/`
- **Frontend**: Next.js 15.1.0 application in `/banking-frontend/`
- **Database**: PostgreSQL with Redis for caching/sessions
- **Infrastructure**: Docker Compose setup for local development

## Architecture Overview

### Backend Architecture
- **Security**: JWT-based authentication with Spring Security
- **Data Layer**: JPA/Hibernate with PostgreSQL, Redis for session storage
- **Core Modules**:
  - `user/` - User management and authentication
  - `account/` - Bank account operations
  - `transaction/` - Financial transactions
  - `billing/` - Billing and recurring payments
  - `loan/` - Loan management
  - `report/` - Report generation (PDF via PDFBox)
  - `audit/` - Audit logging and security tracking
- **Security Features**: Rate limiting, CORS configuration, custom exception handling
- **Scheduled Tasks**: Quartz for recurring operations
- **API Documentation**: OpenAPI/Swagger available

### Frontend Architecture
- **Framework**: Next.js with TypeScript
- **Styling**: Tailwind CSS
- **State Management**: React Query (@tanstack/react-query) for server state
- **UI Components**: Custom components in `/components/ui/`
- **Pages**: File-based routing with pages for dashboard, accounts, transactions, billing, loans, reports, admin
- **Auth**: Context-based authentication management

## Development Commands

### Backend (Spring Boot)
```bash
# Build and run tests
./mvnw clean test

# Build application
./mvnw clean package

# Run application (requires database)
./mvnw spring-boot:run

# Generate JAR file
./mvnw clean package -DskipTests
```

### Frontend (Next.js)
```bash
cd banking-frontend

# Development server
npm run dev

# Build for production
npm run build

# Start production server
npm start

# Linting
npm run lint
```

### Infrastructure
```bash
# Start PostgreSQL and Redis containers
docker-compose up -d

# Stop containers
docker-compose down
```

## Configuration

### Environment Variables
- Copy `.env.example` to `.env` and configure:
  - Database connection (PostgreSQL)
  - Redis configuration
  - JWT secret and expiration
  - Log levels

### Database Setup
- PostgreSQL database with configurable connection via environment variables
- Hibernate DDL auto-configuration (default: update)
- Connection pooling with HikariCP

### Key Configuration Files
- `application.yml` - Spring Boot configuration with environment variable support
- `docker-compose.yml` - Local PostgreSQL and Redis setup
- `banking-frontend/package.json` - Frontend dependencies and scripts

## Development Workflow

1. **Database**: Start with `docker-compose up -d` for PostgreSQL and Redis
2. **Backend**: Run Spring Boot application with `./mvnw spring-boot:run`
3. **Frontend**: Start Next.js dev server with `npm run dev` in `/banking-frontend/`
4. **API Access**: Swagger UI available at `/swagger-ui.html` when running

## Key Technologies

- **Backend**: Spring Boot, Spring Security, JPA/Hibernate, JWT, PDFBox, Quartz
- **Frontend**: Next.js, TypeScript, Tailwind CSS, React Query
- **Database**: PostgreSQL, Redis
- **Build Tools**: Maven (backend), npm (frontend)
- **Testing**: Spring Boot Test, TestContainers for integration tests