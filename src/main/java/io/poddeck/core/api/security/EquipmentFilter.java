package io.poddeck.core.api.security;

import io.poddeck.core.api.ApiConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class EquipmentFilter extends OncePerRequestFilter {
  private final ApiConfiguration apiConfiguration;

  @Override
  protected void doFilterInternal(
    HttpServletRequest request, HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    prepareResponseHeaders(request, response);
    if (request.getMethod().equals(RequestMethod.OPTIONS.name())) {
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private void prepareResponseHeaders(
    HttpServletRequest request, HttpServletResponse response
  ) {
    var origin = request.getHeader("Origin");
    if (origin != null && apiConfiguration.allowedOrigins().contains(origin)) {
      response.setHeader("Access-Control-Allow-Origin", origin);
    }
    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, " +
      "DELETE, OPTIONS");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setHeader("Access-Control-Allow-Headers", "content-type, " +
      "authorization, cluster");
    response.setHeader("Content-Type", "application/json");
  }
}
