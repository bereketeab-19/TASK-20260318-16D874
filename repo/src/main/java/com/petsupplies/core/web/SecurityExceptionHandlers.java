package com.petsupplies.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.core.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

public class SecurityExceptionHandlers {
  public static AuthenticationEntryPoint jsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
    return (request, response, authException) ->
        write(request, response, objectMapper, HttpStatus.UNAUTHORIZED, "Unauthorized");
  }

  public static AccessDeniedHandler jsonAccessDeniedHandler(ObjectMapper objectMapper) {
    return (request, response, accessDeniedException) ->
        write(request, response, objectMapper, HttpStatus.FORBIDDEN, "Forbidden");
  }

  private static void write(
      HttpServletRequest request,
      jakarta.servlet.http.HttpServletResponse response,
      ObjectMapper objectMapper,
      HttpStatus status,
      String message
  ) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ErrorResponse body = new ErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getRequestURI(),
        List.of(),
        null
    );
    objectMapper.writeValue(response.getOutputStream(), body);
  }
}

