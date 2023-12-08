package com.example.springwebrtcdemo.config;

import com.example.springwebrtcdemo.handler.SignalingSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
    webSocketHandlerRegistry
        .addHandler(signalingSocketHandler(), "/call/{conversationId}")
        .setAllowedOrigins("*");
  }

  @Bean
  public WebSocketHandler signalingSocketHandler() {
    return new SignalingSocketHandler();
  }
}
