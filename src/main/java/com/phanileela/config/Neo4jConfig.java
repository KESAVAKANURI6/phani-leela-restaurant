package com.phanileela.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jConfig.class);

    @Value("${cognodb.uri}")
    private String uri;

    @Value("${cognodb.username}")
    private String username;

    @Value("${cognodb.password}")
    private String password;

    @Bean
    public Driver neo4jDriver() {
        logger.info("Connecting to CognoDB at: {}", uri);
        try {
            Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
            driver.verifyConnectivity();
            logger.info("Successfully connected to CognoDB!");
            return driver;
        } catch (Exception e) {
            logger.warn("Could not verify CognoDB connectivity: {}. Will retry on first query.", e.getMessage());
            return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        }
    }
}
