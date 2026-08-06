package com.phanileela.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphService {

    private final Driver driver;

    public GraphService(Driver driver) {
        this.driver = driver;
    }

    public Map<String, Object> getGraphData() {
        try (Session session = driver.session()) {
            // Fetch nodes: MenuItem, Ingredient, Category, Allergen
            Result nodesResult = session.run(
                "MATCH (n) " +
                "WHERE n:MenuItem OR n:Ingredient OR n:Category OR n:Allergen " +
                "RETURN n, labels(n) as nodeLabels LIMIT 150"
            );

            List<Map<String, Object>> nodes = new ArrayList<>();
            while (nodesResult.hasNext()) {
                Record record = nodesResult.next();
                Node node = record.get("n").asNode();
                List<Object> labels = record.get("nodeLabels").asList();
                String label = labels.isEmpty() ? "Unknown" : labels.get(0).toString();

                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("id", node.get("id").asString("n-" + node.id()));
                nodeData.put("type", label.toLowerCase());

                switch (label) {
                    case "MenuItem" -> {
                        nodeData.put("label", node.get("name").asString());
                        nodeData.put("emoji", node.get("imageEmoji").asString("\uD83C\uDF7D"));
                        nodeData.put("price", node.get("price").asInt(0));
                        nodeData.put("isVeg", node.get("isVeg").asBoolean(true));
                    }
                    case "Ingredient" -> {
                        nodeData.put("label", node.get("name").asString());
                        nodeData.put("ingredientType", node.get("type").asString(""));
                    }
                    case "Category" -> {
                        nodeData.put("label", node.get("name").asString());
                        nodeData.put("icon", node.get("icon").asString("\uD83C\uDF7D"));
                    }
                    case "Allergen" -> nodeData.put("label", node.get("name").asString());
                    default -> nodeData.put("label", node.get("name").asString("Unknown"));
                }

                nodes.add(Map.of("data", nodeData));
            }

            // Fetch relationships between these node types
            Result relsResult = session.run(
                "MATCH (n)-[r]->(m) " +
                "WHERE (n:MenuItem OR n:Ingredient OR n:Category OR n:Allergen) " +
                "AND (m:MenuItem OR m:Ingredient OR m:Category OR m:Allergen) " +
                "RETURN n.id as sourceId, m.id as targetId, type(r) as relType LIMIT 300"
            );

            List<Map<String, Object>> edges = new ArrayList<>();
            Set<String> edgeIds = new HashSet<>();
            while (relsResult.hasNext()) {
                Record record = relsResult.next();
                String sourceId = record.get("sourceId").asString("");
                String targetId = record.get("targetId").asString("");
                String relType = record.get("relType").asString();

                if (!sourceId.isEmpty() && !targetId.isEmpty()) {
                    String edgeId = sourceId + "-" + relType + "-" + targetId;
                    if (!edgeIds.contains(edgeId)) {
                        edgeIds.add(edgeId);
                        edges.add(Map.of("data", Map.of(
                            "id", edgeId,
                            "source", sourceId,
                            "target", targetId,
                            "label", relType.replace("_", " ")
                        )));
                    }
                }
            }

            return Map.of("nodes", nodes, "edges", edges);
        }
    }
}
