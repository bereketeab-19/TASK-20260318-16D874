package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petsupplies.messaging.security.TopicScopeInterceptor;
import com.petsupplies.user.security.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

class TopicScopeInterceptorTest {
  @Test
  void merchantA_cannot_subscribe_to_merchantB_topic() {
    var interceptor = new TopicScopeInterceptor();
    var user = new SecurityUser(
        1L,
        "merchantA",
        "x",
        "mrc_A",
        List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))
    );

    var auth = new UsernamePasswordAuthenticationToken(user, "x", user.getAuthorities());

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/messages.mrc_B.123");
    accessor.setUser(auth);
    accessor.setLeaveMutable(true);

    Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertThatThrownBy(() -> interceptor.preSend(msg, new org.springframework.messaging.support.ExecutorSubscribableChannel()))
        .isInstanceOf(AccessDeniedException.class);
  }
}

