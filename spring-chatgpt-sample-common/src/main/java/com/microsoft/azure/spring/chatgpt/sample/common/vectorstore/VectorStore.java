package com.microsoft.azure.spring.chatgpt.sample.common.vectorstore;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public interface VectorStore {
    void saveRow(String key, CassandraEntity row) throws IOException;

    void saveRows(List<CassandraEntity> rows) throws IOException;

    CassandraEntity getRow(String key);

    void removeRow(String key);

    List<CassandraEntity> searchTopKNearest(Vector<Float> embedding, int k);

    List<CassandraEntity> searchTopKNearest(Vector<Float> embedding, int k, double cutOff);
}
