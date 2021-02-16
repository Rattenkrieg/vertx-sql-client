package io.vertx.mysqlclient;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Repeat;
import io.vertx.ext.unit.junit.RepeatRule;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@RunWith(VertxUnitRunner.class)
public class MySQLPoolTest extends MySQLTestBase {

  Vertx vertx;
  MySQLConnectOptions options;
  MySQLPool pool;

  @Rule
  public RepeatRule rule = new RepeatRule();

  @Before
  public void setup() {
    vertx = Vertx.vertx();
    options = new MySQLConnectOptions(MySQLTestBase.options);
    pool = MySQLPool.pool(vertx, options, new PoolOptions());
  }

  @After
  public void tearDown(TestContext ctx) {
    if (pool != null) {
      pool.close();
    }
    vertx.close(ctx.asyncAssertSuccess());
  }

  @Test
  public void testContinuouslyConnecting(TestContext ctx) {
    Async async = ctx.async(3);
    pool.getConnection(ctx.asyncAssertSuccess(conn1 -> async.countDown()));
    pool.getConnection(ctx.asyncAssertSuccess(conn2 -> async.countDown()));
    pool.getConnection(ctx.asyncAssertSuccess(conn3 -> async.countDown()));
    async.await();
  }

  @Test
  public void testContinuouslyQuery(TestContext ctx) {
    Async async = ctx.async(3);
    pool.preparedQuery("SELECT 1").execute(ctx.asyncAssertSuccess(res1 -> {
      ctx.assertEquals(1, res1.size());
      async.countDown();
    }));
    pool.preparedQuery("SELECT 2").execute(ctx.asyncAssertSuccess(res1 -> {
      ctx.assertEquals(1, res1.size());
      async.countDown();
    }));
    pool.preparedQuery("SELECT 3").execute(ctx.asyncAssertSuccess(res1 -> {
      ctx.assertEquals(1, res1.size());
      async.countDown();
    }));
    async.await();
  }

  // This test check that when using pooled connections, the preparedQuery pool operation
  // will actually use the same connection for the prepare and the query commands
  @Test
  public void testConcurrentMultipleConnection(TestContext ctx) {
    PoolOptions poolOptions = new PoolOptions().setMaxSize(2);
    MySQLPool pool = MySQLPool.pool(vertx, new MySQLConnectOptions(this.options).setCachePreparedStatements(false), poolOptions);
    try {
      int numRequests = 1500;
      Async async = ctx.async(numRequests);
      for (int i = 0;i < numRequests;i++) {
        pool.preparedQuery("SELECT * FROM Fortune WHERE id=?").execute(Tuple.of(1), ctx.asyncAssertSuccess(results -> {
          ctx.assertEquals(1, results.size());
          Tuple row = results.iterator().next();
          ctx.assertEquals(1, row.getInteger(0));
          ctx.assertEquals("fortune: No such file or directory", row.getString(1));
          async.countDown();
        }));
      }
      async.awaitSuccess(10_000);
    } finally {
      pool.close();
    }
  }

  @Test
  public void checkBorderConditionBetweenIdleAndGetConnection(TestContext ctx) {
    Async killConnections = ctx.async();
    MySQLConnection.connect(vertx, options, ctx.asyncAssertSuccess(conn -> {
      conn.query("SELECT CONNECTION_ID()").execute(ctx.asyncAssertSuccess(r -> {
        Integer currentConnectionId = r.iterator().next().getInteger(0);
        String query = "SELECT ID FROM INFORMATION_SCHEMA.PROCESSLIST WHERE ID <> ? AND User = ? AND db = ?";
        Collector<Row, ?, List<Integer>> collector = mapping(row -> row.getInteger(0), toList());
        conn.preparedQuery(query).collecting(collector).execute(Tuple.of(currentConnectionId, options.getUser(), options.getDatabase()), ctx.asyncAssertSuccess(ids -> {
          CompositeFuture killAll = ids.value().stream()
            .<Future>map(connId -> {
              Promise<RowSet<Row>> promise = Promise.promise();
              conn.query("KILL " + connId).execute(promise);
              return promise.future();
            })
            .collect(Collectors.collectingAndThen(toList(), CompositeFuture::all));
          killAll.onSuccess(cf -> conn.close()).onComplete(ctx.asyncAssertSuccess(v -> killConnections.countDown()));
        }));
      }));
    }));
    killConnections.awaitSuccess();

    int concurrentRequestAmount = 100;
    int idle = 1000;
    int poolSize = 5;

    options.setIdleTimeout(idle).setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
    PoolOptions poolOptions = new PoolOptions();
    poolOptions.setMaxSize(poolSize).setIdleTimeout(idle).setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
    pool = MySQLPool.pool(options, poolOptions);

    Async async = ctx.async(concurrentRequestAmount);
    for (int i = 0; i < concurrentRequestAmount; i++) {
      CompletableFuture.runAsync(() -> {
        pool.query("SELECT CURRENT_TIMESTAMP;").execute(ctx.asyncAssertSuccess(rowSet -> {
          String query = "SELECT COUNT(*) as cnt FROM INFORMATION_SCHEMA.PROCESSLIST WHERE User = ? AND db = ?";
          pool.preparedQuery(query).execute(Tuple.of(options.getUser(), options.getDatabase()), ctx.asyncAssertSuccess(rows -> {
            Integer count = rows.iterator().next().getInteger("cnt");
            ctx.assertInRange(count, 1, poolSize, "Oops!...Connections exceed poolSize. Are you leaked connections?.");
            async.countDown();
          }));
        }));
      });
    }
  }
}
