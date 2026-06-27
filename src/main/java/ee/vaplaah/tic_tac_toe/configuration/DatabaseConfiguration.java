package ee.vaplaah.tic_tac_toe.configuration;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Reactive MongoDB infrastructure configuration: names the target database, enables auto-index
 * creation, and exposes the {@link com.mongodb.reactivestreams.client.MongoClient} bean used by
 * all repositories.
 */
@EnableReactiveMongoRepositories
public class DatabaseConfiguration extends AbstractReactiveMongoConfiguration {

    @NonNull
    @Override
    protected String getDatabaseName() {
        return "tictactoe_db";
    }

    @Override
    public boolean autoIndexCreation() {
        return true;
    }

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create();
    }
}