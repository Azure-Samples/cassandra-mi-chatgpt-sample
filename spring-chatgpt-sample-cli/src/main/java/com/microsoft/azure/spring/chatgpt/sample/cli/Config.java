package com.microsoft.azure.spring.chatgpt.sample.cli;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.azure.spring.chatgpt.sample.common.AzureOpenAIClient;
import com.microsoft.azure.spring.chatgpt.sample.common.DocumentIndexPlanner;
import com.microsoft.azure.spring.chatgpt.sample.common.vectorstore.CassandraVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    public Config() throws IOException {
    }

    @Bean
    public DocumentIndexPlanner planner(AzureOpenAIClient openAIClient, CassandraVectorStore vectorStore) throws IOException {
        return new DocumentIndexPlanner(openAIClient);
    }

    @Bean
    public CassandraVectorStore vectorStore() throws IOException {
        CassandraVectorStore store = new CassandraVectorStore();
        return store;
    }

    @Bean
    public AzureOpenAIClient AzureOpenAIClient() {
        var innerClient = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
        return new AzureOpenAIClient(innerClient, embeddingDeploymentId, null);
    }

}
