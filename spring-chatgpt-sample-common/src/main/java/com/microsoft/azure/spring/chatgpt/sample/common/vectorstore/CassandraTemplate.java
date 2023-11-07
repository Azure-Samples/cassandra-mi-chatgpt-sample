package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.CqlVector;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CassandraTemplate {

    CqlSession cassandraSession;
    String vectorstore;
    String keyspace;

    public CassandraTemplate() throws IOException {
        //CREATE TABLE vectorstore (id text PRIMARY KEY, hash text, text text, embedding vector <float, 1536> );
        Configurations config = new Configurations();
        String dc = config.getProperty("DC");
        this.vectorstore = config.getProperty("table");
        this.keyspace = config.getProperty("keyspace");
        cassandraSession = CqlSession.builder().withLocalDatacenter(dc).build();
        log.info("Creating keyspace and table if not exists...");
        cassandraSession.execute("CREATE KEYSPACE IF NOT EXISTS "+keyspace+" WITH replication = {'class': 'NetworkTopologyStrategy', '"+dc+"': '3'}");
        cassandraSession.execute("CREATE TABLE IF NOT EXISTS "+keyspace+"."+vectorstore+" (id text PRIMARY KEY, hash text, text text, embedding vector <float, 1536> )");
        log.info("Finished creating keyspace and table if not exists.");
    }

    public List<CassandraEntity> insertMany(List<CassandraEntity> entities) throws IOException, InterruptedException {
        final ExecutorService es = Executors.newCachedThreadPool();
        for (CassandraEntity entity : entities) {
            es.execute(() -> {
                try {
                    insert(entity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        es.shutdown();

        final boolean finished = es.awaitTermination(5, TimeUnit.MINUTES);

        if (finished) {
            log.info("Loaded {} rows of contextual vector search data", entities.size());
        } else {
            log.info("Timeout elapsed before completion of insertMany");
        }
        return entities;
    }

    public CassandraEntity insert(CassandraEntity entity) throws IOException {

        CqlVector vector = CqlVector.newInstance(entity.getEmbedding());
        cassandraSession.execute("INSERT INTO "+keyspace+"."+vectorstore+" (id, hash, text, embedding) VALUES (?, ?, ?, ?)",
                entity.getId(), entity.getHash(), entity.getText(), vector);
        return entity;
    }
    public CassandraEntity selectOneById(Object id) {
        CassandraEntity doc = new CassandraEntity();
        doc = cassandraSession.execute("SELECT * FROM "+keyspace+"."+vectorstore+" WHERE id = ?", id).one().get(0, CassandraEntity.class);
        return doc;
    }

    public void insertVector(String preparedStatement, int id, String text, String hash, String embedding) {
        PreparedStatement prepared = cassandraSession.prepare(preparedStatement);
        BoundStatement bound = prepared.bind(id, text, hash, embedding).setIdempotent(true);
        cassandraSession.execute(bound);
    }
    public boolean deleteById(Object id) {

        cassandraSession.execute("DELETE FROM "+keyspace+"."+vectorstore+" WHERE id = ?", id);
        return true;
    }

    public Row selectOne() {
        Row row = cassandraSession.execute("SELECT * FROM "+keyspace+"."+vectorstore+" LIMIT 1").one();
        return row;
    }

    public List<CassandraEntity> select(String cql) {
        List<CassandraEntity> docs = cassandraSession.execute(cql).all().stream().map(row -> {
            CassandraEntity doc = new CassandraEntity();
            doc.setId(row.getString("id"));
            doc.setHash(row.getString("hash"));
            doc.setText(row.getString("text"));
            CqlVector<Float> embedding = row.get("embedding", CqlVector.class);
            Vector<Float> vector = new Vector<>();
            for (int i = 0; i < embedding.size(); i++) {
                vector.add(embedding.get(i));
            }
            doc.setEmbedding(vector);
            return doc;
        }).collect(java.util.stream.Collectors.toList());
        return docs;
    }
}
