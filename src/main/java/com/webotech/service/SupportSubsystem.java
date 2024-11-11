/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service;

import static com.webotech.service.data.SupportData.AVAILABLE_PROCESSOR_COUNT;
import static com.webotech.service.data.SupportData.CL_ARGS;
import static com.webotech.service.data.SupportData.HOSTNAME;
import static com.webotech.service.data.SupportData.IP_ADDRESS;
import static com.webotech.service.data.SupportData.JAVA_CLASSPATH;
import static com.webotech.service.data.SupportData.JAVA_HOME;
import static com.webotech.service.data.SupportData.JAVA_SPEC_VERSION;
import static com.webotech.service.data.SupportData.JAVA_VERSION;
import static com.webotech.service.data.SupportData.MEMORY_B;
import static com.webotech.service.data.SupportData.MEMORY_GI_B;
import static com.webotech.service.data.SupportData.MEMORY_KI_B;
import static com.webotech.service.data.SupportData.OS_ARCHITECTURE;
import static com.webotech.service.data.SupportData.OS_NAME;
import static com.webotech.service.data.SupportData.OS_VERSION;
import static com.webotech.service.data.SupportData.PID;
import static com.webotech.service.data.SupportData.USER;

import com.webotech.service.data.SupportData;
import com.webotech.statemachine.service.api.AppContext;
import com.webotech.statemachine.service.api.Subsystem;
import java.lang.ProcessHandle.Info;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SupportSubsystem<C extends AppContext<?>> implements Subsystem<C> {

  private static final Logger logger = LogManager.getLogger(SupportSubsystem.class);

  //Host data
  private static final Runtime RUNTIME = Runtime.getRuntime();
  private static final int kiboScalar = 1024;
  private static final double oneThousand = 1000.0;
  private static final int noOfProcessors = RUNTIME.availableProcessors();
  private static final String osName = System.getProperty("os.name");
  private static final String osArch = System.getProperty("os.arch");
  private static final String osVersion = System.getProperty("os.version");
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
  private static final long maxMemoryBytes = RUNTIME.maxMemory();
  private static final double b = maxMemoryBytes >> 1024; //TODO
  private static final double maxMemoryMebiBytes =
      Math.round(oneThousand * maxMemoryBytes / (kiboScalar * kiboScalar)) / oneThousand;
  private static final double maxMemoryGibiBytes =
      Math.round(oneThousand * maxMemoryMebiBytes / kiboScalar) / oneThousand;
  private static final Info info = ProcessHandle.current().info();
  private static final long pid = ProcessHandle.current().pid();
  private static final String processArgs = Arrays.toString(
      info.arguments().orElseGet(() -> new String[0]));
  private static final String processUser = info.user().orElseGet(() -> "");

  //JVM data
  private static final String javaSpecVersion = System.getProperty("java.specification.version");
  private static final String javaVersion = System.getProperty("java.version");
  private static final String javaHome = System.getProperty("java.home");
  private static final String javaClasspath = System.getProperty("java.class.path");
  private static final Map<String, String> hostMap = Map.of(OS_NAME, osName, OS_ARCHITECTURE,
      osArch, OS_VERSION, osVersion, HOSTNAME, hostname, IP_ADDRESS, ipAddr,
      AVAILABLE_PROCESSOR_COUNT, String.valueOf(noOfProcessors));
  private static final Map<String, String> processMap = Map.of(PID, String.valueOf(pid), CL_ARGS,
      processArgs, USER, processUser, MEMORY_B, String.valueOf(maxMemoryBytes), MEMORY_KI_B,
      String.valueOf(maxMemoryMebiBytes), MEMORY_GI_B, String.valueOf(maxMemoryGibiBytes));
  private static final Map<String, String> jvmMap = Map.of(JAVA_SPEC_VERSION, javaSpecVersion,
      JAVA_VERSION, javaVersion, JAVA_HOME, javaHome, JAVA_CLASSPATH, javaClasspath);

  public static final SupportData supportData = new SupportData(hostMap, processMap, jvmMap);

  @Override
  public void start(C appContext) {
/*
TODO
Log out classpath, running path
prepare Unique address appname|appinstance|pid|host
deadlock detection
Runtime.getRuntime()...

everything here is static
 */
    logger.info("\n{}", supportData);
  }

  @Override
  public void stop(C appContext) {

  }
}
