# Booking Service – How to Run

### **1. Prerequisites**

Make sure you have installed:

* **JDK 11 or 17**
* **SBT**
* **MySQL**
* **Kafka + Zookeeper**

---

# **2. Start MySQL**

Create the schema:

```sql
CREATE DATABASE hotel_booking;
```

Update `application.conf` with your DB credentials:

```hocon
db.default.url="jdbc:mysql://localhost:3306/hotel_booking"
db.default.username="root"
db.default.password="your_password"
```

---

# **3. Start Kafka & Zookeeper**

### **Start Zookeeper**

```sh
zookeeper-server-start.sh config/zookeeper.properties
```

### **Start Kafka**

```sh
kafka-server-start.sh config/server.properties
```

### **Create Outbox Topic**

```sh
kafka-topics.sh --create --topic booking-outbox --bootstrap-server localhost:9092
```

---

# **4. Run Booking Service**

From project root:

```sh
cd booking-service
sbt run
```

Booking Service runs on:

```
http://localhost:9002
```

---

# **5. Booking APIs (Postman Collection)**

Use this collection to test all endpoints:

**Postman Collection:**
[https://tejakrishna-g-9710885.postman.co/workspace/Teja-Krishna-Gandam's-Workspace~c2fcbc57-358c-43f1-8a7d-6f28c1e682e8/collection/50250192-d3286a09-6256-435a-a9a8-a98a7372f827?action=share&creator=50250192](https://tejakrishna-g-9710885.postman.co/workspace/Teja-Krishna-Gandam's-Workspace~c2fcbc57-358c-43f1-8a7d-6f28c1e682e8/collection/50250192-d3286a09-6256-435a-a9a8-a98a7372f827?action=share&creator=50250192)

---

# **6. Refer to Project Tech Spec**

Full project specification is documented here:

[https://docs.google.com/document/d/11bjs6H4MYlR1RKGjhPuceq4ceioiJBp_seJ1LzujyH4/edit?tab=t.0#heading=h.bl9psd8tejpq](https://docs.google.com/document/d/11bjs6H4MYlR1RKGjhPuceq4ceioiJBp_seJ1LzujyH4/edit?tab=t.0#heading=h.bl9psd8tejpq)
