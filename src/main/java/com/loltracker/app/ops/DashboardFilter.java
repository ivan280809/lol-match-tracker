package com.loltracker.app.ops;

import com.loltracker.app.player.RiotPlatform;
import java.time.LocalDate;

public record DashboardFilter(
    String status,
    boolean withError,
    RiotPlatform platform,
    String syncStatus,
    String query,
    String champion,
    LocalDate fromDate,
    LocalDate toDate,
    String sort) {

  public DashboardFilter {
    status = normalizeStatus(status);
    syncStatus = hasText(syncStatus) ? syncStatus.trim() : "";
    query = hasText(query) ? query.trim() : "";
    champion = hasText(champion) ? champion.trim() : "";
    sort = normalizeSort(sort);
  }

  public boolean hasHistorySpecificFilter() {
    return hasText(champion) || fromDate != null || toDate != null;
  }

  public boolean hasActiveFilters() {
    return !"active".equals(status)
        || withError
        || platform != null
        || hasText(syncStatus)
        || hasText(query)
        || hasText(champion)
        || fromDate != null
        || toDate != null
        || !"name".equals(sort);
  }

  public String statusLabel() {
    return switch (status) {
      case "all" -> "Todos";
      case "inactive" -> "Inactivos";
      case "archived" -> "Archivados";
      case "active" -> "Activos";
      default -> "Activos";
    };
  }

  public String syncStatusLabel() {
    if (!hasText(syncStatus)) {
      return "Cualquiera";
    }
    return switch (syncStatus.toUpperCase()) {
      case "NEW" -> "Nueva";
      case "SUCCESS" -> "OK";
      case "ERROR" -> "Error";
      default -> syncStatus;
    };
  }

  public String sortLabel() {
    return switch (sort) {
      case "last-error" -> "Ultimo error";
      case "last-sync" -> "Ultima sync";
      case "rank" -> "Rank";
      case "activity" -> "Actividad";
      case "name" -> "Nombre";
      default -> "Nombre";
    };
  }

  public static DashboardFilter defaultFilter() {
    return new DashboardFilter("active", false, null, "", "", "", null, null, "name");
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String normalizeStatus(String value) {
    if (!hasText(value)) {
      return "active";
    }
    return switch (value.trim().toLowerCase()) {
      case "all", "inactive", "archived", "active" -> value.trim().toLowerCase();
      default -> "active";
    };
  }

  private static String normalizeSort(String value) {
    if (!hasText(value)) {
      return "name";
    }
    return switch (value.trim().toLowerCase()) {
      case "name", "last-error", "last-sync", "rank", "activity" -> value.trim().toLowerCase();
      default -> "name";
    };
  }
}
