# 🧩 Dynamic JSON Server for Java (Javalin)

A lightweight, annotation-free JSON API server built using **Javalin**, with support for:

- 🔧 Dynamic entity creation at runtime
- 📄 Auto-generated OpenAPI (Swagger) docs
- 📤 Image upload support with file validation
- 🧪 Clean, minimal API surface ideal for prototyping, mocking, or microservice bootstrapping

---

## 🚀 Features

- ✅ Create any entity (like `books`, `authors`) dynamically
- ✅ Instant CRUD endpoints without restarting the server
- ✅ Swagger UI with OpenAPI 3.0 spec (served from classpath)
- ✅ Secure image upload with:
  - MIME & extension checks
  - File size limits
  - Conflict prevention
- ✅ Builder-style configuration
- ✅ No annotations, no reflection, no magic

---

## 🔧 How to Use

### 1. Add as Dependency (Coming Soon)
You can use this project as a **Maven dependency** once published:

```xml
<dependency>
    <groupId>dev.ruthvik</groupId>
    <artifactId>json-server</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

*(Until then, build locally or include source)*

---

### 2. Initialize the Server

```java
JsonServer server = JsonServer.builder()
    .port(7070)
    .maxRequestSize(5 * 1024 * 1024) // Optional
    .uploadDir(Paths.get("my/uploads")) // Optional
    .build();

server.run();
```

---

### 3. Create a New Entity

```http
POST /entities
Content-Type: application/json

{
  "entityName": "books"
}
```

✅ This adds `/books` endpoint with full CRUD.

---

### 4. Perform CRUD Operations

Assuming you created `books`, the following endpoints are available:

- `GET    /books`
- `POST   /books`
- `GET    /books/{id}`
- `PUT    /books/{id}`
- `DELETE /books/{id}`

No need to define schema — entities are schemaless!

---

### 5. Upload an Image

```http
POST /upload
Content-Type: multipart/form-data

Form field: image = (select a `.jpg`, `.png`, etc.)
```

Success response:

```json
{
  "url": "/uploads/my-image.jpg"
}
```

🛡️ File validations:
- Only `image/*` MIME types
- Blocks dangerous extensions like `.html`, `.js`
- Max upload size is configurable

---

### 6. View Swagger UI

> 📄 Auto-generated from your active entities

Visit:

```
http://localhost:7070/swagger
```

It includes:
- `/entities` management
- `/upload` documentation
- All dynamic entity routes

---


## 📝 OpenAPI

- Generated to: `src/main/resources/swagger/swagger.yml`
- Automatically refreshed on server startup
- UI available at `/swagger`

---

## 🔐 Security Notes

- Image upload is locked to `image/*` content type
- Dangerous extensions are blocked
- File name is sanitized (`trim`, slash removal, etc.)
- Existing file names trigger `409 Conflict`

---

## 📦 TODO (Optional Enhancements)

- [ ] Publish to Maven Central / Jitpack
- [ ] Add CLI or GUI frontend
- [ ] Add tests & CI pipeline
- [ ] Entity schema validation (optional strict mode)

---

## 👤 Author

**Ruthvik**  
📫 [GitHub](https://github.com/Ruthvik0) | [ruthvikalladi136@gmail.com](mailto:ruthvikalladi136@gmail.com)

---

## 🧪 License

MIT – Free to use, modify, and contribute.

---
