package com.textbasedgame.configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.config.MorphiaConfig;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
class ApplicationConfiguration {
    Dotenv dotenv = Dotenv.load();
    private final String DB_NAME = Objects.requireNonNull(dotenv.get("MONGODB_NAME"));

    Datastore datastore;


    @Bean
    @Scope("singleton")
    public Datastore datastore(MongoClient mongoClient) {

        MorphiaConfig config = MorphiaConfig.load().database(DB_NAME);
        this.datastore = Morphia.createDatastore(mongoClient, config);
        return this.datastore;
    }

    @Bean
    public MongoClient mongoClient() {
        String DB_URI = Objects.requireNonNull(dotenv.get("MONGODB_URI"));
        return MongoClients.create(DB_URI);
    }

    @Bean
    MongoOperations mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, DB_NAME);
    }

    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(5);
        exec.setMaxPoolSize(10);
        exec.setQueueCapacity(25);
        exec.initialize();
        return exec;
    }

}