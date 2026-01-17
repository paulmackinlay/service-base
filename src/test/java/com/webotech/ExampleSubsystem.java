package com.webotech;

import com.webotech.statemachine.service.api.Subsystem;
import com.webotech.util.PropertyUtil;
import com.webotech.util.ServiceUtil.BasicAppContext;
import java.util.concurrent.atomic.AtomicReference;

public class ExampleSubsystem implements Subsystem<BasicAppContext> {

  public ExampleSubsystem(AtomicReference<String> propRef) {
    propRef.set(PropertyUtil.getProperty("key", "oh no!"));
  }

  @Override
  public void start(BasicAppContext appContext) {
    // Do nothing
  }

  @Override
  public void stop(BasicAppContext appContext) {
    // Do nothing
  }
}
