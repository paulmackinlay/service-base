/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Should be used to maintain all the properties for a process. The intended use of this class is:
 * <ol>
 * <li>load properties from resources or files using the load* methods at app start-up</li>
 * <li>use get* methods to retrieve properties through-out the app in a thread safe manner</li>
 * </ol>
 * Loading validates that the properties are sensible (no duplicate keys, no leading/trailing
 * whitespace). It also drops any properties that are defined as System properties since it is
 * useful (and usual) to override configured properties using command line parameters. Finally,
 * retrieving a property is done with this cascading approach:
 * <ol>
 * <li>get the internal property value, if it doesn't exist</li>
 * <li>get the System property value, if it doesn't exist</li>
 * <li>get the passed in default value</li>
 * </ol>
 * <p>
 * The methods in this utility class read/write state to an internal {@link Properties} object which
 * is thread-safe.
 */
public final class PropertyUtil {

  private static final Logger logger = LogManager.getLogger(PropertyUtil.class);
  private static final String FALSE = "false";
  private static final String TRUE = "true";
  private static final String PROPERTIES_EXT = ".properties";
  private static final String LEADING_REGEX = "\\s+.*";
  private static final String TRAILING_REGEX = ".*\\s+";
  private static final AtomicBoolean isChecked = new AtomicBoolean(false);
  private static final Properties config = new Properties();

  private PropertyUtil() {
    // Not for instanciation outside this class
  }

  /**
   * Loads properties from one or more resources
   */
  public static void loadPropertyResources(String... resources) {
    Stream.of(resources).forEach(PropertyUtil::loadPropertiesResource);
  }

