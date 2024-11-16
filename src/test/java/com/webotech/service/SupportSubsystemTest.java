/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.webotech.TestingUtil;
import com.webotech.service.data.SupportData;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportSubsystemTest {

  private final static TestAppContext testAppContext = new TestAppContext("test", new String[0]);
  private SupportSubsystem<TestAppContext> supportSubsystem;

  @BeforeEach
  void setup() {
    supportSubsystem = new SupportSubsystem<>();
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
}