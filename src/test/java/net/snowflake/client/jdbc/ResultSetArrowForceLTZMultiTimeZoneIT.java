/*
 * Copyright (c) 2012-2019 Snowflake Computing Inc. All right reserved.
 */
package net.snowflake.client.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.TimeZone;
import net.snowflake.client.category.TestCategoryArrow;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Compare json and arrow resultSet behaviors 1/2 */
@RunWith(Parameterized.class)
@Category(TestCategoryArrow.class)
public class ResultSetArrowForceLTZMultiTimeZoneIT extends ResultSetArrowForce0MultiTimeZone {
  @Parameterized.Parameters(name = "format={0}, tz={1}")
  public static Collection<Object[]> data() {
    return ResultSetArrowForce0MultiTimeZone.testData();
  }

  public ResultSetArrowForceLTZMultiTimeZoneIT(String queryResultFormat, String timeZone) {
    super(queryResultFormat, timeZone);
  }

  @Test
  public void testTimestampLTZ() throws SQLException {
    for (int scale = 0; scale <= 9; scale++) {
      testTimestampLTZWithScale(scale);
    }
  }

  private void testTimestampLTZWithScale(int scale) throws SQLException {
    String[] cases = {
      "2017-01-01 12:00:00 Z",
      "2014-01-02 16:00:00 Z",
      "2014-01-02 12:34:56 Z",
      "1970-01-01 00:00:00 Z",
      "1970-01-01 00:00:01 Z",
      "1969-12-31 11:59:59 Z",
      "0000-01-01 00:00:01 Z",
      "0001-12-31 11:59:59 Z"
    };

    long[] times = {
      1483272000000L,
      1388678400000L,
      1388666096000L,
      0,
      1000,
      -43201000,
      -62167391999000L,
      -62104276801000L
    };

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getDefault());

    String table = "test_arrow_ts_ltz";

    String column = "(a timestamp_ltz(" + scale + "))";

