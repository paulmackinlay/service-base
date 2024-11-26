/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service;

import com.webotech.statemachine.service.AbstractAppService;
import com.webotech.statemachine.util.Threads;
import com.webotech.util.ArgUtil;
import com.webotech.util.ServiceUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestBaseService extends AbstractAppService<TestAppContext> {

  TestBaseService(String[] args) {
    super(ServiceUtil.instrumentContext(
        new TestAppContext(TestBaseService.class.getSimpleName(), args)));
  }

  public static void main(String[] args) {
    String[] managedArgs = getManagedArgs(args);
    TestBaseService testBaseService = new TestBaseService(managedArgs);
    scheduleServiceStop(testBaseService, 5);
    ServiceUtil.startService(testBaseService);
  }

  private static void scheduleServiceStop(TestBaseService testBaseService, int seconds) {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
        Threads.newNamedDaemonThreadFactory("a-dopo"));
    executor.schedule(() -> testBaseService.stop(), seconds, TimeUnit.SECONDS);
  }

  private static String[] getManagedArgs(String[] args) {
    String[] managedArgs;
    if (ArgUtil.getArgValue(args, "config") == null) {
      List<String> argList = new ArrayList<>(List.of(args));
      argList.add("config=config1.properties");
      managedArgs = argList.toArray(String[]::new);
    } else {
      managedArgs = args;
    }
    return managedArgs;
  }
}
