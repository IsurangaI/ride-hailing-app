# Ride-Hailing Backend: Complete Step-by-Step Guide
### React + TypeScript Frontend · Java/Spring Boot Microservices · AWS Free Tier

---

## Table of Contents
1. [Windows Machine Setup](#1-windows-machine-setup)
2. [Project Structure Overview](#2-project-structure-overview)
3. [Auth Service — Login & JWT](#3-auth-service--login--jwt)
4. [React + TypeScript Frontend — Login UI](#4-react--typescript-frontend--login-ui)
5. [Booking Service](#5-booking-service)
6. [Driver Matching Service](#6-driver-matching-service)
7. [Fare Analytics Service](#7-fare-analytics-service)
8. [Kafka Setup (Local + AWS MSK)](#8-kafka-setup-local--aws-msk)
9. [Dockerizing Every Service](#9-dockerizing-every-service)
10. [AWS Free Tier Deployment](#10-aws-free-tier-deployment)
11. [docker-compose for Local Dev](#11-docker-compose-for-local-dev)
12. [README Template for GitHub](#12-readme-template-for-github)

---

## 1. Windows Machine Setup

Install everything in this order. Each tool is needed before the next step.

### 1.1 Install WSL2 (Windows Subsystem for Linux)
Docker Desktop on Windows runs best with WSL2. Open PowerShell as Administrator:

```powershell
wsl --install
# Reboot when prompted
# After reboot, set WSL2 as default
wsl --set-default-version 2
```

### 1.2 Install Docker Desktop
1. Download from https://www.docker.com/products/docker-desktop/
2. During install, check "Use WSL 2 instead of Hyper-V"
3. After install, open Docker Desktop → Settings → Resources → WSL Integration → enable your distro
4. Verify in a terminal:
```bash
docker --version      # Docker Desktop 4.x
docker-compose --version
```

### 1.3 Install Java 21 (Amazon Corretto — same JVM used on AWS)
1. Download Amazon Corretto 21 from https://aws.amazon.com/corretto/
2. Install the `.msi` (Windows installer)
3. Verify:
```bash
java -version
# openjdk 21... Amazon Corretto
```

### 1.4 Install IntelliJ IDEA Community Edition
Download from https://www.jetbrains.com/idea/download/ — the free Community edition is enough for this project.

### 1.5 Install Node.js (for the React frontend)
Download Node.js 20 LTS from https://nodejs.org/
```bash
node --version   # v20.x
npm --version    # 10.x
```

### 1.6 Install Git
Download from https://git-scm.com/download/win
```bash
git --version
```

### 1.7 Install AWS CLI
1. Download from https://aws.amazon.com/cli/
2. Install the `.msi`
3. Configure with your AWS account:
```bash
aws configure
# AWS Access Key ID: (from IAM → your user → Security credentials)
# AWS Secret Access Key: (same place)
# Default region: eu-west-1   (Ireland — closest to Sri Lanka with good free tier)
# Default output format: json
```

### 1.8 Install Maven
Download from https://maven.apache.org/download.cgi — extract to `C:\maven`
Add `C:\maven\bin` to your system PATH (System Properties → Environment Variables → Path → Edit → New).
```bash
mvn --version
```

### 1.9 Install Postman (for testing APIs)
Download from https://www.postman.com/downloads/

---

## 2. Project Structure Overview

Create this folder structure on your machine. All services are siblings inside one Git repository (a monorepo).

```
ride-hailing/
├── docker-compose.yml          ← Spins up everything locally
├── README.md
│
├── frontend/                   ← React + TypeScript
│   ├── src/
│   │   ├── pages/
│   │   │   └── LoginPage.tsx
│   │   ├── services/
│   │   │   └── authService.ts
│   │   └── App.tsx
│   ├── package.json
│   └── Dockerfile
│
├── auth-service/               ← Spring Boot — JWT login
│   ├── src/main/java/
│   ├── Dockerfile
│   └── pom.xml
│
├── booking-service/            ← Spring Boot — ride orchestration
│   ├── src/main/java/
│   ├── Dockerfile
│   └── pom.xml
│
├── matching-service/           ← Spring Boot — Redis geo + driver matching
│   ├── src/main/java/
│   ├── Dockerfile
│   └── pom.xml
│
└── fare-service/               ← Spring Boot — event-driven fare calculation
    ├── src/main/java/
    ├── Dockerfile
    └── pom.xml
```

Initialize the repo:
```bash
mkdir ride-hailing && cd ride-hailing
git init
```

---

## 3. Auth Service — Login & JWT

This is the most important service to understand first because every other service will call it to validate tokens.

### 3.1 Create the Spring Boot project
Go to https://start.spring.io/ and configure:
- **Project**: Maven
- **Language**: Java
- **Spring Boot**: 3.3.x
- **Group**: `com.ridehailing`
- **Artifact**: `auth-service`
- **Java**: 21
- **Dependencies**: Spring Web, Spring Security, Spring Data JPA, PostgreSQL Driver, Lombok, Validation

Download and extract into `ride-hailing/auth-service/`.

### 3.2 pom.xml — add the JWT library
Inside `<dependencies>` in `pom.xml`, add:

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### 3.3 application.yml

Create `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

server:
  port: 8081

jwt:
  secret: your-256-bit-secret-key-change-this-in-production-minimum-32-chars
  expiration: 86400000   # 24 hours in milliseconds
```

### 3.4 User entity

`src/main/java/com/ridehailing/auth/model/User.java`:

```java
package com.ridehailing.auth.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;   // stored as bcrypt hash

    private String role = "RIDER";  // RIDER or DRIVER
}
```

### 3.5 UserRepository

```java
package com.ridehailing.auth.repository;

import com.ridehailing.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
}
```

### 3.6 JWT Utility

`src/main/java/com/ridehailing/auth/security/JwtUtil.java`:

```java
package com.ridehailing.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

### 3.7 Auth Controller

`src/main/java/com/ridehailing/auth/controller/AuthController.java`:

```java
package com.ridehailing.auth.controller;

import com.ridehailing.auth.dto.LoginRequest;
import com.ridehailing.auth.dto.RegisterRequest;
import com.ridehailing.auth.dto.AuthResponse;
import com.ridehailing.auth.model.User;
import com.ridehailing.auth.repository.UserRepository;
import com.ridehailing.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")   // restrict to your frontend URL in production
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, "Email already in use"));
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : "RIDER");
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
                    return ResponseEntity.ok(new AuthResponse(token, "Login successful"));
                })
                .orElse(ResponseEntity.status(401)
                        .body(new AuthResponse(null, "Invalid credentials")));
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(jwtUtil.validateToken(token));
    }
}
```

### 3.8 DTOs

Create these three simple record classes in `com.ridehailing.auth.dto`:

```java
// LoginRequest.java
public record LoginRequest(String email, String password) {}

// RegisterRequest.java
public record RegisterRequest(String email, String password, String role) {}

// AuthResponse.java
public record AuthResponse(String token, String message) {}
```

### 3.9 Security Config

`src/main/java/com/ridehailing/auth/config/SecurityConfig.java`:

```java
package com.ridehailing.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 3.10 Test the auth service

```bash
cd auth-service
mvn spring-boot:run
```

In Postman, POST to `http://localhost:8081/api/auth/register`:
```json
{
  "email": "test@example.com",
  "password": "password123",
  "role": "RIDER"
}
```
You should get back a JWT token. Copy it and test `POST /api/auth/login` with the same credentials.

---

## 4. React + TypeScript Frontend — Login UI

### 4.1 Create the project
```bash
cd ride-hailing
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install axios react-router-dom @types/react-router-dom
```

### 4.2 Auth service (API calls)

`src/services/authService.ts`:

```typescript
import axios from 'axios';

const API_BASE = import.meta.env.VITE_AUTH_SERVICE_URL || 'http://localhost:8081';

export interface AuthResponse {
  token: string | null;
  message: string;
}

export const login = async (email: string, password: string): Promise<AuthResponse> => {
  const response = await axios.post<AuthResponse>(`${API_BASE}/api/auth/login`, {
    email,
    password,
  });
  return response.data;
};

export const register = async (email: string, password: string, role: string): Promise<AuthResponse> => {
  const response = await axios.post<AuthResponse>(`${API_BASE}/api/auth/register`, {
    email,
    password,
    role,
  });
  return response.data;
};

export const saveToken = (token: string): void => {
  localStorage.setItem('jwt_token', token);
};

export const getToken = (): string | null => {
  return localStorage.getItem('jwt_token');
};

export const logout = (): void => {
  localStorage.removeItem('jwt_token');
};

export const isLoggedIn = (): boolean => {
  return !!getToken();
};
```

### 4.3 Login Page

`src/pages/LoginPage.tsx`:

```typescript
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, register, saveToken } from '../services/authService';

export default function LoginPage() {
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'RIDER' | 'DRIVER'>('RIDER');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const result = isRegister
        ? await register(email, password, role)
        : await login(email, password);

      if (result.token) {
        saveToken(result.token);
        navigate('/dashboard');
      } else {
        setError(result.message);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: '80px auto', padding: '0 16px' }}>
      <h1>{isRegister ? 'Create account' : 'Sign in'}</h1>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 16 }}>
          <label>Email</label>
          <input
            type="email"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
        </div>
        <div style={{ marginBottom: 16 }}>
          <label>Password</label>
          <input
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
        </div>
        {isRegister && (
          <div style={{ marginBottom: 16 }}>
            <label>Role</label>
            <select
              value={role}
              onChange={e => setRole(e.target.value as 'RIDER' | 'DRIVER')}
              style={{ display: 'block', width: '100%', marginTop: 4 }}
            >
              <option value="RIDER">Rider</option>
              <option value="DRIVER">Driver</option>
            </select>
          </div>
        )}
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit" disabled={loading} style={{ width: '100%' }}>
          {loading ? 'Please wait...' : isRegister ? 'Register' : 'Login'}
        </button>
      </form>
      <p style={{ marginTop: 16, textAlign: 'center' }}>
        {isRegister ? 'Already have an account?' : "Don't have an account?"}{' '}
        <button
          onClick={() => setIsRegister(!isRegister)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline' }}
        >
          {isRegister ? 'Sign in' : 'Register'}
        </button>
      </p>
    </div>
  );
}
```

### 4.4 Protected Route wrapper

`src/components/ProtectedRoute.tsx`:

```typescript
import { Navigate } from 'react-router-dom';
import { isLoggedIn } from '../services/authService';

interface Props {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: Props) {
  if (!isLoggedIn()) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}
```

### 4.5 App.tsx with routing

```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import ProtectedRoute from './components/ProtectedRoute';

function Dashboard() {
  return <h1>Dashboard — you are logged in!</h1>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={
          <ProtectedRoute><Dashboard /></ProtectedRoute>
        } />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
```

### 4.6 Frontend .env file
Create `frontend/.env`:
```
VITE_AUTH_SERVICE_URL=http://localhost:8081
```
For production (after deployment), create `frontend/.env.production`:
```
VITE_AUTH_SERVICE_URL=https://your-api-gateway-url.execute-api.eu-west-1.amazonaws.com
```

---

## 5. Booking Service

This service owns the ride lifecycle: REQUESTED → MATCHED → IN_PROGRESS → COMPLETED.

### 5.1 Create at start.spring.io
Same settings as auth-service but:
- **Artifact**: `booking-service`
- **Dependencies**: Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Kafka, Lombok, Validation, Resilience4j (search for "Resilience4J")

### 5.2 Key concepts to implement

**The Ride entity** — store in PostgreSQL (RDS):
```java
@Entity
@Table(name = "rides")
@Data
public class Ride {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String riderId;
    private String driverId;

    @Enumerated(EnumType.STRING)
    private RideStatus status;   // REQUESTED, MATCHED, IN_PROGRESS, COMPLETED, CANCELLED

    private Double pickupLat, pickupLng;
    private Double dropLat, dropLng;
    private LocalDateTime requestedAt;

    @Version   // ← This enables Optimistic Locking — prevents two threads updating simultaneously
    private Long version;
}
```

**Optimistic Locking** — the `@Version` field prevents race conditions. If two requests try to match the same driver simultaneously, one will get an `OptimisticLockException` and can retry cleanly. Add this to your README as a trade-off.

**Publishing to Kafka** — when a ride is created, publish an event:
```java
@Service @RequiredArgsConstructor
public class BookingService {
    private final RideRepository rideRepository;
    private final KafkaTemplate<String, RideRequestedEvent> kafkaTemplate;

    @Transactional
    public Ride requestRide(String riderId, double pickupLat, double pickupLng,
                             double dropLat, double dropLng) {
        Ride ride = new Ride();
        ride.setRiderId(riderId);
        ride.setStatus(RideStatus.REQUESTED);
        ride.setPickupLat(pickupLat);
        ride.setPickupLng(pickupLng);
        ride.setDropLat(dropLat);
        ride.setDropLng(dropLng);
        ride.setRequestedAt(LocalDateTime.now());
        rideRepository.save(ride);

        // Publish event to Kafka — matching-service will pick this up
        kafkaTemplate.send("ride-requests", ride.getId(),
            new RideRequestedEvent(ride.getId(), riderId, pickupLat, pickupLng));

        return ride;
    }
}
```

**Resilience4j Circuit Breaker** — protect calls to an external payment service:
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "queuePayment")
public PaymentResult chargeRider(String riderId, double amount) {
    return paymentClient.charge(riderId, amount);
}

public PaymentResult queuePayment(String riderId, double amount, Throwable t) {
    // Fallback: add to a queue and process later
    pendingPaymentQueue.add(new PendingPayment(riderId, amount));
    return new PaymentResult("QUEUED", "Payment will be processed shortly");
}
```

---

## 6. Driver Matching Service

This is the high-throughput service. Drivers ping their GPS every few seconds; when a ride is requested, find the nearest available driver using Redis geospatial commands.

### 6.1 Dependencies (pom.xml)
Add to the matching-service:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 6.2 Redis Geo commands

```java
@Service @RequiredArgsConstructor
public class DriverLocationService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String GEO_KEY = "driver:locations";

    // Called every few seconds by each driver app
    public void updateDriverLocation(String driverId, double lat, double lng) {
        redisTemplate.opsForGeo()
            .add(GEO_KEY, new Point(lng, lat), driverId);
        // Point(longitude, latitude) — note the order!
    }

    // Called when a ride is requested
    public List<String> findNearbyDrivers(double lat, double lng, double radiusKm) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
            redisTemplate.opsForGeo()
                .radius(GEO_KEY, new Circle(new Point(lng, lat),
                    new Distance(radiusKm, Metrics.KILOMETERS)),
                    RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(5));

        return results.getContent().stream()
            .map(r -> r.getContent().getName())
            .collect(Collectors.toList());
    }
}
```

### 6.3 Kafka Consumer — listen for ride requests

```java
@Component @RequiredArgsConstructor
public class RideRequestConsumer {
    private final DriverLocationService locationService;
    private final KafkaTemplate<String, DriverDispatchedEvent> kafkaTemplate;

    @KafkaListener(topics = "ride-requests", groupId = "matching-service")
    public void onRideRequested(RideRequestedEvent event) {
        List<String> nearbyDrivers = locationService.findNearbyDrivers(
            event.getPickupLat(), event.getPickupLng(), 5.0);

        if (!nearbyDrivers.isEmpty()) {
            String driverId = nearbyDrivers.get(0);  // nearest driver
            kafkaTemplate.send("driver-dispatches",
                new DriverDispatchedEvent(event.getRideId(), driverId));
        }
    }
}
```

### 6.4 JVM tuning note for your README
Add this to your service's Dockerfile for the matching service:
```dockerfile
ENV JAVA_OPTS="-XX:+UseZGC -Xms256m -Xmx512m"
```
ZGC keeps GC pauses under 1ms, important for real-time location updates. Document this in your README under "JVM Tuning" — interviewers love this.

---

## 7. Fare Analytics Service

This service listens for completed trips via Kafka and calculates dynamic fares using Java 21 Virtual Threads.

### 7.1 Enable Virtual Threads
In `application.yml`:
```yaml
spring:
  threads:
    virtual:
      enabled: true   # Spring Boot 3.2+ automatically uses virtual threads for @Async
```

Or configure manually for batch processing:

```java
@Configuration
public class ThreadConfig {
    @Bean("fareExecutor")
    public Executor fareExecutor() {
        // Java 21 Virtual Threads — lightweight, can have millions simultaneously
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

### 7.2 Fare calculation consumer

```java
@Component @RequiredArgsConstructor
public class TripCompletedConsumer {

    @Async("fareExecutor")
    @KafkaListener(topics = "trips-completed", groupId = "fare-service")
    public void onTripCompleted(TripCompletedEvent event) {
        // Runs on a virtual thread — won't block if thousands arrive simultaneously
        double baseFare = 2.50;
        double perKmRate = 1.20;
        double distanceKm = calculateDistance(
            event.getPickupLat(), event.getPickupLng(),
            event.getDropLat(), event.getDropLng());

        double surgeMultiplier = getSurgeMultiplier(event.getTimestamp());
        double totalFare = (baseFare + (distanceKm * perKmRate)) * surgeMultiplier;

        // Save and notify
        fareRepository.save(new Fare(event.getRideId(), totalFare, distanceKm));
    }

    private double getSurgeMultiplier(LocalDateTime time) {
        int hour = time.getHour();
        // Peak hours: 7-9 AM and 5-8 PM
        if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20)) return 1.5;
        return 1.0;
    }
}
```

---

## 8. Kafka Setup (Local + AWS MSK)

### 8.1 Local Kafka via Docker Compose
Add this to your `docker-compose.yml` (full file in section 11):
```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.6.0
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181

kafka:
  image: confluentinc/cp-kafka:7.6.0
  depends_on: [zookeeper]
  ports:
    - "9092:9092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

### 8.2 Spring Boot Kafka config
In each service's `application.yml`:
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092   # use localhost:9092 outside docker
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.ridehailing.*"
```

### 8.3 Topics to create
The services need these Kafka topics:
- `ride-requests` — booking-service → matching-service
- `driver-dispatches` — matching-service → booking-service
- `trips-completed` — booking-service → fare-service

Create them on startup by adding to any service:
```java
@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic rideRequestsTopic() { return TopicBuilder.name("ride-requests").build(); }
    @Bean
    public NewTopic driverDispatchesTopic() { return TopicBuilder.name("driver-dispatches").build(); }
    @Bean
    public NewTopic tripsCompletedTopic() { return TopicBuilder.name("trips-completed").build(); }
}
```

---

## 9. Dockerizing Every Service

### 9.1 Dockerfile for every Spring Boot service
Create this `Dockerfile` in each service folder (auth-service, booking-service, etc.):

```dockerfile
# Stage 1: Build
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B    # cache dependencies layer
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Run (much smaller image)
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

For the matching-service, change `JAVA_OPTS` to include ZGC:
```dockerfile
ENV JAVA_OPTS="-XX:+UseZGC -Xms256m -Xmx512m"
```

### 9.2 Dockerfile for React frontend

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

`frontend/nginx.conf`:
```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;  # needed for React Router
    }
}
```

### 9.3 Build and test locally
```bash
# From the root ride-hailing folder
docker build -t auth-service ./auth-service
docker build -t booking-service ./booking-service
docker build -t matching-service ./matching-service
docker build -t fare-service ./fare-service
docker build -t ridehailing-frontend ./frontend

# Or just use docker-compose:
docker-compose up --build
```

---

## 10. AWS Free Tier Deployment

### 10.1 What you'll use (all free tier eligible)

| Service | What For | Free Tier Limit |
|---|---|---|
| **ECR** | Store Docker images | 500 MB/month |
| **ECS Fargate** | Run containers (serverless) | 750 hours/month |
| **RDS PostgreSQL** | auth-service + booking-service DB | 750 hours, 20 GB |
| **ElastiCache Redis** | Driver location cache | 750 hours |
| **API Gateway** | Route HTTP to services | 1M calls/month |
| **S3 + CloudFront** | Host React frontend | 5 GB + 15 GB transfer |
| **MSK (Kafka)** | Event bus | Not fully free — use t3.small in VPC |
| **CloudWatch** | Logs + metrics | 5 GB logs/month |

> **MSK note**: Amazon MSK is not in the free tier. For your portfolio project, keep Kafka running in Docker on ECS (via docker-compose) using the Confluent community image, or use a single `t3.small` MSK broker which costs ~$0.03/hour. Many interviewers will accept "would use MSK in production" in your README.

### 10.2 Step-by-step AWS deployment

**Step 1: Create an ECR registry for each service**
```bash
aws ecr create-repository --repository-name ride-hailing/auth-service --region eu-west-1
aws ecr create-repository --repository-name ride-hailing/booking-service --region eu-west-1
aws ecr create-repository --repository-name ride-hailing/matching-service --region eu-west-1
aws ecr create-repository --repository-name ride-hailing/fare-service --region eu-west-1
```

**Step 2: Log in to ECR and push images**
```bash
# Get login token
aws ecr get-login-password --region eu-west-1 | \
  docker login --username AWS --password-stdin \
  YOUR_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com

# Tag and push (repeat for each service)
docker tag auth-service:latest \
  YOUR_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/ride-hailing/auth-service:latest

docker push \
  YOUR_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/ride-hailing/auth-service:latest
```

**Step 3: Create RDS PostgreSQL (Free Tier)**
```bash
aws rds create-db-instance \
  --db-instance-identifier ridehailing-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16 \
  --master-username postgres \
  --master-user-password YourStrongPassword123 \
  --allocated-storage 20 \
  --no-multi-az \
  --no-publicly-accessible \
  --region eu-west-1
```

**Step 4: Create ElastiCache Redis (Free Tier)**
```bash
aws elasticache create-cache-cluster \
  --cache-cluster-id ridehailing-redis \
  --cache-node-type cache.t3.micro \
  --engine redis \
  --num-cache-nodes 1 \
  --region eu-west-1
```

**Step 5: Create ECS Cluster**
```bash
aws ecs create-cluster --cluster-name ride-hailing-cluster --region eu-west-1
```

**Step 6: Create ECS Task Definitions**

Create `ecs-task-auth-service.json`:
```json
{
  "family": "auth-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::YOUR_ACCOUNT_ID:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "auth-service",
      "image": "YOUR_ACCOUNT_ID.dkr.ecr.eu-west-1.amazonaws.com/ride-hailing/auth-service:latest",
      "portMappings": [{"containerPort": 8081, "protocol": "tcp"}],
      "environment": [
        {"name": "SPRING_DATASOURCE_URL", "value": "jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/authdb"},
        {"name": "SPRING_DATASOURCE_USERNAME", "value": "postgres"},
        {"name": "SPRING_DATASOURCE_PASSWORD", "value": "YourStrongPassword123"},
        {"name": "JWT_SECRET", "value": "your-production-secret-min-32-chars"}
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/auth-service",
          "awslogs-region": "eu-west-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

Register it:
```bash
aws ecs register-task-definition --cli-input-json file://ecs-task-auth-service.json
```

Repeat the same pattern for each service (booking, matching, fare), changing port numbers (8082, 8083, 8084) and environment variables.

**Step 7: Create ECS Services**
```bash
aws ecs create-service \
  --cluster ride-hailing-cluster \
  --service-name auth-service \
  --task-definition auth-service \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[YOUR_SUBNET_ID],securityGroups=[YOUR_SG_ID],assignPublicIp=ENABLED}" \
  --region eu-west-1
```

**Step 8: Create API Gateway**
- Go to AWS Console → API Gateway → Create API → HTTP API
- Add integrations pointing to each ECS service's load balancer
- Routes:
  - `POST /api/auth/*` → auth-service
  - `POST /api/rides/*` → booking-service
  - `POST /api/drivers/*` → matching-service

**Step 9: Deploy the React frontend to S3 + CloudFront**
```bash
# Build with production API URL
cd frontend
VITE_AUTH_SERVICE_URL=https://your-api-gateway-id.execute-api.eu-west-1.amazonaws.com npm run build

# Create S3 bucket
aws s3 mb s3://ridehailing-frontend-yourname --region eu-west-1

# Upload
aws s3 sync dist/ s3://ridehailing-frontend-yourname

# Enable static website hosting
aws s3 website s3://ridehailing-frontend-yourname \
  --index-document index.html \
  --error-document index.html
```

Create a CloudFront distribution pointing to this S3 bucket via the AWS Console (CloudFront → Create distribution → Origin domain → select your S3 bucket).

---

## 11. docker-compose for Local Dev

This is the single file that spins up your entire system locally with one command.

`docker-compose.yml` (in root `ride-hailing/` folder):

```yaml
version: '3.8'

services:
  # --- Infrastructure ---
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_PASSWORD: postgres
      POSTGRES_USER: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on: [zookeeper]
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  # --- Services ---
  auth-service:
    build: ./auth-service
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/authdb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      JWT_SECRET: local-dev-secret-key-change-in-production-min32
    depends_on: [postgres]

  booking-service:
    build: ./booking-service
    ports:
      - "8082:8082"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bookingdb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      AUTH_SERVICE_URL: http://auth-service:8081
    depends_on: [postgres, kafka]

  matching-service:
    build: ./matching-service
    ports:
      - "8083:8083"
    environment:
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    depends_on: [redis, kafka]

  fare-service:
    build: ./fare-service
    ports:
      - "8084:8084"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/faredb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    depends_on: [postgres, kafka]

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on: [auth-service]

volumes:
  postgres_data:
```

`init-db.sql` (creates the databases):
```sql
CREATE DATABASE authdb;
CREATE DATABASE bookingdb;
CREATE DATABASE faredb;
```

**Run everything:**
```bash
docker-compose up --build
```

Open http://localhost:3000 — your login page will appear.

---

## 12. README Template for GitHub

Your README.md is what a recruiter sees first. Copy this structure:

```markdown
# 🚕 RideHailing — Distributed Microservices Backend

A production-grade ride-hailing platform demonstrating microservices architecture,
event-driven design, and cloud-native deployment. Spin up the entire system with one command.

## Quick Start

\`\`\`bash
git clone https://github.com/yourusername/ride-hailing.git
cd ride-hailing
docker-compose up --build
\`\`\`
Open http://localhost:3000 for the React login UI.
Open http://localhost:8081/swagger-ui.html for auth-service API docs.

---

## System Architecture

[include the architecture diagram image here]

**Data flow**: React → API Gateway → Spring Boot services → Kafka → PostgreSQL/Redis

---

## Services

| Service | Port | Purpose | Key Technologies |
|---|---|---|---|
| auth-service | 8081 | Login, registration, JWT | Spring Security, BCrypt, JJWT |
| booking-service | 8082 | Ride lifecycle, payments | Saga pattern, Resilience4j |
| matching-service | 8083 | GPS tracking, driver matching | Redis GEOADD, Kafka |
| fare-service | 8084 | Dynamic pricing | Java 21 Virtual Threads |

---

## Design Trade-offs

**Why Redis for driver locations?**
Driver GPS updates arrive every 2–5 seconds per driver. Using PostgreSQL for this
would create thousands of writes per second and slow read queries. Redis GEOADD/GEORADIUS
provides sub-millisecond read/write latency for geospatial queries and keeps the main
database free for transactional ride data.

**Why Kafka instead of direct HTTP calls between services?**
HTTP couples services together — if matching-service is slow, booking-service blocks.
Kafka decouples them: booking-service publishes a ride-requested event and returns
immediately; matching-service processes it when ready. This gives us resilience,
replay-ability (reprocess failed events), and the ability to add new consumers (analytics,
notifications) without touching existing services.

**Why Optimistic Locking for ride bookings?**
Two riders could theoretically try to book the same available driver slot simultaneously.
Using @Version on the Ride entity ensures that if two concurrent updates conflict, one
gets an OptimisticLockException and retries — no deadlocks, no lost updates, minimal overhead.

**Why ZGC for matching-service?**
The matching service processes thousands of GPS coordinate updates per minute. Traditional
GC (G1, Parallel) can pause threads for 50-200ms during collection — long enough to miss
matching a nearby driver. ZGC targets <1ms pauses by doing most collection concurrently
with application threads.

---

## Failure Modes

**What if matching-service crashes mid-dispatch?**
The ride-requests Kafka topic is durable with auto-offset-commit disabled.
If matching-service crashes, on restart it re-reads from its last committed offset
and reprocesses the event. The booking-service uses idempotency checks (rideId already
in MATCHED state → skip) to handle duplicate events.

**What if the payment gateway times out?**
booking-service wraps payment calls with a Resilience4j Circuit Breaker.
After 5 failures in 10 seconds, the breaker opens and immediately calls
queuePayment() fallback, which queues the charge for async retry.

---

## AWS Deployment

Deployed on AWS using free-tier services:
- **ECS Fargate** — containerized services (no EC2 to manage)
- **RDS PostgreSQL t3.micro** — transactional data
- **ElastiCache Redis t3.micro** — driver location cache
- **API Gateway** — routes and authentication
- **S3 + CloudFront** — React frontend CDN
- **CloudWatch** — centralized logs and JVM metrics
```

---

## Quick Reference: Ports

| Service | Local Port |
|---|---|
| React Frontend | 3000 |
| auth-service | 8081 |
| booking-service | 8082 |
| matching-service | 8083 |
| fare-service | 8084 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 |
