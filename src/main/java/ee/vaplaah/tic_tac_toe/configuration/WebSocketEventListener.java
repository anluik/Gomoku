package ee.vaplaah.tic_tac_toe.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Spring WebSocket publishes events when messages are received from the client.
 * If you are using STOMP, these are the events published:
 * - SessionConnectedEvent
 * - SessionConnectEvent
 * - SessionDisconnectEvent
 * - SessionSubscribeEvent
 * - SessionUnsubscribeEvent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    /**
     * Event raised when the session of a WebSocket client using a STOMP as the WebSocket sub-protocol is closed.
     * Note that this event may be raised more than once for a single session and therefore event consumers should
     * be idempotent and ignore a duplicate event.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {

    }
}
