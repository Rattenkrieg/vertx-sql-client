package io.vertx.sqlclient;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.sqlclient.SqlConnectOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.sqlclient.SqlConnectOptions} original class using Vert.x codegen.
 */
public class SqlConnectOptionsConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, SqlConnectOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "cachePreparedStatements":
          if (member.getValue() instanceof Boolean) {
            obj.setCachePreparedStatements((Boolean)member.getValue());
          }
          break;
        case "database":
          if (member.getValue() instanceof String) {
            obj.setDatabase((String)member.getValue());
          }
          break;
        case "host":
          if (member.getValue() instanceof String) {
            obj.setHost((String)member.getValue());
          }
          break;
        case "hosts":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<io.vertx.sqlclient.SqlHost> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(new io.vertx.sqlclient.SqlHost((io.vertx.core.json.JsonObject)item));
            });
            obj.setHosts(list);
          }
          break;
        case "password":
          if (member.getValue() instanceof String) {
            obj.setPassword((String)member.getValue());
          }
          break;
        case "port":
          if (member.getValue() instanceof Number) {
            obj.setPort(((Number)member.getValue()).intValue());
          }
          break;
        case "preparedStatementCacheMaxSize":
          if (member.getValue() instanceof Number) {
            obj.setPreparedStatementCacheMaxSize(((Number)member.getValue()).intValue());
          }
          break;
        case "preparedStatementCacheSqlLimit":
          if (member.getValue() instanceof Number) {
            obj.setPreparedStatementCacheSqlLimit(((Number)member.getValue()).intValue());
          }
          break;
        case "properties":
          if (member.getValue() instanceof JsonObject) {
            java.util.Map<String, java.lang.String> map = new java.util.LinkedHashMap<>();
            ((Iterable<java.util.Map.Entry<String, Object>>)member.getValue()).forEach(entry -> {
              if (entry.getValue() instanceof String)
                map.put(entry.getKey(), (String)entry.getValue());
            });
            obj.setProperties(map);
          }
          break;
        case "tracingPolicy":
          if (member.getValue() instanceof String) {
            obj.setTracingPolicy(io.vertx.core.tracing.TracingPolicy.valueOf((String)member.getValue()));
          }
          break;
        case "user":
          if (member.getValue() instanceof String) {
            obj.setUser((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(SqlConnectOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(SqlConnectOptions obj, java.util.Map<String, Object> json) {
    json.put("cachePreparedStatements", obj.getCachePreparedStatements());
    if (obj.getDatabase() != null) {
      json.put("database", obj.getDatabase());
    }
    if (obj.getHosts() != null) {
      JsonArray array = new JsonArray();
      obj.getHosts().forEach(item -> array.add(item.toJson()));
      json.put("hosts", array);
    }
    if (obj.getPassword() != null) {
      json.put("password", obj.getPassword());
    }
    json.put("preparedStatementCacheMaxSize", obj.getPreparedStatementCacheMaxSize());
    if (obj.getProperties() != null) {
      JsonObject map = new JsonObject();
      obj.getProperties().forEach((key, value) -> map.put(key, value));
      json.put("properties", map);
    }
    if (obj.getTracingPolicy() != null) {
      json.put("tracingPolicy", obj.getTracingPolicy().name());
    }
    if (obj.getUser() != null) {
      json.put("user", obj.getUser());
    }
  }
}
