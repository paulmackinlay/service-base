/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech;

import com.webotech.ExampleApp.AppContext;
import com.webotech.service.PropSubsystem;
import com.webotech.service.SupportSubsystem;
import com.webotech.statemachine.service.AbstractAppContext;
import com.webotech.statemachine.service.AbstractAppService;
import com.webotech.util.ServiceUtil;
import java.util.List;

public class ExampleApp extends AbstractAppService<AppContext> {

  ExampleApp(String[] args) {
    super(new AppContext("ExampleApp", args)
        .withSubsystems(List.of(new PropSubsystem<>(), new SupportSubsystem<>())));
  }

  public static void main(String[] args) {
    ServiceUtil.startService(new ExampleApp(args));
  }

  public static class AppContext extends AbstractAppContext<AppContext> {

    protected AppContext(String appName, String[] initArgs) {
      super(appName, initArgs);
    }
  }
}
