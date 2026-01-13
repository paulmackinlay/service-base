/*
 * Copyright (c) 2024-2025 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

import com.webotech.TestingUtil;
import com.webotech.service.PropSubsystem;
import com.webotech.service.SupportSubsystem;
import com.webotech.service.TestAppContext;
import com.webotech.statemachine.service.api.AppService;
import com.webotech.statemachine.service.api.Subsystem;
import com.webotech.util.ServiceUtil.BasicAppContext;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceUtilTest {

  private AppService<TestAppContext> appService;
  private AppService<BasicAppContext> basicAppService;

  @BeforeEach
  void setup() {
    appService = mock(AppService.class);
    basicAppService = mock(AppService.class);
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
  void shouldRunPreStartLogic() {
    AtomicReference<TestAppContext> refToContext = new AtomicReference<>();
    ServiceUtil.startService(appService, a -> refToContext.set(a.getAppContext()));
    assertSame(appService.getAppContext(), refToContext.get());
  }

  @Test
  void shouldRunPreStartLogicBeforeStartException() {
    AtomicReference<TestAppContext> refToContext = new AtomicReference<>();
    IllegalStateException exception = new IllegalStateException("test induced");
    doThrow(exception).when(appService).start();
    ServiceUtil.startService(appService, a -> refToContext.set(a.getAppContext()));
    assertSame(appService.getAppContext(), refToContext.get());
    verify(appService, times(1)).stop();
  }

  @Test
  void shouldSetAppServiceOnContext() {
    when(basicAppService.getAppContext()).thenReturn(new BasicAppContext("appName", null));
    ServiceUtil.startService(basicAppService);
    assertSame(basicAppService, basicAppService.getAppContext().getAppService());
  }


  @Test
  void shouldGetContextWithStandardSubsystems() throws IOException {
    TestAppContext appContext = new TestAppContext("test", new String[0]);
    try (OutputStream logSteam = TestingUtil.initLogCaptureStream()) {
      appContext = ServiceUtil.equipContext(appContext);
      List<Subsystem<TestAppContext>> subsystems = appContext.getSubsystems();
      assertEquals(2, subsystems.size());
      assertInstanceOf(PropSubsystem.class, subsystems.get(0));
      assertInstanceOf(SupportSubsystem.class, subsystems.get(1));
      assertEquals("Loading properties\n"
          + "Loading properties from resource [config.properties]\n"
          + "0 properties loaded\n"
          + "TestAppContext instrumented with the following Subsystems:\n"
          + "\tcom.webotech.service.PropSubsystem\n"
          + "\tcom.webotech.service.SupportSubsystem\n", TestingUtil.asNormalisedTxt(logSteam));
    }
  }

  @Test
  void shouldGetContextWithStandardSubsystemsAndMore() {
    TestAppContext appContext = new TestAppContext("test", new String[0]);
    Subsystem subsystem = mock(Subsystem.class);
    appContext = ServiceUtil.equipContext(appContext, subsystem);
    List<Subsystem<TestAppContext>> subsystems = appContext.getSubsystems();
    assertEquals(3, subsystems.size());
    assertInstanceOf(PropSubsystem.class, subsystems.get(0));
    assertInstanceOf(SupportSubsystem.class, subsystems.get(1));
    assertSame(subsystem, subsystems.get(2));
  }

  @Test
  void shouldGetEquippedBasicContext() {
    String appName = "AnApp";
    String[] args = new String[0];
    BasicAppContext appContext = ServiceUtil.equipBasicContext(appName, args);
    assertSame(args, appContext.getInitArgs());
    assertSame(appName, appContext.getAppName());
    List<? extends Class<? extends Subsystem>> subsystemClasses = appContext.getSubsystems()
        .stream().map(s -> s.getClass()).toList();
    List<? extends Class<? extends Subsystem>> expectedClasses = List.of(PropSubsystem.class,
        SupportSubsystem.class);
    assertEquals(expectedClasses, subsystemClasses);
  }

  @Test
  void shouldGetEquippedBasicContextWithSubsystems() {
    Subsystem<BasicAppContext> subsystem1 = mock(Subsystem.class);
    Subsystem<BasicAppContext> subsystem2 = mock(Subsystem.class);
    String appName = "AnApp";
    String[] args = new String[0];
    BasicAppContext appContext = ServiceUtil.equipBasicContext(appName, args, subsystem1,
        subsystem2);
    assertSame(args, appContext.getInitArgs());
    assertSame(appName, appContext.getAppName());
    List<Subsystem<BasicAppContext>> subsystems = appContext.getSubsystems();
    List<? extends Class<? extends Subsystem>> subsystemClasses = subsystems
        .stream().map(s -> s.getClass()).toList();
    assertEquals(PropSubsystem.class, subsystemClasses.get(0));
    assertEquals(SupportSubsystem.class, subsystemClasses.get(1));
    assertSame(subsystem1, subsystems.get(2));
    assertSame(subsystem2, subsystems.get(3));
  }
}