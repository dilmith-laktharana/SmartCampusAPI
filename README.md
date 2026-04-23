# Smart Campus API - JAX-RS RESTful Service

Student Name:R.P.Dilmith Laktharana 
Student Number: 20232357/w2153467
Module: 5COSC022W Client-Server Architectures  
Academic Year: 2025/26



##  Overview

This is a RESTful API built with JAX-RS (Jersey) for managing Rooms and Sensors in a Smart Campus environment. The API supports CRUD operations, nested resources (sensor readings), query-based filtering, and comprehensive error handling with custom exception mappers.

---

##  How to Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Build Instructions
```bash
# Clone the repository
git clone https://github.com/[YOUR-USERNAME]/SmartCampusAPI.git
cd SmartCampusAPI

# Build the project
mvn clean package

# Run the server
java -jar target/SmartCampusAPI-1.0-SNAPSHOT.jar
```

The API will start at: `http://localhost:8080/api/v1`

---

##  Sample cURL Commands

### 1. Discovery Endpoint
```bash
curl http://localhost:8080/api/v1
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

### 3. Create a Sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":22.5,"roomId":"LIB-301"}'
```

### 4. Filter Sensors by Type
```bash
curl http://localhost:8080/api/v1/sensors?type=Temperature
```

### 5. Post a Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.3}'
```

### 6. Get Reading History
```bash
curl http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

---

## Report: Answers to Coursework Questions

### Part 1: Setup & Discovery

#### 1.1 JAX-RS Resource Lifecycle
By default, JAX-RS creates a **new instance** of each resource class for every incoming HTTP request (per-request scope). This means instance variables are not shared between requests. To safely manage shared in-memory data like HashMaps of rooms and sensors, I implemented a **singleton DataStore class** that uses thread-safe `ConcurrentHashMap` collections. This prevents race conditions when multiple concurrent requests access or modify the shared data structures, ensuring data consistency without requiring manual synchronization in resource methods.

#### 1.2 HATEOAS and Hypermedia
HATEOAS (Hypermedia as the Engine of Application State) is a REST constraint where API responses include hyperlinks to related resources and possible actions. The Discovery endpoint demonstrates this by providing a map of available resource collections. This approach benefits client developers by:
- Reducing coupling between client and server (clients follow links rather than hardcoding URLs)
- Making the API self-documenting and discoverable
- Allowing the server to change URL structures without breaking clients
- Enabling dynamic navigation similar to browsing a website

### Part 2: Room Management

#### 2.1 Returning IDs vs Full Objects
When returning a list of rooms, there are trade-offs:
- **Returning only IDs:** Minimizes bandwidth and payload size, but forces clients to make N additional requests to fetch full details (N+1 problem), increasing latency.
- **Returning full objects:** Eliminates additional requests and provides all data upfront, but increases response size and may send unnecessary data.

The best approach depends on use case: for dashboards needing immediate display, full objects are preferable; for mobile apps with limited bandwidth or large lists, IDs with pagination are better. In this implementation, I return full objects for simplicity and reduced latency.

#### 2.2 DELETE Idempotency
Yes, the DELETE operation is idempotent in my implementation. Idempotency means that making the same request multiple times has the same effect as making it once. When a client sends DELETE /rooms/LIB-301:
1. **First request:** The room exists and is deleted → returns 200 OK
2. **Subsequent requests:** The room no longer exists → returns 404 Not Found

Although the HTTP status codes differ, the **server state is identical** after both requests (the room is gone). This satisfies the idempotency requirement: repeating the operation doesn't change the outcome beyond the initial application.

### Part 3: Sensors & Filtering

#### 3.1 @Consumes Annotation Behavior
If a client sends data in a format that doesn't match the `@Consumes(MediaType.APPLICATION_JSON)` annotation (such as `text/plain` or `application/xml`), JAX-RS automatically returns **HTTP 415 Unsupported Media Type** before the request reaches the resource method. The framework checks the `Content-Type` header and rejects incompatible requests immediately, providing built-in content negotiation without requiring manual validation in the code.

