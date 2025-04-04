/*
 * Copyright (c) 2024-2025 Paul Mackinlay <paul.mackinlay@gmail.com>
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
 * A {@link Subsystem} that loads properties for use within an application. This should be the
 * first subsystem that is used, once started properties can be accessed statically using
 * {@link PropertyUtil}.
 * <p>
 * Properties are loaded from one or more files that are defined using a System property with key
 * {@link PropSubsystem#CONFIG_KEY} or a command line argument like
 * <i>config=config.properties</i>.
 * If both are defined, the System property will override the argument.
 * <p>
 * You can define a single property file, a comma separated list or a directory that contains
 * multiple *.properties files. Note that property file names should contain characters that are
 * alphanumeric or -_. (hyphen, underscore, dot).
 * <p> Command line usage using arguments is as follows:
 * <pre>
 *      java MyApp config=prop1.properties
 *      java MyApp config=prop1.properties,prop2.properties
 *      java MyApp config=config_dir/
 * </pre>
 * <p>
 * Property files will be loaded first as regular files in the filesystem, if they don't exist they
 * will be loaded as internally packaged resources, if they don't exist it will attempt an
 * internally packaged <i>config.properties</i> resource.
 * <p>
 * Loaded properties are stripped of leading/trailing whitespace and properties with duplicate keys
 * will cause an {@link IllegalStateException}. While loading properties a System property with
 * the same key will override a property defined in a file - this allows you to override a property
 * using command line.
 * <p>
 * The following properties allow you to control if and how properties are logged:
 * <ul>
 * <li>{@link PropSubsystem#PROP_KEY_LOG_PROP_VALUES_AFTER_LOAD} - controls if properties are logged
 * and has a default value of <pre>true</pre></li>
 * <li>{@link PropSubsystem#PROP_KEY_EXCLUDE_PROP_LOG_FOR_KEYS_CONTAINING_CSV} - excludes logging of
 * property values where keys contain a {@link String} in the CSV and has a default value of
 * <pre>secret,password,passwd,credential</pre></li>
 * </ul>
 * By default, the properties are equivalent to using
 * <pre>
 * com.webotech.service.PropSubsystem.logPropValuesAfterLoad=true
 * com.webotech.service.PropSubsystem.excludePropLogForKeysContainingCsv=secret,password,passwd,credential
 * </pre>
 */
public class PropSubsystem<C extends AppContext<?>> implements Subsystem<C> {

  private static final Logger logger = LogManager.getLogger(PropSubsystem.class);
  public static final String CONFIG_KEY = "config";
  private static final String EXCL_REGEX_PATTERN = "(?i).*%s.*";
  private static final List<String> defaultExclList = List.of("secret", "password", "passwd",
      "credential");
  static final Pattern csvPropPattern = Pattern.compile(
      "^([a-zA-Z0-9\\.\\-_" + Pattern.quote(File.separator) + "]*),+([a-zA-Z0-9\\."
          + Pattern.quote(File.separator) + "]*)$");
  static final Pattern propPattern = Pattern.compile(
      "^([a-zA-Z0-9\\.\\-_" + Pattern.quote(File.separator) + "]*)$");
  /**
   * Property key with expected value of true|false to control if properties are logged after
   * loading. The default value is true.
   */
  public static final String PROP_KEY_LOG_PROP_VALUES_AFTER_LOAD = "com.webotech.service.PropSubsystem.logPropValuesAfterLoad";
  /**
   * Property key with expected CSV value containing a list of property keys for which values are
   * not logged. Generally used to obscure sensitive information in logs.
   */
  public static final String PROP_KEY_EXCLUDE_PROP_LOG_FOR_KEYS_CONTAINING_CSV = "com.webotech.service.PropSubsystem.excludePropLogForKeysContainingCsv";

  @Override
  public void start(C appContext) {
    logger.info("Loading properties");
    List<String> propFiles = determinePropFiles(appContext.getInitArgs());
    loadProps(propFiles);
    logProps();
  }

  private static void logProps() {
    Map<String, String> loadedProps = new TreeMap<>(PropertyUtil.getPropertiesAsMap());
    logger.info("{} properties loaded", loadedProps.size());
    if (PropertyUtil.getPropertyAsBoolean(PROP_KEY_LOG_PROP_VALUES_AFTER_LOAD, true)) {
      List<String> keyExcludes = PropertyUtil.getPropertyAsList(
          PROP_KEY_EXCLUDE_PROP_LOG_FOR_KEYS_CONTAINING_CSV, defaultExclList);
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
        if (PropertyUtil.isResourceDir(file)) {
          PropertyUtil.loadAllPropertyResources(file);
        } else {
          PropertyUtil.loadPropertyResources(file);
        }
      }
    }
  }

  private static List<String> determinePropFiles(String[] initArgs) {
    List<String> propFiles = parse(System.getProperty(CONFIG_KEY));
    if (propFiles.isEmpty()) {
      propFiles = parse(ArgUtil.getArgValue(initArgs, CONFIG_KEY));
    }
    if (propFiles.isEmpty()) {
      propFiles.add("config.properties");
    }
    return propFiles;
  }

  @Override
  public void stop(C appContext) {
    logger.info("Unloading properties");
    PropertyUtil.getPropertiesAsMap().keySet().forEach(PropertyUtil::removeProperty);
  }

  static List<String> parse(String txt) {
    List<String> propFiles = new ArrayList<>();
    if (txt != null) {
      Matcher matcher = csvPropPattern.matcher(txt);
      if (!matcher.matches()) {
        matcher = propPattern.matcher(txt);
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
