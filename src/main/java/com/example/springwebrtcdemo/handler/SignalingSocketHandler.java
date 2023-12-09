package com.example.springwebrtcdemo.handler;

import com.example.springwebrtcdemo.constant.CommonConstant;
import com.example.springwebrtcdemo.model.SignalMessage;
import com.example.springwebrtcdemo.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class SignalingSocketHandler extends TextWebSocketHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SignalingSocketHandler.class);

  private final Map<String, WebSocketSession> connectedUsers = new HashMap<>();

  private final Map<String, String> conversation = new HashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    log.info("[" + session.getId() + "] Connection established " + session.getId());
    String conversationId = this.getConversationIdFromSession(session);
    log.info(conversationId);

    SignalMessage newUser = new SignalMessage();
    newUser.setType(CommonConstant.TYPE_INIT);
    newUser.setSender(session.getId());

    connectedUsers
        .values()
        .forEach(
            webSocketSession -> {
              String targetConversationId = conversation.get(webSocketSession.getId());
              if (conversationId.equals(targetConversationId)) {
                try {
                  webSocketSession.sendMessage(new TextMessage(Utils.getString(newUser)));
                } catch (Exception e) {
                  log.warn("Error after connection established", e);
                }
              }
            });

    connectedUsers.put(session.getId(), session);
    conversation.put(session.getId(), conversationId);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    log.info(
        "["
            + session.getId()
            + "] Connection closed "
            + session.getId()
            + " with status: "
            + status.getReason());
    removeUserAndSendLogout(session.getId());
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.info(
        "["
            + session.getId()
            + "] Connection error "
            + session.getId()
            + " with status: "
            + exception.getLocalizedMessage());
    removeUserAndSendLogout(session.getId());
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    try {
      log.info("handleTextMessage : {}", message.getPayload());

      SignalMessage signalMessage = Utils.getObject(message.getPayload());
      String destinationUser = signalMessage.getReceiver();
      WebSocketSession destinationSocket = connectedUsers.get(destinationUser);
      if (destinationSocket != null && destinationSocket.isOpen()) {
        signalMessage.setSender(session.getId());
        String resendingMessage = Utils.getString(signalMessage);
        log.info("send message {} to {}", resendingMessage, destinationUser);
        synchronized (destinationSocket) {
          destinationSocket.sendMessage(new TextMessage(resendingMessage));
        }
      }
    } catch (Exception e) {
      log.error("Error handle text message " + e.getMessage());
    }
  }

  private void removeUserAndSendLogout(final String sessionId) {

    connectedUsers.remove(sessionId);
    SignalMessage userOut = new SignalMessage();
    userOut.setType(CommonConstant.TYPE_LOGOUT);
    userOut.setSender(sessionId);

    connectedUsers
        .values()
        .forEach(
            webSocket -> {
              try {
                webSocket.sendMessage(new TextMessage(Utils.getString(userOut)));
              } catch (Exception e) {
                LOG.warn("Error remove user", e);
              }
            });
  }

  private String getConversationIdFromSession(WebSocketSession session) {
    return Objects.requireNonNull(session.getUri()).getPath().replaceFirst("/call/", "");
  }
}
