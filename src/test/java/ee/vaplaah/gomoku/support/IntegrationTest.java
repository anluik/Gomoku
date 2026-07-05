package ee.vaplaah.gomoku.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests (named {@code *IntTest}, run by the Failsafe plugin).
 *
 * <p>Boots the full Spring context against a throwaway MongoDB provided by Testcontainers.
 * A single container is started once (singleton-container pattern) and reused across every
 * {@code *IntTest} in the suite; Testcontainers' reaper stops it when the JVM exits. The random
 * container URI is injected via {@code spring.data.mongodb.uri}, which takes precedence over
 * any host/port in {@code application.yml}, so tests never touch a developer's local Mongo.
 */
@SpringBootTest
public abstract class IntegrationTest {

    static final MongoDBContainer MONGO_DB =
        new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    static {
        // Testcontainers' shaded docker-java falls back to Docker API version 1.32 when none is
        // configured, and Docker Engine 29+ rejects that with HTTP 400 ("Could not find a valid
        // Docker environment"). Pin a version modern engines accept (1.44 = Engine 25+, Jan 2024).
        // Remove once Testcontainers raises its default (> 1.21.3).
        if (System.getProperty("api.version") == null) {
            System.setProperty("api.version", "1.44");
        }
        MONGO_DB.start();
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB::getReplicaSetUrl);
    }
}
