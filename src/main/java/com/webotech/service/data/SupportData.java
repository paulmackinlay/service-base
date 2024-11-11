/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service.data;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SupportData {

  public static final String OS_NAME = "OS name";
  public static final String OS_ARCHITECTURE = "OS architecture";
  public static final String OS_VERSION = "OS version";
  public static final String HOSTNAME = "Hostname";
  public static final String IP_ADDRESS = "IP address";
  public static final String AVAILABLE_PROCESSOR_COUNT = "Available processor count";
  public static final String PID = "PID";
  public static final String CL_ARGS = "CL args";
  public static final String USER = "User";
  public static final String MEMORY_B = "Memory (B)";
  public static final String MEMORY_KI_B = "Memory (KiB)";
  public static final String MEMORY_GI_B = "Memory (GiB)";
  public static final String JAVA_SPEC_VERSION = "Java spec version";
  public static final String JAVA_VERSION = "Java version";
  public static final String JAVA_HOME = "Java home";
  public static final String JAVA_CLASSPATH = "Java classpath";
  private final Map<String, String> host;
  private final Map<String, String> process;
  private final Map<String, String> jvm;

  public SupportData(Map<String, String> host, Map<String, String> process,
      Map<String, String> jvm) {
    this.host = Collections.unmodifiableMap(new TreeMap<>(host));
    this.process = Collections.unmodifiableMap(new TreeMap<>(process));
    this.jvm = Collections.unmodifiableMap(new TreeMap<>(jvm));
  }

  public Map<String, String> getHost() {
    return host;
  }

  public Map<String, String> getProcess() {
    return process;
  }

  public Map<String, String> getJvm() {
    return jvm;
  }

  private static String toString(Map<String, String> map) {
    return map.entrySet().stream().map(e -> "\t" + e.getKey() + ": " + e.getValue() + "\n").collect(
        Collectors.joining());
  }

  @Override
  public String toString() {
    return new StringBuilder("Host ").append(SupportData.class.getSimpleName()).append("\n")
        .append(toString(host)).append("JVM ").append(SupportData.class.getSimpleName())
        .append("\n").append(toString(jvm)).append("Process ")
        .append(SupportData.class.getSimpleName()).append("\n").append(toString(process))
        .toString();
  }
}