  /**
   * Loads properties from all *.properties resources in resourceDir.
   */
  public static void loadAllPropertyResources(String resourceDir) {
    try {
      Path resourcePath = Path.of(
          PropertyUtil.class.getClassLoader().getResource(resourceDir).toURI());
      if (Files.isDirectory(resourcePath)) {
        loadProperiesFilesFromDir(resourcePath);
      } else {
        throw new IllegalArgumentException("[" + resourceDir + "] is not a resource directory");
      }
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Loads properties from one or more files.
   */
  public static void loadPropertyFiles(String... propertyFiles) {
    logger.info("Loading properties in files {}", Arrays.toString(propertyFiles));
    for (String propertyFile : propertyFiles) {
      Path propertyPath = Paths.get(propertyFile);
      if (Files.isRegularFile(propertyPath)) {
        try {
          loadPropertiesStream(Files.newInputStream(propertyPath));
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      } else {
        throw new IllegalArgumentException("Expect [" + propertyFile + "] to be a property file.");
      }
    }
  }

  /**
   * Loads properties from all files with extension <i>properties</i> in directory propertyDir.
   */
  public static void loadAllPropertyFiles(String propertyDir) {
    logger.info("Loading all .properties files in directory [{}]", propertyDir);
    Path dir = Paths.get(propertyDir);
    if (Files.isDirectory(dir)) {
      loadProperiesFilesFromDir(dir);
    } else {
      throw new IllegalArgumentException("Expect a directory with *.properties files in it");
    }
  }

  /**
   * Returns a property value from the loaded properties, otherwise the system
   * property value, otherwise the defaultValue.
   */
  public static String getProperty(String propertyKey, String defaultValue) {
    checkPropertiesOnce();
    return config.containsKey(propertyKey) ? config.getProperty(propertyKey)
        : System.getProperty(propertyKey, defaultValue);
  }

  /**
   * Returns a property as an int from the loaded properties, otherwise the system
   * property value, otherwise the defaultValue.
   */
  public static int getPropertyAsInt(String propertyKey, int defaultValue) {
    checkPropertiesOnce();
    return Integer.parseInt(getProperty(propertyKey, Integer.toString(defaultValue)));
  }

  /**
   * Returns a property as a boolean from the loaded properties, otherwise the system
   * property value, otherwise the defaultValue.
   */
  public static boolean getPropertyAsBoolean(String propertyKey, boolean defaultValue) {
    checkPropertiesOnce();
    return Boolean.parseBoolean(getProperty(propertyKey, defaultValue ? TRUE : FALSE));
  }

  /**
   * Returns a property as a List<String> from the loaded properties, otherwise the system
   * property value, otherwise the defaultValue.
   */
  public static List<String> getPropertyAsList(String propertyKey, List<String> defaultValue) {
    checkPropertiesOnce();
    String csv = getProperty(propertyKey, null);
    return csv != null ? Stream.of(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
        .collect(Collectors.toList()) : defaultValue;
  }

  /**
   * @return the loaded properties
   */
  public static Map<String, String> getProperties() {
    return config.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
  }

  /**
   * Creates or updates a property with key.
   *
   * @return - the previous value of the property if it was updated
   */
  public static String setProperty(String key, String value) {
    Object previous = config.setProperty(key, value);
    if (previous != null) {
      String prev = String.valueOf(previous);
      logger.warn("Property with key [{}] had value [{}] replaced with [{}]", key,
          prev, value);
      return prev;
    }
    return null;
  }

  /**
   * Removes a property with key.
   *
   * @return - the previous value of the property if it was deleted
   */
  public static String removeProperty(String key) {
    Object previous = config.remove(key);
    if (previous != null) {
      String prev = String.valueOf(previous);
      logger.warn("Property with key [{}] has been removed", key);
      return prev;
    }
    return null;
  }

  /**
   * @return true if resource is a directory
   */
  public static boolean isResourceDir(String resource) {
    URL resourceUrl = PropertyUtil.class.getClassLoader().getResource(resource);
    if (resourceUrl != null) {
      try {
        Path resourcePath = Path.of(resourceUrl.toURI());
        return Files.isDirectory(resourcePath);
      } catch (URISyntaxException e) {
        // ignore
      }
    }
    return false;
  }

  private static void loadProperiesFilesFromDir(Path dir) {
    try (Stream<Path> list = Files.list(dir)) {
      list.filter(f -> Files.isRegularFile(f) && f.toString().endsWith(PROPERTIES_EXT))
          .forEach(f -> loadPropertyFiles(f.toString()));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void checkPropertiesOnce() {
    if (isChecked.compareAndSet(false, true) && config.isEmpty()) {
      logger.warn(
          "No properties have been loaded, did you forget to call one of the load methods?");
    }
  }

  private static void loadPropertiesStream(InputStream inputStream) throws IOException {
    if (inputStream != null) {
      inputStream = validateDuplicateKeys(inputStream);
      Properties newProperties = new Properties();
      newProperties.load(inputStream);
      stripSystemProperties(newProperties);
      validate(newProperties);
      config.putAll(newProperties);
      for (String key : config.stringPropertyNames()) {
        if (config.getProperty(key) == null || config.getProperty(key).isEmpty()) {
          logger.warn("Removing empty property with key [{}]", key);
          config.remove(key);
        }
      }
    } else {
      logger.warn("Properties stream does not exist");
    }
  }

  private static Set<String> existingPropertyKeys() {
    return config.stringPropertyNames();
  }

  private static void validate(Properties newProperties) {
    Set<String> existingPropertyKeys = existingPropertyKeys();
    newProperties.entrySet().forEach(e -> {
      String n = String.valueOf(e.getKey());
      String v = String.valueOf(e.getValue());
      validateTxt(n);
      validateValue(n, v);
      validateDuplicate(existingPropertyKeys, n);
    });
  }

  private static void stripSystemProperties(Properties newProperties) {
    newProperties.stringPropertyNames().stream().forEach(k -> {
      if (System.getProperty(k) != null) {
        logger.warn(
            "System property with key [{}] exists, it will not be loaded into the internal properties",
            k);
        newProperties.remove(k);
      }
    });
  }

  private static void validateDuplicate(Set<String> existingPropertyKeys, String newPropertyKey) {
    if (existingPropertyKeys.contains(newPropertyKey)) {
      throw new IllegalStateException(
          "Property with key [" + newPropertyKey + "] is already defined");
    }
  }

  private static void validateValue(String key, String value) {
    try {
      validateTxt(value);
    } catch (IllegalArgumentException e) {
      logger.warn("Property with key [{}] has an invalid value [{}]", key, value);
    }
  }

  private static void validateTxt(String txt) {
    if (txt == null) {
      throw new IllegalArgumentException("'" + txt + "' cannot be null");
    }
    if (Pattern.matches(LEADING_REGEX, txt) || Pattern.matches(TRAILING_REGEX, txt)) {
      throw new IllegalArgumentException("'" + txt + "' contains leading/trailing whitespace");
    }
    if (txt.length() < 1) {
      throw new IllegalArgumentException("'" + txt + "' cannot be empty");
    }
  }

  private static void loadPropertiesResource(String propertiesResourceName) {
    logger.info("Loading properties from resource [{}]", propertiesResourceName);
    try {
      loadPropertiesStream(
          PropertyUtil.class.getClassLoader().getResourceAsStream(propertiesResourceName));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static InputStream validateDuplicateKeys(InputStream propertyStream) {
    Map<String, String> kvs = new HashMap<>();
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      propertyStream.transferTo(baos);
      InputStream propertyStreamClone = new ByteArrayInputStream(baos.toByteArray());
      InputStream propertyStreamClone1 = new ByteArrayInputStream(baos.toByteArray());
      try (BufferedReader bufferedReader = new BufferedReader(
          new InputStreamReader(propertyStreamClone))) {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          if (line.trim().startsWith("#")) {
            continue;
          }
          String[] kv = line.split("=", 2);
          if (kvs.containsKey(kv[0])) {
            throw new IllegalArgumentException(
                "Property stream contains duplicate key [" + kv[0] + "]");
          }
          kvs.put(kv[0], kv[1]);
        }
      }
      return propertyStreamClone1;
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
