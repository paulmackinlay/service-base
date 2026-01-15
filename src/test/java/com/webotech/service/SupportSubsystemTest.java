/*
 * Copyright (c) 2024-2025 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.webotech.TestingUtil;
import com.webotech.service.data.SupportData;
import com.webotech.util.PropertyUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportSubsystemTest {

  private static final TestAppContext testAppContext = new TestAppContext("test", new String[0]);
  private SupportSubsystem<TestAppContext> supportSubsystem;

  @BeforeEach
  void setup() {
    supportSubsystem = new SupportSubsystem<>();
  }

  @Test
  void shouldGetAllSupportData() {
    SupportData supportData = SupportSubsystem.supportData;
    Map<String, String> data = new HashMap<>();
    data.putAll(supportData.getHost());
    data.putAll(supportData.getProcess());
    data.putAll(supportData.getJvm());
    assertEquals(data, supportData.getAll());
  }

  @Test
  void shouldLogSupportData() throws IOException {
    SupportData supportData = SupportSubsystem.supportData;
    assertEquals(
        Set.of(SupportData.AVAILABLE_PROCESSOR_COUNT, SupportData.HOSTNAME, SupportData.IP_ADDRESS,
            SupportData.OS_ARCHITECTURE, SupportData.OS_NAME, SupportData.OS_VERSION),
        supportData.getHost().keySet());
    assertEquals(
        Set.of(SupportData.JAVA_CLASSPATH, SupportData.JAVA_HOME, SupportData.JAVA_SPEC_VERSION,
            SupportData.JAVA_VERSION), supportData.getJvm().keySet());
    assertEquals(Set.of(SupportData.CL_ARGS, SupportData.MEMORY_B, SupportData.MEMORY_KI_B,
            SupportData.MEMORY_GI_B, SupportData.PID, SupportData.USER),
        supportData.getProcess().keySet());
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      supportSubsystem.start(testAppContext);
      String log = TestingUtil.asNormalisedTxt(logStream);
      List<String> keys = Arrays.asList(SupportData.AVAILABLE_PROCESSOR_COUNT, SupportData.HOSTNAME,
          SupportData.IP_ADDRESS, SupportData.OS_ARCHITECTURE, SupportData.OS_NAME,
          SupportData.OS_VERSION, SupportData.JAVA_CLASSPATH, SupportData.JAVA_HOME,
          SupportData.JAVA_SPEC_VERSION, SupportData.JAVA_VERSION, SupportData.CL_ARGS,
          SupportData.MEMORY_B, SupportData.MEMORY_KI_B, SupportData.MEMORY_GI_B, SupportData.PID,
          SupportData.USER);
      for (String key : keys) {
        assertTrue(log.contains(key));
      }
      assertTrue(log.contains("Host SupportData"));
      assertTrue(log.contains("JVM SupportData"));
      assertTrue(log.contains("Process SupportData"));
    }
  }

  @Test
  void shouldStartDetectingDeadlocks() throws IOException {
    PropertyUtil.setProperty(SupportSubsystem.PROP_KEY_ENABLE_SUPPORT_DATA_LOGGING, "false");
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      supportSubsystem.start(testAppContext);
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("Will schedule deadlock detection every 60000 millis\n", log);
    } finally {
      PropertyUtil.removeProperty(SupportSubsystem.PROP_KEY_ENABLE_SUPPORT_DATA_LOGGING);
    }
  }

  @Test
  void shouldStopDetectingDeadlocks() throws IOException {
    PropertyUtil.setProperty(SupportSubsystem.PROP_KEY_ENABLE_SUPPORT_DATA_LOGGING, "false");
    supportSubsystem.start(testAppContext);
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      supportSubsystem.stop(testAppContext);
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("Shutting down deadlock detection with timeout PT5S\n", log);
    } finally {
      PropertyUtil.removeProperty(SupportSubsystem.PROP_KEY_ENABLE_SUPPORT_DATA_LOGGING);
    }
  }

  @Test
  void shouldWarnAboutMultipleNonDaemonThreads() throws IOException {
    CountDownLatch latch = new CountDownLatch(1);
    Thread nonDaemonThread = new Thread(() -> {
      try {
        latch.await(3, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }, "i_am_bad");
    nonDaemonThread.setDaemon(false);
    nonDaemonThread.start();
    supportSubsystem.start(testAppContext);
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      supportSubsystem.stop(testAppContext);
      latch.countDown();
      String[] logs = TestingUtil.asNormalisedTxt(logStream).split("\n");
      assertEquals("Shutting down deadlock detection with timeout PT5S", logs[0]);
      assertEquals(
          "The following are all non-daemon threads [i_am_bad, main]. All of them need to be stopped for the application to exit, "
              + "typically the main thread of an application is the only non-daemon thread.",
          logs[1]);
    } finally {
      PropertyUtil.removeProperty(SupportSubsystem.PROP_KEY_ENABLE_SUPPORT_DATA_LOGGING);
    }
  }
}