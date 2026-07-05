package ee.vaplaah.gomoku;

import ee.vaplaah.gomoku.support.IntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke integration test: verifies the full Spring context boots against a real MongoDB.
 * Also serves as proof that the Failsafe + Testcontainers pipeline is wired correctly.
 */
class GomokuApplicationIntTest extends IntegrationTest {

    @Test
    void contextLoads() {
    }
}
