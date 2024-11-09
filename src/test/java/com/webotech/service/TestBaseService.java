/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service;

import com.webotech.statemachine.service.AbstractAppService;
import com.webotech.statemachine.service.api.AppService;
import com.webotech.util.ServiceUtil;
import java.util.List;

public class TestBaseService extends AbstractAppService<TestAppContext> {

  public TestBaseService(String[] args) {
    super(new TestAppContext(TestBaseService.class.getSimpleName(), args).withSubsystems(
        List.of(new PropSubsystem<>())));
  }

  public static void main(String[] args) {
    AppService<TestAppContext> app = new TestBaseService(args);
    ServiceUtil.startService(app);
  }
}
