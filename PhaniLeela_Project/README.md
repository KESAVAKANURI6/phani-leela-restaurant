# 🪔 Phani Leela — Authentic Indian Restaurant

> A full-stack Indian restaurant application backed by **CognoDB** (Graph Database), built for the WEXA AI Take-Home Assignment.

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | HTML5, CSS3, Vanilla JavaScript |
| **Backend** | Java 17, Spring Boot 3.2, Maven |
| **Database** | CognoDB (Graph DB via Neo4j Java Driver 5.20) |
| **Graph Viz** | Cytoscape.js |

---

## 🌐 Why a Graph Database?

A relational database (MySQL) would struggle with these queries. A graph database handles them naturally:

| Feature | Graph Advantage |
|---|---|
| **Allergen Check** | 3-hop: `MenuItem → HAS_INGREDIENT → Ingredient → IS_ALLERGEN → Allergen` — one Cypher line vs 3 SQL JOINs |
| **Dish Recommendations** | `MenuItem -[PAIRS_WITH]-> MenuItem` — graph traversal, impossible natively in SQL |
| **Ingredient Network** | "Find all dishes sharing an ingredient" — multi-hop traversal |
| **Order History** | `Customer → PLACED → Order → CONTAINS → MenuItem → HAS_INGREDIENT → Ingredient` — 6-hop in one query |

---

## 📊 Graph Data Model

```
(MenuItem)-[:IN_CATEGORY]->(Category)
(MenuItem)-[:HAS_INGREDIENT {amount}]->(Ingredient)
(MenuItem)-[:PAIRS_WITH]->(MenuItem)
(Ingredient)-[:IS_ALLERGEN]->(Allergen)
(Order)-[:CONTAINS {quantity, subtotal}]->(MenuItem)
```

**Stats:**
- 30 Menu Items across 7 categories
- 51 Ingredients (Spices, Dairy, Proteins, Vegetables, Grains, Nuts)
- 5 Allergen types
- 40+ PAIRS_WITH recommendation relationships
- 5 pre-seeded sample orders

---

## 🚀 Running the Application

### Step 1: Set Environment Variables

```bash
# Windows PowerShell
$env:COGNODB_URI="bolt+s://YOUR_INSTANCE.databases.cognodb.cloud"
$env:COGNODB_USERNAME="cognodb"
$env:COGNODB_PASSWORD="your_password_here"
```

### Step 2: Build and Run

```bash
cd wexa-techpath
mvn spring-boot:run
```

### Step 3: Open Browser

```
http://localhost:8080
```

---

## 📱 Features

### 🍽️ Menu
- Browse 30 authentic Indian dishes
- Filter by category (Starters, Main Course, Biryani, etc.)
- Toggle Veg / Non-Veg
- **Allergen Filter** — 3-hop graph query removes unsafe dishes in real-time
- Search dishes by name

### 🛒 Cart & Ordering
- Click any dish → opens detail modal
- See ingredients, allergens, and **"Pairs Well With"** recommendations
- Add to cart, adjust quantities
- Select table number (T1–T10)
- Place order → saved to graph database

### 📋 Orders
- View all orders with table number, customer name, status
- Orders show status: Preparing / Served

### 🌐 Graph Explorer
- Interactive Cytoscape.js visualization
- See Menu Items (orange), Ingredients (green), Categories (purple), Allergens (red)
- Click any node to see details
- Zoom, pan, drag to explore the food knowledge graph

---

## 🔗 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/menu` | All menu items |
| GET | `/api/menu/{id}` | Dish detail + ingredients + PAIRS_WITH |
| GET | `/api/menu/safe?allergens=Dairy,Gluten` | Allergen-safe dishes (3-hop query) |
| GET | `/api/categories` | All categories with item counts |
| GET | `/api/stats` | Graph statistics |
| GET | `/api/orders` | All orders (recent 20) |
| POST | `/api/orders` | Place a new order |
| GET | `/api/graph` | Graph data for Cytoscape.js |

---

## 🏗️ Project Structure

```
wexa-techpath/
├── pom.xml
├── .env.example
├── README.md
└── src/main/
    ├── java/com/phanileela/
    │   ├── PhaniLeelaApplication.java
    │   ├── config/Neo4jConfig.java
    │   ├── exception/GlobalExceptionHandler.java
    │   ├── service/
    │   │   ├── MenuService.java
    │   │   ├── OrderService.java
    │   │   └── GraphService.java
    │   ├── controller/
    │   │   ├── MenuController.java
    │   │   ├── OrderController.java
    │   │   └── GraphController.java
    │   └── seed/RestaurantDataSeeder.java
    └── resources/
        ├── application.properties
        └── static/
            ├── index.html
            ├── css/styles.css
            └── js/
                ├── api.js
                ├── app.js
                └── graph.js
```

---

## 👨‍💻 Author

**KANURI KESAVA V V S N MURTHY**  
Full Stack Java Developer  
Codegnan IT Solutions, Hyderabad
