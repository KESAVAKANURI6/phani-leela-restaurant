package com.phanileela.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.neo4j.driver.Values.parameters;

@Service
public class OrderService {

    private final Driver driver;

    public OrderService(Driver driver) {
        this.driver = driver;
    }

    public List<Map<String, Object>> getAllOrders() {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (o:Order) " +
                "OPTIONAL MATCH (o)-[oc:CONTAINS]->(m:MenuItem) " +
                "RETURN o.id as id, o.tableNumber as tableNumber, o.status as status, " +
                "o.totalAmount as totalAmount, o.createdAt as createdAt, o.customerName as customerName, " +
                "o.paymentMethod as paymentMethod, o.paymentStatus as paymentStatus, " +
                "collect({name: m.name, emoji: m.imageEmoji, quantity: oc.quantity, price: m.price}) as items " +
                "ORDER BY o.createdAt DESC LIMIT 30"
            );
            return result.list(r -> {
                Map<String, Object> order = new HashMap<>();
                order.put("id", r.get("id").asString());
                order.put("tableNumber", r.get("tableNumber").asString());
                order.put("status", r.get("status").asString());
                order.put("totalAmount", r.get("totalAmount").asInt(0));
                order.put("createdAt", r.get("createdAt").asString(""));
                order.put("customerName", r.get("customerName").asString("Guest"));
                order.put("paymentMethod", r.get("paymentMethod").asString("UPI"));
                order.put("paymentStatus", r.get("paymentStatus").asString("PAID"));
                order.put("items", r.get("items").asList());
                return order;
            });
        }
    }

    public Map<String, Object> placeOrder(Map<String, Object> orderRequest) {
        try (Session session = driver.session()) {
            String orderId = "order-" + System.currentTimeMillis();
            String tableNumber = (String) orderRequest.getOrDefault("tableNumber", "T1");
            String customerName = (String) orderRequest.getOrDefault("customerName", "Guest");
            String specialNote = (String) orderRequest.getOrDefault("specialNote", "");
            String paymentMethod = (String) orderRequest.getOrDefault("paymentMethod", "UPI");
            String paymentStatus = (String) orderRequest.getOrDefault("paymentStatus", "PAID");
            String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderRequest.get("items");
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("Order must contain at least one item");
            }

            // Create Order node with parameterized query
            session.run(
                "CREATE (o:Order {id: $orderId, tableNumber: $tableNumber, customerName: $customerName, " +
                "specialNote: $note, status: 'Preparing', createdAt: $createdAt, totalAmount: 0, " +
                "paymentMethod: $paymentMethod, paymentStatus: $paymentStatus})",
                parameters("orderId", orderId, "tableNumber", tableNumber,
                           "customerName", customerName, "note", specialNote, "createdAt", createdAt,
                           "paymentMethod", paymentMethod, "paymentStatus", paymentStatus)
            );

            int total = 0;
            for (Map<String, Object> item : items) {
                String dishId = item.containsKey("id") ? (String) item.get("id") : (String) item.get("menuItemId");
                int quantity = ((Number) item.getOrDefault("quantity", 1)).intValue();

                Result priceResult = session.run(
                    "MATCH (m:MenuItem {id: $dishId}) RETURN m.price as price",
                    parameters("dishId", dishId)
                );
                if (priceResult.hasNext()) {
                    int price = priceResult.next().get("price").asInt();
                    total += price * quantity;
                    session.run(
                        "MATCH (o:Order {id: $orderId}), (m:MenuItem {id: $dishId}) " +
                        "CREATE (o)-[:CONTAINS {quantity: $qty, subtotal: $subtotal}]->(m)",
                        parameters("orderId", orderId, "dishId", dishId,
                                   "qty", quantity, "subtotal", price * quantity)
                    );
                }
            }

            session.run(
                "MATCH (o:Order {id: $orderId}) SET o.totalAmount = $total",
                parameters("orderId", orderId, "total", total)
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("totalAmount", total);
            response.put("status", "Preparing");
            response.put("paymentMethod", paymentMethod);
            response.put("paymentStatus", paymentStatus);
            response.put("message", "Order placed successfully! Your food is being prepared.");
            return response;
        }
    }

    public Map<String, Object> getOrderById(String id) {
        try (Session session = driver.session()) {
            Result result = session.run(
                "MATCH (o:Order {id: $id}) " +
                "OPTIONAL MATCH (o)-[oc:CONTAINS]->(m:MenuItem)-[:IN_CATEGORY]->(c:Category) " +
                "RETURN o.id as id, o.tableNumber as tableNumber, o.status as status, " +
                "o.totalAmount as totalAmount, o.createdAt as createdAt, " +
                "o.customerName as customerName, o.specialNote as specialNote, " +
                "o.paymentMethod as paymentMethod, o.paymentStatus as paymentStatus, " +
                "collect({name: m.name, emoji: m.imageEmoji, quantity: oc.quantity, " +
                "price: m.price, subtotal: oc.subtotal, category: c.name}) as items",
                parameters("id", id)
            );
            if (!result.hasNext()) throw new IllegalArgumentException("Order not found: " + id);
            Record r = result.next();
            Map<String, Object> order = new HashMap<>();
            order.put("id", r.get("id").asString());
            order.put("tableNumber", r.get("tableNumber").asString());
            order.put("status", r.get("status").asString());
            order.put("totalAmount", r.get("totalAmount").asInt());
            order.put("createdAt", r.get("createdAt").asString());
            order.put("customerName", r.get("customerName").asString());
            order.put("specialNote", r.get("specialNote").asString(""));
            order.put("paymentMethod", r.get("paymentMethod").asString("UPI"));
            order.put("paymentStatus", r.get("paymentStatus").asString("PAID"));
            order.put("items", r.get("items").asList());
            return order;
        }
    }

    public Map<String, Object> updateOrderStatus(String id, String status) {
        try (Session session = driver.session()) {
            session.run(
                "MATCH (o:Order {id: $id}) SET o.status = $status",
                parameters("id", id, "status", status)
            );
            return Map.of("success", true, "id", id, "status", status);
        }
    }

    public Map<String, Object> updatePaymentStatus(String id, String paymentStatus) {
        try (Session session = driver.session()) {
            session.run(
                "MATCH (o:Order {id: $id}) SET o.paymentStatus = $paymentStatus",
                parameters("id", id, "paymentStatus", paymentStatus)
            );
            return Map.of("success", true, "id", id, "paymentStatus", paymentStatus);
        }
    }

    public Map<String, Object> deleteOrder(String id) {
        try (Session session = driver.session()) {
            session.run(
                "MATCH (o:Order {id: $id}) DETACH DELETE o",
                parameters("id", id)
            );
            return Map.of("success", true, "id", id);
        }
    }

    public Map<String, Object> deleteAllOrders() {
        try (Session session = driver.session()) {
            session.run("MATCH (o:Order) DETACH DELETE o");
            return Map.of("success", true, "message", "All orders removed successfully");
        }
    }
}
