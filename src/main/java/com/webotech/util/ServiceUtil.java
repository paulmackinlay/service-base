/*
 * Copyright (c) 2024-2025 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

import com.webotech.service.PropSubsystem;
import com.webotech.service.SupportSubsystem;
import com.webotech.statemachine.service.AbstractAppContext;
import com.webotech.statemachine.service.api.AppContext;
import com.webotech.statemachine.service.api.AppService;
import com.webotech.statemachine.service.api.Subsystem;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
    startService(appService, a -> {
      C context = a.getAppContext();
      if (context instanceof WithAppService appContext) {
        appContext.setAppService(a);
      }
    });
  }

  /**
   * This will consume the {@link AppService} in preStartLogic before starting, allowing custom
   * manipulation. The {@link AppService} is then started while handling exceptions. Typically,
   * this will block until the {@link AppService} stops.
   */
  public static <C extends AppContext<?>> void startService(AppService<C> appService,
      Consumer<AppService<C>> preStartLogic) {
    try {
      preStartLogic.accept(appService);
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
    if (logger.isInfoEnabled()) {
      logger.info("{} instrumented with the following Subsystems:{}",
          appContext.getClass().getSimpleName(),
          Arrays.stream(allSubsystems).map(s -> s.getClass().getName())
              .collect(Collectors.joining("\n\t", "\n\t", "")));
    }
    return appContext.withSubsystems(Arrays.asList(allSubsystems));
  }

  /**
   * @return {@link BasicAppContext} that has been equipped in order with a {@link PropSubsystem} and a
   * {@link SupportSubsystem} followed by any supplied {@link Subsystem}s.
   */
  public static BasicAppContext equipBasicContext(String appName, String[] initArgs,
      Subsystem<BasicAppContext>... subsystems) {
    return equipContext(basicContext(appName, initArgs), subsystems);
  }

  private static BasicAppContext basicContext(String appName, String[] initArgs) {
    return new BasicAppContext(appName, initArgs);
  }

  /**
   * A basic {@link AbstractAppContext} implementation which also provides access to the running
   * {@link AppService}.
   */
  public static class BasicAppContext extends AbstractAppContext<BasicAppContext> implements
      WithAppService<BasicAppContext> {

    private final AtomicReference<AppService<BasicAppContext>> appServiceRef;

    public BasicAppContext(String appName, String[] initArgs) {
      super(appName, initArgs);
      appServiceRef = new AtomicReference<>();
    }

    @Override
    public AppService<BasicAppContext> getAppService() {
      return appServiceRef.get();
    }

    @Override
    public void setAppService(AppService<BasicAppContext> appService) {
      appServiceRef.set(appService);
    }
  }

  /**
   * When this API layer is applied to an {@link AppContext} it provides hooks so that a reference
   * to the running {@link AppService} can be set/retrieved.
   * {@link ServiceUtil#startService(AppService)}
   * takes care of setting the reference when the {@link AppService} is started.
   */
  public interface WithAppService<C> {

    void setAppService(AppService<C> appService);

    AppService<C> getAppService();
  }
}