#### 3.2 Query Parameters vs Path Segments
Query parameters (`?type=CO2`) are semantically superior to path segments (`/sensors/type/CO2`) for filtering because:
- **Semantic clarity:** Query params indicate optional filtering criteria, while path segments suggest the type is part of the resource's identity
- **Composability:** Multiple filters can be combined naturally (`?type=CO2&status=ACTIVE`), whereas path segments would require complex nested routes
- **RESTful design:** The resource collection (`/sensors`) remains the same; query params refine the results without changing the resource identifier
- **Flexibility:** Query params are optional by nature, allowing the same endpoint to serve both filtered and unfiltered requests

### Part 4: Sub-Resources

#### 4.1 Sub-Resource Locator Benefits
The Sub-Resource Locator pattern provides significant architectural benefits:
- **Separation of Concerns:** Each resource class has a single, focused responsibility (SensorResource handles sensors, SensorReadingResource handles readings), following the Single Responsibility Principle
- **Maintainability:** Changes to reading logic don't require modifying the sensor resource class
- **Scalability:** As the API grows, delegating to specialized classes prevents monolithic controller classes that become unmanageable
- **Testability:** Sub-resource classes can be unit tested independently
- **Code organization:** Related operations are grouped logically without creating a massive controller with dozens of methods

Without this pattern, defining all nested paths (`/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}`, etc.) in one class would create tight coupling and make the codebase difficult to navigate.

### Part 5: Error Handling & Logging

#### 5.2 HTTP 422 vs 404 for Missing References
HTTP 422 (Unprocessable Entity) is more semantically accurate than 404 when a sensor references a non-existent room because:
- **404 Not Found** indicates the requested URL/resource doesn't exist
- In this case, `/api/v1/sensors` exists and is valid — the problem is the **payload content** (the roomId field references something that doesn't exist)
- **422 Unprocessable Entity** specifically means: "The server understood the request and the syntax is valid, but the data cannot be processed due to semantic errors"
- This gives the client precise information: their JSON is well-formed, but contains logically invalid data (a foreign key violation)

#### 5.4 Security Risks of Exposing Stack Traces
Exposing Java stack traces to external API consumers creates serious security vulnerabilities:
- **Internal structure disclosure:** Stack traces reveal package names, class hierarchies, and file paths, helping attackers map the application's architecture
- **Technology fingerprinting:** Library and framework names/versions in stack traces let attackers identify known CVEs and exploit specific vulnerabilities
- **Business logic exposure:** Method names and parameters may reveal proprietary algorithms or sensitive workflows
- **Database structure hints:** ORM stack traces can expose table names and relationships
- **Attack surface expansion:** Detailed error paths show attackers which inputs trigger which code paths, enabling targeted exploitation

The Global Exception Mapper prevents this by returning generic error messages externally while logging full details server-side for debugging.

#### 5.5 Benefits of Filters for Logging
Using JAX-RS filters for cross-cutting concerns like logging is advantageous because:
- **DRY Principle:** Logging logic exists in one place rather than duplicated across every resource method
- **Consistency:** All requests are logged uniformly, ensuring no endpoints are missed
- **Maintainability:** Changes to log format require editing only the filter, not dozens of methods
- **Completeness:** Filters log requests that fail early (404s, 415s, authentication failures) before reaching resource methods, providing full observability
- **Separation of concerns:** Business logic stays focused on domain operations, not infrastructure concerns like logging
- **Framework integration:** Filters have access to request/response contexts that individual methods don't


---

## 📦 Technology Stack

- **JAX-RS Implementation:** Jersey 2.41
- **Embedded Server:** Grizzly HTTP Server
- **Data Storage:** In-memory ConcurrentHashMap (no database)
- **Build Tool:** Maven
- **Java Version:** 11

---

## 👨‍💻 Author

Dilmith Laktharana 
20232357/w2153467
University of Westminster  
5COSC022W - Client-Server Architectures
