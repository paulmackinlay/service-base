/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service;

import static com.webotech.service.data.SupportData.AVAILABLE_PROCESSOR_COUNT;
import static com.webotech.service.data.SupportData.CL_ARGS;
import static com.webotech.service.data.SupportData.HOSTNAME;
import static com.webotech.service.data.SupportData.IP_ADDRESS;
import static com.webotech.service.data.SupportData.MEMORY_B;
import static com.webotech.service.data.SupportData.MEMORY_GI_B;
import static com.webotech.service.data.SupportData.MEMORY_KI_B;
import static com.webotech.service.data.SupportData.OS_ARCHITECTURE;
import static com.webotech.service.data.SupportData.USER;

import com.webotech.service.data.SupportData;
import com.webotech.service.support.DeadlockDetector;
import com.webotech.statemachine.service.api.AppContext;
import com.webotech.statemachine.service.api.Subsystem;
import com.webotech.util.PropertyUtil;
import java.lang.ProcessHandle.Info;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SupportSubsystem<C extends AppContext<?>> implements Subsystem<C> {

  private static final Logger logger = LogManager.getLogger(SupportSubsystem.class);

  //Host data
  private static final Runtime RUNTIME = Runtime.getRuntime();
  private static final int KIBO_SCALAR = 1024;
  private static final double ONE_THOUSAND = 1000.0;
  private static final int NO_OF_PROCESSORS = RUNTIME.availableProcessors();
  private static final String OS_NAME = System.getProperty("os.name");
  private static final String OS_ARCH = System.getProperty("os.arch");
  private static final String OS_VERSION = System.getProperty("os.version");
  private static String hostname = null;
  private static String ipAddr = null;

  static {
    try {
      InetAddress ip = InetAddress.getLocalHost();
      hostname = ip.getHostName();
      ipAddr = ip.getHostAddress();
    } catch (UnknownHostException e) {
      logger.error("Unable to determine hostname and IP address", e);
    }
  }

  //Process data
  private static final long MAX_MEMORY_BYTES = RUNTIME.maxMemory();
  private static final double b = MAX_MEMORY_BYTES >> 1024; //TODO
  private static final double MAX_MEMORY_MEBI_BYTES =
      Math.round(ONE_THOUSAND * MAX_MEMORY_BYTES / (KIBO_SCALAR * KIBO_SCALAR)) / ONE_THOUSAND;
  private static final double MAX_MEMORY_GIBI_BYTES =
      Math.round(ONE_THOUSAND * MAX_MEMORY_MEBI_BYTES / KIBO_SCALAR) / ONE_THOUSAND;
  private static final Info info = ProcessHandle.current().info();
  private static final long PID = ProcessHandle.current().pid();
  private static final String PROCESS_ARGS = Arrays.toString(
      info.arguments().orElse(new String[0]));
  private static final String PROCESS_USER = info.user().orElse("");

  //JVM data
  private static final String JAVA_SPEC_VERSION = System.getProperty("java.specification.version");
  private static final String JAVA_VERSION = System.getProperty("java.version");
  private static final String JAVA_HOME = System.getProperty("java.home");
  private static final String JAVA_CLASSPATH = System.getProperty("java.class.path");
  private static final Map<String, String> hostMap = Map.of(SupportData.OS_NAME, OS_NAME,
      OS_ARCHITECTURE,
      OS_ARCH, SupportData.OS_VERSION, OS_VERSION, HOSTNAME, hostname, IP_ADDRESS, ipAddr,
      AVAILABLE_PROCESSOR_COUNT, String.valueOf(NO_OF_PROCESSORS));
  private static final Map<String, String> processMap = Map.of(SupportData.PID, String.valueOf(PID),
      CL_ARGS,
      PROCESS_ARGS, USER, PROCESS_USER, MEMORY_B, String.valueOf(MAX_MEMORY_BYTES), MEMORY_KI_B,
      String.valueOf(MAX_MEMORY_MEBI_BYTES), MEMORY_GI_B, String.valueOf(MAX_MEMORY_GIBI_BYTES));
  private static final Map<String, String> jvmMap = Map.of(SupportData.JAVA_SPEC_VERSION,
      JAVA_SPEC_VERSION,
      SupportData.JAVA_VERSION, JAVA_VERSION, SupportData.JAVA_HOME, JAVA_HOME,
      SupportData.JAVA_CLASSPATH, JAVA_CLASSPATH);

  public static final SupportData supportData = new SupportData(hostMap, processMap, jvmMap);
  private final DeadlockDetector deadlockDetector;

  public SupportSubsystem() {
    deadlockDetector = new DeadlockDetector();
  }

  @Override
  public void start(C appContext) {
/*
TODO
Log out classpath, running path
prepare Unique address appname|appinstance|pid|host
deadlock detection
 */
    if (PropertyUtil.getPropertyAsBoolean(
        "com.webotech.service.SupportSubsystem.enableSupportDataLogging", true)) {
      logger.info("\n{}", supportData);
    }

    if (PropertyUtil.getPropertyAsBoolean(
        "com.webotech.service.SupportSubsystem.enableDeadlockDetection", true)) {
      String iso8601Period = PropertyUtil.getProperty(
          "com.webotech.service.SupportSubsystem.deadlockDetectionPeriodIso8601", "PT10S");
      deadlockDetector.startDetecting(iso8601Period);
    }
  }

  @Override
  public void stop(C appContext) {

  }

  private static class DeadlockDetectTask implements Runnable {

    private final ThreadMXBean threadMxBean;

    DeadlockDetectTask(ThreadMXBean threadMxBean) {
      this.threadMxBean = threadMxBean;
    }

    @Override
    public void run() {
      long[] deadlockedThreadIds = threadMxBean.findDeadlockedThreads();
      if (deadlockedThreadIds != null) {
        ThreadInfo[] threadInfos = threadMxBean.getThreadInfo(deadlockedThreadIds, true, true);
        //TODO log this
        System.out.println(threadDump(threadInfos));
        System.out.println(threadDump(threadMxBean.dumpAllThreads(true, true)));
      }
    }

    private String threadDump(ThreadInfo[] threadInfos) {
      return Arrays.stream(threadInfos).map(Object::toString).collect(Collectors.joining());
    }
  }
}
