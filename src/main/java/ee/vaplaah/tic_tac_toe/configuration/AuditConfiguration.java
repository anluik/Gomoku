package ee.vaplaah.tic_tac_toe.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;

@Configuration
@EnableReactiveMongoAuditing
public class AuditConfiguration {}
