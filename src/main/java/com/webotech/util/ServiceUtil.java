/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

import com.webotech.statemachine.service.api.AppContext;
import com.webotech.statemachine.service.api.AppService;

public class ServiceUtil {

  private ServiceUtil() {
    // Not for instanciation outside this class
  }

  /**
   * This will start an {@link AppService} handling exceptions. Typically, this will block until the
   * {@link AppService} stops.
   */
  //TODO test this
  public static <C extends AppContext<?>> void startService(AppService<C> appService) {
    try {
      appService.start();
    } catch (Exception e) {
      appService.stop();
    }
  }
}
