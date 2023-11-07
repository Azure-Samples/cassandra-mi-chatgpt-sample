package com.microsoft.azure.spring.chatgpt.sample.webapi;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.azure.spring.chatgpt.sample.common.AzureOpenAIClient;
import com.microsoft.azure.spring.chatgpt.sample.common.ChatPlanner;
import com.microsoft.azure.spring.chatgpt.sample.common.vectorstore.CassandraVectorStore;
import com.microsoft.azure.spring.chatgpt.sample.common.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.data.cassandra.core.CassandraTemplate;
//import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;

@Configuration
public class Config {

    @Value("${AZURE_OPENAI_EMBEDDINGDEPLOYMENTID}")
    private String embeddingDeploymentId;

    @Value("${AZURE_OPENAI_CHATDEPLOYMENTID}")
    private String chatDeploymentId;

    @Value("${AZURE_OPENAI_ENDPOINT}")
    private String endpoint;

    @Value("${AZURE_OPENAI_APIKEY}")
    private String apiKey;

    @Value("${vector-store.file}")
    private String vectorJsonFile;

    public Config() throws IOException {
    }

    @Bean
    public ChatPlanner planner(AzureOpenAIClient openAIClient, VectorStore vectorStore) {
        return new ChatPlanner(openAIClient, vectorStore);
    }

    @Bean
    public AzureOpenAIClient AzureOpenAIClient() {
        var innerClient = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(apiKey))
            .buildClient();
        return new AzureOpenAIClient(innerClient, embeddingDeploymentId, chatDeploymentId);
    }

/*    @Bean
    public CassandraUtils cassandraTemplate() throws IOException {
        return new CassandraUtils();
    }*/

    @Bean
    public VectorStore vectorStore() throws IOException {
        CassandraVectorStore store = new CassandraVectorStore();
        String currentPath = new java.io.File(".").getCanonicalPath();;
        String path = currentPath+vectorJsonFile.replace(  "\\", "//");
        store.loadFromJsonFile(path);
        return store;
    }
}
