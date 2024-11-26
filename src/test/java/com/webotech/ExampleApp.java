/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech;

import com.webotech.ExampleApp.ExampleContext;
import com.webotech.statemachine.service.AbstractAppContext;
import com.webotech.statemachine.service.AbstractAppService;
import com.webotech.util.ServiceUtil;

public class ExampleApp extends AbstractAppService<ExampleContext> {

  ExampleApp(String[] args) {
    super(ServiceUtil.instrumentContext(new ExampleContext("ExampleApp", args)));
  }

  public static void main(String[] args) {
    ServiceUtil.startService(new ExampleApp(args));
  }

  public static class ExampleContext extends AbstractAppContext<ExampleContext> {

    ExampleContext(String appName, String[] initArgs) {
      super(appName, initArgs);
    }
  }
}
