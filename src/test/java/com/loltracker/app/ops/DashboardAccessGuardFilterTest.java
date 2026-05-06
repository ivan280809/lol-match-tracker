package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class DashboardAccessGuardFilterTest {

  @Test
  void disabledGuardAllowsDashboardRequests() throws ServletException, IOException {
    DashboardAccessGuardFilter filter = filter(false, "admin", "secret");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
  }

  @Test
  void enabledGuardRequiresBasicCredentialsForDashboard() throws ServletException, IOException {
    DashboardAccessGuardFilter filter = filter(true, "admin", "secret");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(401, response.getStatus());
    assertTrue(response.getHeader("WWW-Authenticate").contains("LOL Match Tracker"));
  }

  @Test
  void enabledGuardAllowsConfiguredBasicCredentials() throws ServletException, IOException {
    DashboardAccessGuardFilter filter = filter(true, "admin", "secret");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
    request.addHeader("Authorization", basic("admin", "secret"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(200, response.getStatus());
  }

  @Test
  void healthEndpointBypassesGuard() throws ServletException, IOException {
    DashboardAccessGuardFilter filter = filter(true, "admin", "secret");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(200, response.getStatus());
  }

  private DashboardAccessGuardFilter filter(boolean enabled, String username, String password) {
    DashboardAccessGuardFilter filter = new DashboardAccessGuardFilter();
    ReflectionTestUtils.setField(filter, "enabled", enabled);
    ReflectionTestUtils.setField(filter, "username", username);
    ReflectionTestUtils.setField(filter, "password", password);
    return filter;
  }

  private String basic(String username, String password) {
    String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    return "Basic " + token;
  }
}
