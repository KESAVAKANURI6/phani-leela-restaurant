package com.phanileela.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.neo4j.driver.Values.parameters;

@Service
public class MenuService {

    private final Driver driver;

    public MenuService(Driver driver) {
        this.driver = driver;
    }

    public List<Map<String, Object>> getAllMenuItems() {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (m:MenuItem)-[:IN_CATEGORY]->(c:Category) " +
                "OPTIONAL MATCH (m)-[:HAS_INGREDIENT]->(i:Ingredient)-[:IS_ALLERGEN]->(a:Allergen) " +
                "RETURN m.id as id, m.name as name, m.description as description, " +
                "m.price as price, m.imageEmoji as imageEmoji, m.imageUrl as imageUrl, m.isVeg as isVeg, " +
                "c.id as categoryId, c.name as categoryName, " +
                "collect(DISTINCT a.name) as allergens " +
                "ORDER BY c.sortOrder, m.name"
            );
            return result.list(r -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", r.get("id").asString());
                item.put("name", r.get("name").asString());
                item.put("description", r.get("description").asString(""));
                item.put("price", r.get("price").asInt());
                item.put("imageEmoji", r.get("imageEmoji").asString("\uD83C\uDF7D"));
                item.put("imageUrl", r.get("imageUrl").isNull() ? "" : r.get("imageUrl").asString(""));
                item.put("isVeg", r.get("isVeg").asBoolean(true));
                item.put("categoryId", r.get("categoryId").asString());
                item.put("categoryName", r.get("categoryName").asString());
                item.put("allergens", r.get("allergens").asList());
                return item;
            });
        }
    }

    public Map<String, Object> getMenuItemById(String id) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (m:MenuItem {id: $id})-[:IN_CATEGORY]->(c:Category) " +
                "OPTIONAL MATCH (m)-[hi:HAS_INGREDIENT]->(i:Ingredient) " +
                "OPTIONAL MATCH (i)-[:IS_ALLERGEN]->(a:Allergen) " +
                "RETURN m, c.name as categoryName, " +
                "collect(DISTINCT {id: i.id, name: i.name, type: i.type, amount: hi.amount}) as ingredients, " +
                "collect(DISTINCT a.name) as allergens",
                parameters("id", id)
            );
            if (!result.hasNext()) throw new IllegalArgumentException("Menu item not found: " + id);
            Record record = result.next();
            Node node = record.get("m").asNode();

            Map<String, Object> item = new HashMap<>();
            item.put("id", node.get("id").asString());
            item.put("name", node.get("name").asString());
            item.put("description", node.get("description").asString(""));
            item.put("price", node.get("price").asInt());
            item.put("imageEmoji", node.get("imageEmoji").asString("\uD83C\uDF7D"));
            item.put("isVeg", node.get("isVeg").asBoolean(true));
            item.put("categoryName", record.get("categoryName").asString());
            item.put("ingredients", record.get("ingredients").asList());
            item.put("allergens", record.get("allergens").asList());

            // PAIRS_WITH recommendations — 2-hop traversal: MenuItem -> PAIRS_WITH -> MenuItem -> IN_CATEGORY -> Category
            Result pairsResult = session.run(
                "MATCH (m:MenuItem {id: $id})-[:PAIRS_WITH]->(p:MenuItem)-[:IN_CATEGORY]->(c:Category) " +
                "RETURN p.id as id, p.name as name, p.price as price, " +
                "p.imageEmoji as imageEmoji, p.isVeg as isVeg, c.name as categoryName LIMIT 4",
                parameters("id", id)
            );
            List<Map<String, Object>> pairs = pairsResult.list(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.get("id").asString());
                m.put("name", r.get("name").asString());
                m.put("price", r.get("price").asInt());
                m.put("imageEmoji", r.get("imageEmoji").asString("\uD83C\uDF7D"));
                m.put("isVeg", r.get("isVeg").asBoolean(true));
                m.put("categoryName", r.get("categoryName").asString());
                return m;
            });

            if (pairs.isEmpty()) {
                Result fallbackResult = session.run(
                    "MATCH (p:MenuItem)-[:IN_CATEGORY]->(c:Category) " +
                    "WHERE p.id <> $id AND (c.id = 'cat-breads' OR c.id = 'cat-beverages' OR c.id = 'cat-icecream' OR c.id = 'cat-desserts') " +
                    "RETURN p.id as id, p.name as name, p.price as price, " +
                    "p.imageEmoji as imageEmoji, p.isVeg as isVeg, c.name as categoryName LIMIT 3",
                    parameters("id", id)
                );
                pairs = fallbackResult.list(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", r.get("id").asString());
                    m.put("name", r.get("name").asString());
                    m.put("price", r.get("price").asInt());
                    m.put("imageEmoji", r.get("imageEmoji").asString("\uD83C\uDF7D"));
                    m.put("isVeg", r.get("isVeg").asBoolean(true));
                    m.put("categoryName", r.get("categoryName").asString());
                    return m;
                });
            }
            item.put("pairsWith", pairs);
            return item;
        }
    }

    public List<Map<String, Object>> getMenuByCategory(String categoryId) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (m:MenuItem)-[:IN_CATEGORY]->(c:Category {id: $categoryId}) " +
                "OPTIONAL MATCH (m)-[:HAS_INGREDIENT]->(i:Ingredient)-[:IS_ALLERGEN]->(a:Allergen) " +
                "RETURN m.id as id, m.name as name, m.description as description, " +
                "m.price as price, m.imageEmoji as imageEmoji, m.isVeg as isVeg, " +
                "c.id as categoryId, c.name as categoryName, collect(DISTINCT a.name) as allergens ORDER BY m.name",
                parameters("categoryId", categoryId)
            );
            return result.list(r -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", r.get("id").asString());
                item.put("name", r.get("name").asString());
                item.put("description", r.get("description").asString(""));
                item.put("price", r.get("price").asInt());
                item.put("imageEmoji", r.get("imageEmoji").asString("\uD83C\uDF7D"));
                item.put("isVeg", r.get("isVeg").asBoolean(true));
                item.put("categoryId", r.get("categoryId").asString());
                item.put("categoryName", r.get("categoryName").asString());
                item.put("allergens", r.get("allergens").asList());
                return item;
            });
        }
    }

    /**
     * Allergen-safe menu search using a 3-hop graph traversal:
     * MenuItem -[:HAS_INGREDIENT]-> Ingredient -[:IS_ALLERGEN]-> Allergen
     * Returns only dishes that contain NONE of the specified allergens.
     * This multi-hop pattern would require awkward nested subqueries in SQL.
     */
    public List<Map<String, Object>> getAllergenSafeItems(List<String> allergenNames) {
        List<String> lowerAllergens = allergenNames.stream().map(String::toLowerCase).toList();
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (m:MenuItem)-[:IN_CATEGORY]->(c:Category) " +
                "OPTIONAL MATCH (m)-[:HAS_INGREDIENT]->(i:Ingredient)-[:IS_ALLERGEN]->(a:Allergen) " +
                "WHERE toLower(a.name) IN $allergens " +
                "WITH m, c, count(a) as dangerCount " +
                "WHERE dangerCount = 0 " +
                "OPTIONAL MATCH (m)-[:HAS_INGREDIENT]->(i2:Ingredient)-[:IS_ALLERGEN]->(a2:Allergen) " +
                "RETURN m.id as id, m.name as name, m.description as description, " +
                "m.price as price, m.imageEmoji as imageEmoji, m.isVeg as isVeg, " +
                "c.id as categoryId, c.name as categoryName, collect(DISTINCT a2.name) as allergens " +
                "ORDER BY c.sortOrder, m.name",
                parameters("allergens", lowerAllergens)
            );
            return result.list(r -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", r.get("id").asString());
                item.put("name", r.get("name").asString());
                item.put("description", r.get("description").asString(""));
                item.put("price", r.get("price").asInt());
                item.put("imageEmoji", r.get("imageEmoji").asString("\uD83C\uDF7D"));
                item.put("isVeg", r.get("isVeg").asBoolean(true));
                item.put("categoryId", r.get("categoryId").asString());
                item.put("categoryName", r.get("categoryName").asString());
                item.put("allergens", r.get("allergens").asList());
                return item;
            });
        }
    }

    public List<Map<String, Object>> getAllCategories() {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (c:Category) " +
                "OPTIONAL MATCH (c)<-[:IN_CATEGORY]-(m:MenuItem) " +
                "RETURN c.id as id, c.name as name, c.icon as icon, c.sortOrder as sortOrder, " +
                "count(m) as itemCount ORDER BY c.sortOrder"
            );
            return result.list(r -> {
                Map<String, Object> cat = new HashMap<>();
                cat.put("id", r.get("id").asString());
                cat.put("name", r.get("name").asString());
                cat.put("icon", r.get("icon").asString("\uD83C\uDF7D"));
                cat.put("sortOrder", r.get("sortOrder").asInt(0));
                cat.put("itemCount", r.get("itemCount").asLong());
                return cat;
            });
        }
    }

    public Map<String, Object> getStats() {
        try (Session session = driver.session()) {
            long menuCount = session.run("MATCH (m:MenuItem) RETURN count(m) as cnt").single().get("cnt").asLong();
            long ingCount = session.run("MATCH (i:Ingredient) RETURN count(i) as cnt").single().get("cnt").asLong();
            long orderCount = session.run("MATCH (o:Order) RETURN count(o) as cnt").single().get("cnt").asLong();
            long relCount = session.run("MATCH ()-[r]->() RETURN count(r) as cnt").single().get("cnt").asLong();

            // Most ordered dishes - 2-hop: Order -[CONTAINS]-> MenuItem
            Result popularResult = session.run(
                "MATCH (o:Order)-[:CONTAINS]->(m:MenuItem) " +
                "WITH m, count(o) as orderCount " +
                "ORDER BY orderCount DESC LIMIT 3 " +
                "RETURN m.name as name, m.imageEmoji as emoji, m.price as price, orderCount"
            );
            List<Map<String, Object>> popular = popularResult.list(r -> Map.of(
                "name", r.get("name").asString(),
                "emoji", r.get("emoji").asString("\uD83C\uDF7D"),
                "price", r.get("price").asInt(),
                "orderCount", r.get("orderCount").asLong()
            ));

            // Most used ingredients - 2-hop: MenuItem -[HAS_INGREDIENT]-> Ingredient
            Result topIngResult = session.run(
                "MATCH (m:MenuItem)-[:HAS_INGREDIENT]->(i:Ingredient) " +
                "WITH i, count(m) as usedIn " +
                "ORDER BY usedIn DESC LIMIT 5 " +
                "RETURN i.name as name, i.type as type, usedIn"
            );
            List<Map<String, Object>> topIng = topIngResult.list(r -> Map.of(
                "name", r.get("name").asString(),
                "type", r.get("type").asString(),
                "usedIn", r.get("usedIn").asLong()
            ));

            return Map.of(
                "totalMenuItems", menuCount,
                "totalIngredients", ingCount,
                "totalOrders", orderCount,
                "totalRelationships", relCount,
                "popularDishes", popular,
                "topIngredients", topIng
            );
        }
    }

    public Map<String, Object> addMenuItem(Map<String, Object> body) {
        try (Session session = driver.session()) {
            String id = "dish-" + System.currentTimeMillis();
            String name = (String) body.getOrDefault("name", "New Dish");
            int price = Integer.parseInt(body.getOrDefault("price", 100).toString());
            boolean isVeg = Boolean.parseBoolean(body.getOrDefault("isVeg", true).toString());
            String description = (String) body.getOrDefault("description", "");
            String emoji = (String) body.getOrDefault("emoji", "\uD83C\uDF7D");
            String imageUrl = (String) body.getOrDefault("imageUrl", "");
            String categoryId = (String) body.getOrDefault("categoryId", "cat-starters");

            session.run(
                "MERGE (m:MenuItem {id: $id}) " +
                "SET m.name = $name, m.price = $price, m.isVeg = $isVeg, " +
                "    m.description = $desc, m.imageEmoji = $emoji, m.imageUrl = $imageUrl " +
                "WITH m " +
                "MATCH (c:Category {id: $catId}) " +
                "MERGE (m)-[:IN_CATEGORY]->(c)",
                parameters("id", id, "name", name, "price", price, "isVeg", isVeg,
                           "desc", description, "emoji", emoji, "imageUrl", imageUrl, "catId", categoryId)
            );
            return Map.of("success", true, "id", id, "message", "Dish '" + name + "' added successfully!");
        }
    }

    public Map<String, Object> deleteMenuItem(String id) {
        try (Session session = driver.session()) {
            session.run(
                "MATCH (m:MenuItem {id: $id}) DETACH DELETE m",
                parameters("id", id)
            );
            return Map.of("success", true, "message", "Dish deleted successfully!");
        }
    }
}
