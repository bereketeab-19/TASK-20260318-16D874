package com.petsupplies.messaging.security;

import com.petsupplies.user.security.SecurityUser;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Enforces that subscriptions to merchant-scoped topics match the authenticated principal merchantId.
 *
 * Allowed subscription pattern:
 * /topic/messages.{merchantId}
 * /topic/messages.{merchantId}.{sessionId}
 */
@Component
public class TopicScopeInterceptor implements ChannelInterceptor {
  private static final Pattern DEST_PATTERN = Pattern.compile("^/topic/messages\\.([^\\.]+)(?:\\.(\\d+))?$");

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
      String dest = accessor.getDestination();
      String merchantId = extractMerchantId(dest);
      SecurityUser user = securityUser(accessor.getUser());
      if (merchantId == null || user.getMerchantId() == null || !merchantId.equals(user.getMerchantId())) {
        throw new AccessDeniedException("Topic subscription merchant scope mismatch");
      }
    }
    return message;
  }

  private static String extractMerchantId(String destination) {
    if (destination == null) return null;
    Matcher m = DEST_PATTERN.matcher(destination);
    if (!m.matches()) return null;
    return m.group(1);
  }

  private static SecurityUser securityUser(Principal principal) {
    if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token
        && token.getPrincipal() instanceof SecurityUser su) {
      return su;
    }
    if (principal instanceof SecurityUser su) {
      return su;
    }
    throw new AccessDeniedException("Unauthenticated WebSocket user");
  }
}

