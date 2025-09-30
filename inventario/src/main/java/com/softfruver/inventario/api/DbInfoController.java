package com.softfruver.inventario.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DbInfoController {

  private final JdbcTemplate jdbc;

  public DbInfoController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/db/ping")
  public String ping() {
    Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
    return "DB OK => " + one;
  }

  @GetMapping("/db/tables")
  public List<Map<String, Object>> listTables() {
    String sql = """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
        ORDER BY table_name
        """;
    return jdbc.queryForList(sql);
  }
}

