package com.loltracker.app.ops;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = DashboardController.class)
@Slf4j
public class DashboardExceptionHandler {

  @ExceptionHandler(Exception.class)
  public String handleUnexpectedException(
      Exception exception, HttpServletRequest request, RedirectAttributes redirectAttributes) {
    log.error("Unexpected dashboard error at {}", request.getRequestURI(), exception);
    redirectAttributes.addFlashAttribute("errorMessage", "Un error ha ocurrido");
    return "redirect:/";
  }
}
