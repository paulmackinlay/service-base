/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service;

import com.webotech.statemachine.service.AbstractAppContext;

public class TestAppContext extends AbstractAppContext<TestAppContext> {

  public TestAppContext(String appName, String[] initArgs) {
    super(appName, initArgs);
  }
}
