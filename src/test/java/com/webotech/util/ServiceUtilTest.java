/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.webotech.TestingUtil;
import com.webotech.service.PropSubsystem;
import com.webotech.service.SupportSubsystem;
import com.webotech.service.TestAppContext;
import com.webotech.statemachine.service.api.AppService;
import com.webotech.statemachine.service.api.Subsystem;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceUtilTest {

  private AppService<TestAppContext> appService;

  @BeforeEach
  void setup() {
    appService = mock(AppService.class);
  }

  @Test
  void shouldStartServiceSafely() {
    ServiceUtil.startService(appService);
    verify(appService, times(1)).start();
  }

  @Test
  void shouldStopServiceOnException() {
    IllegalStateException exception = new IllegalStateException("test induced");
    doThrow(exception).when(appService).start();
    ServiceUtil.startService(appService);
    verify(appService, times(1)).stop();
  }

  @Test
  void shouldGetContextWithStandardSubsystems() throws IOException {
    TestAppContext appContext = new TestAppContext("test", new String[0]);
    try (OutputStream logSteam = TestingUtil.initLogCaptureStream()) {
      appContext = ServiceUtil.instrumentContext(appContext);
      List<Subsystem<TestAppContext>> subsystems = appContext.getSubsystems();
      assertEquals(2, subsystems.size());
      assertInstanceOf(PropSubsystem.class, subsystems.get(0));
      assertInstanceOf(SupportSubsystem.class, subsystems.get(1));
      assertEquals("TestAppContext instrumented with the following Subsystems:\n"
          + "\tcom.webotech.service.PropSubsystem\n"
          + "\tcom.webotech.service.SupportSubsystem\n", TestingUtil.asNormalisedTxt(logSteam));
    }
  }

  @Test
  void shouldGetContextWithStandardSubsystemsAndMore() {
    TestAppContext appContext = new TestAppContext("test", new String[0]);
    Subsystem subsystem = mock(Subsystem.class);
    appContext = ServiceUtil.instrumentContext(appContext, subsystem);
    List<Subsystem<TestAppContext>> subsystems = appContext.getSubsystems();
    assertEquals(3, subsystems.size());
    assertInstanceOf(PropSubsystem.class, subsystems.get(0));
    assertInstanceOf(SupportSubsystem.class, subsystems.get(1));
    assertSame(subsystem, subsystems.get(2));
  }

}