package com.microsoft.azure.spring.chatgpt.sample.common;

import com.microsoft.azure.spring.chatgpt.sample.common.reader.SimpleFolderReader;
import com.microsoft.azure.spring.chatgpt.sample.common.vectorstore.CassandraVectorStore;
import com.microsoft.azure.spring.chatgpt.sample.common.vectorstore.CassandraEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

@RequiredArgsConstructor
@Slf4j
public class DocumentIndexPlanner {
    @Autowired
    private final AzureOpenAIClient client;
    private final CassandraVectorStore vectorStore;

    public DocumentIndexPlanner(AzureOpenAIClient client) throws IOException {
        this.client = client;
        this.vectorStore = new CassandraVectorStore();
    }
    public void buildFromFolder(String folderPath) throws IOException {
        if (folderPath == null) {
            throw new IllegalArgumentException("folderPath shouldn't be empty.");
        }
        final int[] dimensions = {0};
        SimpleFolderReader reader = new SimpleFolderReader(folderPath);
        TextSplitter splitter = new TextSplitter();

        reader.run((fileName, content) -> {

            log.info("String to process {}...", fileName);
            var textChunks = splitter.split(content);
            List<CassandraEntity> entities = new ArrayList<>();
            for (var chunk: textChunks) {
                var response = client.getEmbeddings(List.of(chunk));
                var embedding = response.getData().get(0).getEmbedding();
                Vector<Float> vector = new Vector<>();
                for (var value: embedding) {
                    vector.add(value.floatValue());
                }
                if (dimensions[0] == 0) {
                    dimensions[0] = embedding.size();
                } else if (dimensions[0] != embedding.size()) {
                    throw new IllegalStateException("Embedding size is not consistent.");
                }
                String key = UUID.randomUUID().toString();
                entities.add(CassandraEntity.builder()
                        .id(key)
                        .hash("")
                        .embedding(vector)
                        .text(chunk)
                        .build());
            }
            try {
                vectorStore.saveDocuments(entities);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        try {
            vectorStore.createVectorIndex(100, dimensions[0], "COS");
        }
        catch (Exception e) {
            log.info("Index already exists");
        }

        log.info("All documents are loaded to Cassandra 5.0 vector store.");
    }
}
