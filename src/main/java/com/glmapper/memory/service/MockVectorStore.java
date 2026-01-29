/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.glmapper.memory.service;

import com.glmapper.memory.model.Msg;
import com.glmapper.memory.model.MsgRole;
import com.glmapper.memory.model.TextBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock vector store for simulating RAG retrieval in demos.
 *
 * <p>This mock implementation simulates vector search by matching keywords
 * in predefined documents. In production, this would be replaced with
 * a real vector database like Milvus, Pinecone, or Elasticsearch.
 */
public class MockVectorStore {

    private static final Logger log = LoggerFactory.getLogger(MockVectorStore.class);

    // Mock document database with predefined documents
    private final Map<String, List<Msg>> documentDatabase = new HashMap<>();

    public MockVectorStore() {
        initializeDocuments();
    }

    /**
     * Searches for documents relevant to the query.
     *
     * @param query the search query
     * @param topK number of top results to return
     * @return list of relevant documents as messages
     */
    public List<Msg> search(String query, int topK) {
        log.info("Vector search: query='{}', topK={}", query, topK);

        List<Msg> results = new ArrayList<>();

        // Simple keyword matching simulation
        String lowerQuery = query.toLowerCase();

        // Check for keyword matches in our document database
        for (Map.Entry<String, List<Msg>> entry : documentDatabase.entrySet()) {
            String keywords = entry.getKey();
            if (containsKeyword(keywords, lowerQuery)) {
                log.debug("Found match in category: {}", keywords);
                results.addAll(entry.getValue());
                if (results.size() >= topK) {
                    break;
                }
            }
        }

        log.info("Vector search returned {} results", results.size());
        return results;
    }

    /**
     * Adds a document to the vector store.
     *
     * @param category document category/keywords
     * @param documents documents to add
     */
    public void addDocuments(String category, List<Msg> documents) {
        documentDatabase.put(category.toLowerCase(), documents);
        log.info("Added {} documents to category '{}'", documents.size(), category);
    }

    private boolean containsKeyword(String keywords, String query) {
        String[] keywordArray = keywords.toLowerCase().split(",");
        for (String keyword : keywordArray) {
            if (query.contains(keyword.trim())) {
                return true;
            }
        }
        return false;
    }

    private void initializeDocuments() {
        log.info("Initializing mock vector store with sample documents...");

        // Java programming documents
        List<Msg> javaDocs = new ArrayList<>();
        javaDocs.add(
                createDocument(
                        "java",
                        "Java is a high-level, class-based, object-oriented programming language.",
                        "Key features: Platform independence, automatic memory management, strong typing, multi-threading."));
        javaDocs.add(
                createDocument(
                        "java",
                        "Spring Boot is built on the Spring framework and provides production-ready infrastructure.",
                        "Features: Auto-configuration, embedded servers, metrics, health checks, externalized configuration."));
        javaDocs.add(
                createDocument(
                        "java",
                        "Redis is an in-memory data structure store, used as database, cache, message broker, and queue.",
                        "Key features: Fast performance, atomic operations, pub/sub messaging, data persistence, clustering."));

        // Python programming documents
        List<Msg> pythonDocs = new ArrayList<>();
        pythonDocs.add(
                createDocument(
                        "python",
                        "Python is a high-level programming language known for its simplicity and readability.",
                        "Key features: Dynamic typing, extensive standard library, framework support (Django, Flask, FastAPI), data science (pandas, numpy)."));
        pythonDocs.add(
                createDocument(
                        "python",
                        "FastAPI is a modern web framework for building APIs with Python 3.7+.",
                        "Features: Automatic API documentation, type hints, dependency injection, WebSocket support, async/await."));

        // Database documents
        List<Msg> dbDocs = new ArrayList<>();
        dbDocs.add(
                createDocument(
                        "database",
                        "MongoDB is a document-oriented NoSQL database designed for flexibility and scalability.",
                        "Features: BSON format, rich query language, horizontal scaling, aggregation framework, geospatial queries."));
        dbDocs.add(
                createDocument(
                        "database",
                        "PostgreSQL is a powerful open-source relational database.",
                        "Features: ACID compliance, complex joins, window functions, full-text search, JSON support, extensions."));

        // Add documents to store
        addDocuments("java,programming,framework,spring,redis", javaDocs);
        addDocuments("python,programming,framework,fastapi,web", pythonDocs);
        addDocuments("database,nosql,mongodb,postgresql,mysql", dbDocs);

        log.info("Mock vector store initialized with {} document categories", documentDatabase.size());
    }

    private Msg createDocument(String category, String summary, String details) {
        return Msg.builder()
                .role(MsgRole.SYSTEM)
                .name("vector-store")
                .content(
                        TextBlock.of(
                                String.format(
                                        "[Document Category: %s]\nSummary: %s\nDetails: %s",
                                        category, summary, details)))
                .build();
    }
}
