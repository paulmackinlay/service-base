/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech;

import com.webotech.statemachine.service.AbstractAppService;
import com.webotech.util.ServiceUtil;
import com.webotech.util.ServiceUtil.BasicAppContext;

public class ExampleApp extends AbstractAppService<BasicAppContext> {

  ExampleApp(String[] args) {
    super(ServiceUtil.equipBasicContext("ExampleApp", args));
  }

  public static void main(String[] args) {
    ServiceUtil.startService(new ExampleApp(args));
  }
}
