package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public interface VectorStore {
    void saveDocument(String key, CassandraEntity doc) throws IOException;

    void saveDocuments(List<CassandraEntity> docs) throws IOException;

    CassandraEntity getDocument(String key);

    void removeDocument(String key);

    List<CassandraEntity> searchTopKNearest(Vector<Float> embedding, int k);

    List<CassandraEntity> searchTopKNearest(Vector<Float> embedding, int k, double cutOff);
}
