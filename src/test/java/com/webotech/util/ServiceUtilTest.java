/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.webotech.service.TestAppContext;
import com.webotech.statemachine.service.api.AppService;
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

}