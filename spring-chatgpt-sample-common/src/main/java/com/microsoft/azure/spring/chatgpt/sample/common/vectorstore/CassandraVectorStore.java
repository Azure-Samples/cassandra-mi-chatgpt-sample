package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.CqlVector;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CassandraVectorStore implements VectorStore {
    private final VectorStoreData data;

    private CassandraTemplate cassandraTemplate;

    private List<String > keys = new ArrayList<>();

    public CassandraVectorStore() throws IOException {
        this.data = new VectorStoreData();
        this.cassandraTemplate =  new CassandraTemplate();
    }
    @Override
    public void saveDocument(String key, CassandraEntity row) throws IOException {
        cassandraTemplate.insert(row);
    }

    public void storeKey(String key) {
        cassandraTemplate.cassandraSession.execute("INSERT INTO "+cassandraTemplate.keyspace+".keys (partitionKey, id) VALUES ('all keys', '"+key+"')");
    }
    public void retrieveKeys () {
        ResultSet resultSet = cassandraTemplate.cassandraSession.execute("SELECT id FROM "+cassandraTemplate.keyspace+".keys where partitionKey = 'all keys'");
        for (Row row : resultSet) {
            String id = row.getString("id");
            keys.add(id);
        }
    }
    public List<String> getKeys() {
        return keys;
    }

    @Override
    public void saveDocuments(List<CassandraEntity> rows) throws IOException {
        try {
            cassandraTemplate.insertMany(rows);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CassandraEntity getDocument(String key) {
        var doc = cassandraTemplate.selectOneById(key);
        return doc;
    }

    @Override
    public void removeDocument(String key) {
        cassandraTemplate.deleteById(key);
    }

    @Override
    public List<CassandraEntity> searchTopKNearest(Vector<Float> embedding, int k) {
        return searchTopKNearest(embedding, k, 0);
    }

    @Override
    public List<CassandraEntity> searchTopKNearest(Vector<Float> embedding, int k, double cutOff) {
        // perform vector search in cassandra
        CqlVector<Float> openaiembedding = CqlVector.newInstance(embedding);
        List<CassandraEntity> result = new ArrayList<>();
        log.info("Getting top {} nearest neighbors - Cassandra MI 5.0 Vector Search",k );
        List <String> keys = getKeys();
        if (keys.size() > 1) {
            log.warn("*** The vector store is partitioned by the file name for each data file loaded into it. ");
            log.warn("*** You have loaded more than one file into the vector store.");
            log.warn("*** This might be ok, but with current design, each vector search will be a cross partition key query. ");
            log.warn("*** Consider loading your data from a single file, or re-designing the data model to limit cross partition key queries. ");
        }
        ResultSet resultSet = cassandraTemplate.cassandraSession.execute("SELECT partitionKey, id, hash, text, embedding, similarity_cosine(embedding, ?) as similarity FROM "+cassandraTemplate.keyspace+"."+cassandraTemplate.vectorstore+" ORDER BY embedding ANN OF ? LIMIT "+k+"", openaiembedding, openaiembedding);
        for (Row row : resultSet) {
            String partitionKey = row.getString("partitionKey");
            String id = row.getString("id");
            String hash = row.getString("hash");
            String text = row.getString("text");
            CqlVector<Float> embedding1 =  row.get("embedding", CqlVector.class);
            Vector<Float> embedding2 = new Vector<>();
            for (Float f : embedding1) {
                embedding2.add(f);
            }
            CassandraEntity cassandraEntity = new CassandraEntity(partitionKey, id, hash, text, embedding2);
            result.add(cassandraEntity);
        }

        log.info("Embedding search complete");
        return result;
    }

    public void createVectorIndex() {
        String statement = "CREATE CUSTOM INDEX vectorstore_embedding_idx ON "+cassandraTemplate.keyspace+"."+cassandraTemplate.vectorstore+" (embedding) USING 'StorageAttachedIndex';";
        cassandraTemplate.cassandraSession.execute(statement);
    }

    public List<CassandraEntity> loadFromJsonFile(String filePath, String fileName) {
        var reader = new ObjectMapper().reader();
        try {
            Row FirstDocFound = cassandraTemplate.selectOne();
            if (FirstDocFound == null) {
                log.info("No vector search data found. Loading default data from file: {} .....", filePath);
                var data = reader.readValue(new File(filePath), VectorStoreData.class);
                List<CassandraEntity> list = new ArrayList<CassandraEntity>(data.store.values());
                list.forEach(doc -> {
                    doc.setPartitionKey(fileName);
                });
                cassandraTemplate.insertMany(list);
                //store the partition keys in a separate table
                cassandraTemplate.cassandraSession.execute("INSERT INTO "+cassandraTemplate.keyspace+".keys (partitionKey, id) VALUES ('all keys','"+fileName+"')");
                try {
                    createVectorIndex();
                }
                catch (Exception e) {
                    log.info("Index already exists");
                }
                return list;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Setter
    @Getter
    private static class VectorStoreData {
        private Map<String, CassandraEntity> store = new ConcurrentHashMap<>();
    }
}
