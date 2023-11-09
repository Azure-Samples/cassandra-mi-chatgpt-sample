package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import java.util.Vector;

@Builder
@Jacksonized
public class CassandraEntity {

    private String partitionKey;
    private String id;
    private String hash;
    private String text;
    private Vector<Float> embedding;

    public CassandraEntity() {}

    public CassandraEntity(String partitionKey, String id, String hash, String text, Vector<Float> embedding) {
        this.id = id;
        this.hash = hash;
        this.text = text;
        this.embedding = embedding;
        this.partitionKey = partitionKey;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Vector<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Vector<Float> embedding) {
        this.embedding = embedding;
    }

    @Override
    public String toString() {
        return "DocEntry{" +
                "id='" + id + '\'' +
                ", hash='" + hash + '\'' +
                ", text='" + text + '\'' +
                ", embedding='" + embedding + '\'' +
                '}';
    }

}