    String values = "('" + StringUtils.join(cases, "'),('") + "'), (null)";
    Connection con = init(table, column, values);
    ResultSet rs = con.createStatement().executeQuery("select * from " + table);
    int i = 0;
    while (i < cases.length) {
      assertTrue(rs.next());
      assertEquals(times[i++], rs.getTimestamp(1).getTime());
      assertEquals(0, rs.getTimestamp(1).getNanos());
    }
    assertTrue(rs.next());
    assertNull(rs.getString(1));
    finish(table, con);
  }

  @Test
  public void testTimestampLTZOutputFormat() throws SQLException {
    String[] cases = {"2017-01-01 12:00:00 Z", "2014-01-02 16:00:00 Z", "2014-01-02 12:34:56 Z"};

    long[] times = {1483272000000L, 1388678400000L, 1388666096000L};

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getDefault());

    String table = "test_arrow_ts_ltz";

    String column = "(a timestamp_ltz)";

    String values = "('" + StringUtils.join(cases, "'),('") + "')";
    try (Connection con = init(table, column, values);
        Statement statement = con.createStatement()) {
      try {
        // use initialized ltz output format
        try (ResultSet rs = statement.executeQuery("select * from " + table)) {
          for (int i = 0; i < cases.length; i++) {
            assertTrue(rs.next());
            assertEquals(times[i], rs.getTimestamp(1).getTime());
            String weekday = rs.getString(1).split(",")[0];
            assertEquals(3, weekday.length());
          }
        }
        // change ltz output format
        statement.execute(
            "alter session set TIMESTAMP_LTZ_OUTPUT_FORMAT='YYYY-MM-DD HH24:MI:SS TZH:TZM'");
        try (ResultSet rs = statement.executeQuery("select * from " + table)) {
          for (int i = 0; i < cases.length; i++) {
            assertTrue(rs.next());
            assertEquals(times[i], rs.getTimestamp(1).getTime());
            String year = rs.getString(1).split("-")[0];
            assertEquals(4, year.length());
          }
        }

        // unset ltz output format, then it should use timestamp_output_format
        statement.execute("alter session unset TIMESTAMP_LTZ_OUTPUT_FORMAT");
        try (ResultSet rs = statement.executeQuery("select * from " + table)) {
          for (int i = 0; i < cases.length; i++) {
            assertTrue(rs.next());
            assertEquals(times[i], rs.getTimestamp(1).getTime());
            String weekday = rs.getString(1).split(",")[0];
            assertEquals(3, weekday.length());
          }
        }
        // set ltz output format back to init value
        statement.execute(
            "alter session set TIMESTAMP_LTZ_OUTPUT_FORMAT='DY, DD MON YYYY HH24:MI:SS TZHTZM'");
        try (ResultSet rs = statement.executeQuery("select * from " + table)) {
          for (int i = 0; i < cases.length; i++) {
            assertTrue(rs.next());
            assertEquals(times[i], rs.getTimestamp(1).getTime());
            String weekday = rs.getString(1).split(",")[0];
            assertEquals(3, weekday.length());
          }
        }
      } finally {
        statement.execute("drop table " + table);
        System.clearProperty("user.timezone");
      }
    }
  }

  @Test
  public void testTimestampLTZWithNulls() throws SQLException {
    String[] cases = {
      "2017-01-01 12:00:00 Z",
      "2014-01-02 16:00:00 Z",
      "2014-01-02 12:34:56 Z",
      "1970-01-01 00:00:00 Z",
      "1970-01-01 00:00:01 Z",
      "1969-12-31 11:59:59 Z",
      "0000-01-01 00:00:01 Z",
      "0001-12-31 11:59:59 Z"
    };

    long[] times = {
      1483272000000L,
      1388678400000L,
      1388666096000L,
      0,
      1000,
      -43201000,
      -62167391999000L,
      -62104276801000L
    };

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getDefault());

    String table = "test_arrow_ts_ltz";

    String column = "(a timestamp_ltz)";

    String values = "('" + StringUtils.join(cases, "'), (null),('") + "')";
    try (Connection con = init(table, column, values);
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery("select * from " + table)) {
      try {
        int i = 0;
        while (i < 2 * cases.length - 1) {
          assertTrue(rs.next());
          if (i % 2 != 0) {
            assertNull(rs.getTimestamp(1));
          } else {
            assertEquals(times[i / 2], rs.getTimestamp(1).getTime());
            assertEquals(0, rs.getTimestamp(1).getNanos());
          }
          i++;
        }
      } finally {
        statement.execute("drop table " + table);
        System.clearProperty("user.timezone");
      }
    }
  }

  @Test
  public void testTimestampLTZWithNanos() throws SQLException {
    String[] cases = {
      "2017-01-01 12:00:00.123456789",
      "2014-01-02 16:00:00.000000001",
      "2014-01-02 12:34:56.1",
      "1969-12-31 23:59:59.000000001",
      "1970-01-01 00:00:00.123412423",
      "1970-01-01 00:00:01.000001",
      "1969-12-31 11:59:59.001",
      "0001-12-31 11:59:59.11"
    };

    long[] times = {
      1483272000123L, 1388678400000L, 1388666096100L, -1000, 123, 1000, -43200999, -62104276800890L
    };

    int[] nanos = {123456789, 1, 100000000, 1, 123412423, 1000, 1000000, 110000000};

    String table = "test_arrow_ts_ltz";

    String column = "(a timestamp_ltz)";

    String values = "('" + StringUtils.join(cases, " Z'),('") + " Z'), (null)";
    try (Connection con = init(table, column, values);
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery("select * from " + table)) {
      try {
        int i = 0;
        while (i < cases.length) {
          assertTrue(rs.next());
          assertEquals(times[i], rs.getTimestamp(1).getTime());
          assertEquals(nanos[i++], rs.getTimestamp(1).getNanos());
        }
        assertTrue(rs.next());
        assertNull(rs.getString(1));
      } finally {
        statement.execute("drop table " + table);
        System.clearProperty("user.timezone");
      }
    }
  }
}
