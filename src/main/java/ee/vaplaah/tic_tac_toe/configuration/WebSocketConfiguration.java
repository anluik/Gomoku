package ee.vaplaah.tic_tac_toe.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.List;
import java.util.Map;

/**
 * The EnableWebSocketMessageBroker annotation sets up STOMP protocol and allows the server to
 * understand and process the STOMP messaging actions. It also configures a message broker which
 * is responsible for making sure that every message is forwarded to the correct place.
 * <br/><br/>
 * WebSocketMessageBrokerConfigurer enables to customize the settings that EnableWebSocketMessageBroker
 * configures by default.
 */
@Configuration
//@EnableWebSocketMessageBroker
public class WebSocketConfiguration { // implements WebSocketMessageBrokerConfigurer {

//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        // Enables an in-memory broker for destinations prefixed with /topic.
//        // This is suitable for basic applications and single-server deployments.
//        // Another option is enableStompBrokerRelay which would be required for scalable, multi-server
//        // applications, as it forwards messages to an external message broker.
//        registry.enableSimpleBroker("/topic");
//        // Designates the prefix for messages routed to @MessageMapping methods in controllers
//        // Often /app is used clearly signals that the message is intended for the application's
//        // custom processing logic rather than being immediately handed off to the message broker.
//        registry.setApplicationDestinationPrefixes("/app");
//        registry.setUserDestinationPrefix("/user");
//    }
//
//    /**
//     * Defines the HTTP URL endpoint clients will use to initiate the WebSocket handshake.
//     * For example, registering /tic_tac_toe means the client connects to ws://server/tic_tac_toe.
//     */
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/tic_tac_toe").withSockJS();
//    }
//
//    @Override
//    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
//        return false;
//    }

    /**
     * Allows to link a URL to a specific session.
     * @return
     */
    @Bean
    public SimpleUrlHandlerMapping handlerMapping(WebSocketHandler handler) {
        return new SimpleUrlHandlerMapping(Map.of("/tic_tac_toe", handler), 1);
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
