/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech;

import com.webotech.statemachine.service.AbstractAppService;
import com.webotech.util.ServiceUtil;
import com.webotech.util.ServiceUtil.BasicAppContext;
import java.util.concurrent.atomic.AtomicReference;

public class ExampleApp extends AbstractAppService<BasicAppContext> {

  public static final AtomicReference<String> propRef = new AtomicReference<>();

  ExampleApp(String[] args) {
    super(ServiceUtil.preemptAppProps(args).equipBasicContext("ExampleApp", args, new ExampleSubsystem(propRef)));
  }

  public static void main(String[] args) {
    ServiceUtil.startService(new ExampleApp(args));
  }
}
