/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class TestingUtil {

  private static final long stateEventQueueTimeoutMills = 5000;
  private static final long machineEndTimeoutMills = 10000;
  private static final AtomicInteger streamCount = new AtomicInteger();

  private TestingUtil() {
    //Not for instanciation outside this class
  }

  public static OutputStream initLogCaptureStream() {
    OutputStream logStream = new ByteArrayOutputStream();
    addOutputStreamLogAppender(logStream,
        String.format("LogStream-%s", streamCount.incrementAndGet()));
    return logStream;
  }

  public static OutputStream initStdOutStream() {
    OutputStream stdOutStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stdOutStream));
    return stdOutStream;
  }

  public static void sleep(long millis) {
    try {
      TimeUnit.MILLISECONDS.sleep(millis);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void addOutputStreamLogAppender(OutputStream logStream, String streamName) {
    LoggerContext context = LoggerContext.getContext(false);
    Configuration configuration = context.getConfiguration();
    PatternLayout patternLayout = PatternLayout.createDefaultLayout();
    Level level = null;
    Filter filter = null;
    Appender appender = OutputStreamAppender.createAppender(patternLayout, filter, logStream,
        streamName, false, true);
    appender.start();
    configuration.addAppender(appender);
    for (LoggerConfig loggerConfig : configuration.getLoggers().values()) {
      loggerConfig.addAppender(appender, level, filter);
    }
    configuration.getRootLogger().addAppender(appender, level, filter);
  }
}