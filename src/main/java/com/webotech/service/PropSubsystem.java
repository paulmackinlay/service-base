/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service;

import com.webotech.statemachine.service.api.AppContext;
import com.webotech.statemachine.service.api.Subsystem;
import com.webotech.util.ArgUtil;
import com.webotech.util.PropertyUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * TODO
 * <p>
 * - check System.Property, arg, default
 * - load file, resource
 * <p>
 * java my_app config=A
 * java my_app config=A,B
 * java my_app -Dconfig=A
 * java my_app -Dconfig=A,B
 */
public class PropSubsystem<C extends AppContext<?>> implements Subsystem<C> {

  private static final Logger logger = LogManager.getLogger(PropSubsystem.class);
  private static final String CONFIG_KEY = "config";
  private static final String EXCL_REGEX_PATTERN = "(?i).*%s.*";
  //TODO javadoc has to say that property file names should contain characters that are alphanumeric or [-_.]
  final static Pattern csvSysPattern = Pattern.compile(
      "^([a-zA-Z0-9\\.\\-_" + Pattern.quote(File.separator) + "]*)\\,+([a-zA-Z0-9\\\\."
          + Pattern.quote(File.separator) + "]*)$");
  final static Pattern sysPattern = Pattern.compile(
      "^([a-zA-Z0-9\\.\\-_" + Pattern.quote(File.separator) + "]*)$");
  final static Pattern csvArgPattern = Pattern.compile(
      "^" + CONFIG_KEY + "\\=([a-zA-Z0-9\\.\\-_" + Pattern.quote(File.separator)
          + "]*)\\,+([a-zA-Z0-9\\\\." + Pattern.quote(File.separator) + "]*)$");
  final static Pattern argPattern = Pattern.compile(
      "^" + CONFIG_KEY + "\\=([a-zA-Z0-9\\.\\-_" + Pattern.quote(File.separator) + "]*)$");
  //TODO should these be public? They should be talked about in javadoc
  public static final String PROP_KEY_LOG_PROP_VALUES_AFTER_LOAD = "com.webotech.service.PropSubsystem.logPropValuesAfterLoad";
  public static final String PROP_KEY_EXCLUDE_PROP_LOG_FOR_KEYS_CONTAINING_CSV = "com.webotech.service.PropSubsystem.excludePropLogForKeysContainingCsv";

  @Override
  public void start(C appContext) {
    logger.info("Loading properties");
    List<String> propFiles = determinePropFiles(appContext.getInitArgs());
    loadProps(propFiles);
    logProps();
  }

  private static void logProps() {
    Map<String, String> loadedProps = new TreeMap<>(PropertyUtil.getProperties());
    logger.info("{} properties loaded", loadedProps.size());
    if (PropertyUtil.getPropertyAsBoolean(PROP_KEY_LOG_PROP_VALUES_AFTER_LOAD, false)) {
      List<String> keyExcludes = PropertyUtil.getPropertyAsList(
          PROP_KEY_EXCLUDE_PROP_LOG_FOR_KEYS_CONTAINING_CSV, List.of());
      for (Entry<String, String> entry : loadedProps.entrySet()) {
        String key = entry.getKey();
        String value = cleanValue(keyExcludes, entry);
        logger.info("{}={}", key, value);
      }
    }
  }

  private static String cleanValue(List<String> keyExcludes, Entry<String, String> entry) {
    String key = entry.getKey();
    for (String exclude : keyExcludes) {
      if (key.matches(String.format(EXCL_REGEX_PATTERN, exclude))) {
        return "***";
      }
    }
    return entry.getValue();
  }

  private static void loadProps(List<String> propFiles) {
    for (String file : propFiles) {
      Path path = Path.of(file);
      if (Files.isRegularFile(path)) {
        if (Files.isDirectory(path)) {
          PropertyUtil.loadAllPropertyFiles(file);
        } else {
          PropertyUtil.loadPropertyFiles(file);
        }
      } else {
        PropertyUtil.loadPropertyResources(file);
      }
    }
  }

  private static <C extends AppContext<?>> List<String> determinePropFiles(String[] initArgs) {
    List<String> propFiles = parse(System.getProperty(CONFIG_KEY), true);
    if (propFiles.isEmpty()) {
      propFiles = parse(ArgUtil.getArgValue(initArgs, CONFIG_KEY), false);
    }
    if (propFiles.isEmpty()) {
      propFiles.add("config.properties");
    }
    return propFiles;
  }

  @Override
  public void stop(C appContext) {
    logger.info("Unloading properties");
    PropertyUtil.getProperties().keySet().forEach(PropertyUtil::removeProperty);
  }

  static List<String> parse(String txt, boolean isSysProp) {
    List<String> propFiles = new ArrayList<>();
    if (txt != null) {
      Matcher matcher = isSysProp ? csvSysPattern.matcher(txt) : csvArgPattern.matcher(txt);
      if (!matcher.matches()) {
        matcher = isSysProp ? sysPattern.matcher(txt) : argPattern.matcher(txt);
      }
      if (matcher.matches()) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
          propFiles.add(matcher.group(i));
        }
      }
    }
    return propFiles;
  }
}
