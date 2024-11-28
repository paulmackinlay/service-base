/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

import com.webotech.service.PropSubsystem;
import com.webotech.service.SupportSubsystem;
import com.webotech.statemachine.service.AbstractAppContext;
import com.webotech.statemachine.service.api.AppContext;
import com.webotech.statemachine.service.api.AppService;
import com.webotech.statemachine.service.api.Subsystem;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceUtil {

  private static final Logger logger = LogManager.getLogger(ServiceUtil.class);

  private ServiceUtil() {
    // Not for instanciation outside this class
  }

  /**
   * This will start an {@link AppService} while handling exceptions. Typically, this will block
   * until the {@link AppService} stops.
   */
  public static <C extends AppContext<?>> void startService(AppService<C> appService) {
    try {
      appService.start();
    } catch (Exception e) {
      appService.stop();
    }
  }

  /**
   * Instruments an {@link AbstractAppContext} with these {@link Subsystem} at the beginning of the
   * list, in order:
   * <ol>
   *   <li>{@link PropSubsystem}</li>
   *   <li>{@link SupportSubsystem}</li>
   * </ol>
   */
  public static <C extends AbstractAppContext<C>> C equipContext(C appContext,
      Subsystem<C>... subsystems) {
    Subsystem<C>[] standardSubsystems = new Subsystem[]{new PropSubsystem<C>(),
        new SupportSubsystem<C>()};
    Subsystem<C>[] allSubsystems = Arrays.copyOf(standardSubsystems,
        standardSubsystems.length + subsystems.length);
    System.arraycopy(subsystems, 0, allSubsystems, 2, subsystems.length);
    logger.info("{} instrumented with the following Subsystems:{}",
        appContext.getClass().getSimpleName(),
        Arrays.stream(allSubsystems).map(s -> s.getClass().getName())
            .collect(Collectors.joining("\n\t", "\n\t", "")));
    return appContext.withSubsystems(Arrays.asList(allSubsystems));
  }

  /**
   * @return {@link BasicAppContext} that has been equipped in order with a {@link PropSubsystem} and a
   * {@link SupportSubsystem}
   */
  public static BasicAppContext equipBasicContext(String appName, String[] initArgs) {
    return equipContext(basicContext(appName, initArgs));
  }

  private static BasicAppContext basicContext(String appName, String[] initArgs) {
    return new BasicAppContext(appName, initArgs);
  }

  /**
   * A basic {@link AbstractAppContext} implementation with no custom data.
   */
  public static class BasicAppContext extends AbstractAppContext<BasicAppContext> {

    public BasicAppContext(String appName, String[] initArgs) {
      super(appName, initArgs);
    }
  }
}
