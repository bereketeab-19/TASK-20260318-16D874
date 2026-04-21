package com.petsupplies.messaging.config;

import com.petsupplies.messaging.security.TopicScopeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final TopicScopeInterceptor topicScopeInterceptor;
  private final String[] allowedOriginPatterns;

  public WebSocketConfig(
      TopicScopeInterceptor topicScopeInterceptor,
      @Value("${app.websocket.allowed-origin-patterns}") String allowedOriginPatternsCsv
  ) {
    this.topicScopeInterceptor = topicScopeInterceptor;
    this.allowedOriginPatterns = java.util.Arrays.stream(allowedOriginPatternsCsv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new);
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOriginPatterns);
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app");
    registry.enableSimpleBroker("/topic");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(topicScopeInterceptor);
  }
}

