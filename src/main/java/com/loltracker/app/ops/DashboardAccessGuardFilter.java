package com.loltracker.app.ops;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DashboardAccessGuardFilter extends OncePerRequestFilter {

  private static final String REALM = "LOL Match Tracker";

  @Value("${app.dashboard.guard.enabled:false}")
  private boolean enabled;

  @Value("${app.dashboard.guard.username:admin}")
  private String username;

  @Value("${app.dashboard.guard.password:}")
  private String password;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!enabled) {
      return true;
    }
    String path = request.getRequestURI();
    return path == null
        || path.startsWith("/actuator/health")
        || path.equals("/error")
        || path.startsWith("/css/")
        || path.startsWith("/js/")
        || path.startsWith("/images/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!hasCredentialsConfigured()) {
      response.sendError(
          HttpServletResponse.SC_SERVICE_UNAVAILABLE,
          "Dashboard guard is enabled but credentials are not configured");
      return;
    }

    if (!authorized(request.getHeader("Authorization"))) {
      response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean hasCredentialsConfigured() {
    return hasText(username) && hasText(password);
  }

  private boolean authorized(String authorization) {
    if (authorization == null || !authorization.startsWith("Basic ")) {
      return false;
    }
    String encoded = authorization.substring("Basic ".length()).trim();
    try {
      String decoded =
          new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
      int separator = decoded.indexOf(':');
      if (separator < 0) {
        return false;
      }
      String providedUsername = decoded.substring(0, separator);
      String providedPassword = decoded.substring(separator + 1);
      return username.equals(providedUsername) && password.equals(providedPassword);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
