/*
 * Copyright (c) 2024-2025 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropertyUtilTest {

  private static final Map<String, String> expectedProps1 = Map.of("prop1", "a-value", "prop2",
      "true", "prop3", "false", "prop4", "one,two,three", "prop5", "23");
  private static final Map<String, String> expectedProps2 = Map.of("prop6", "xyz");
  private static final Map<String, String> expectedProps3 = Map.of("prop1", "value1", "prop2",
      "value2", "prop3", "value3", "prop4", "value4");

  @BeforeEach
  void setup() {
    PropertyUtil.getPropertiesAsMap().keySet().stream()
        .forEach(c -> PropertyUtil.removeProperty(c));
    assertTrue(PropertyUtil.getPropertiesAsMap().isEmpty());
    PropertyUtil.getProperty("blah", "blah");
  }

  @Test
  void shouldLoadAllPropFilesFromResourceDir() {
    PropertyUtil.loadAllPropertyResources("happy/");
    assertEquals(Map.of("prop1", "value1", "prop2", "value2", "prop3", "value3", "prop4", "value4"),
        PropertyUtil.getPropertiesAsMap());
  }

  @Test
  void shouldLoadPropsFromResource() {
    PropertyUtil.loadPropertyResources("test1.properties");
    Map<String, String> props = PropertyUtil.getPropertiesAsMap();
    assertEquals(5, props.size());
    assertEquals(expectedProps1, props);
  }

  @Test
  void shouldLoadPropsFromResources() {
    PropertyUtil.loadPropertyResources("test1.properties", "test2.properties");
    Map<String, String> props = PropertyUtil.getPropertiesAsMap();
    assertEquals(6, props.size());
    Map<String, String> expectedProps = new HashMap<>(expectedProps1);
    expectedProps.putAll(expectedProps2);
    assertEquals(expectedProps, props);
  }

  @Test
  void shouldLoadPropsFromFile() {
    PropertyUtil.loadPropertyFiles("src/test/resources/test1.properties");
    Map<String, String> props = PropertyUtil.getPropertiesAsMap();
    assertEquals(5, props.size());
    assertEquals(expectedProps1, props);
  }

  @Test
  void shouldLoadPropsFromFiles() {
    PropertyUtil.loadPropertyFiles("src/test/resources/test1.properties",
        "src/test/resources/test2.properties");
    Map<String, String> props = PropertyUtil.getPropertiesAsMap();
    assertEquals(6, props.size());
    Map<String, String> expectedProps = new HashMap<>(expectedProps1);
    expectedProps.putAll(expectedProps2);
    assertEquals(expectedProps, props);
  }

  @Test
  void shouldLoadPropsFromDirectory() {
    PropertyUtil.loadAllPropertyFiles("src/test/resources/happy");
    Map<String, String> props = PropertyUtil.getPropertiesAsMap();
    assertEquals(4, props.size());
    assertEquals(expectedProps3, props);
  }

  @Test
  void shouldDisallowNonPropertyFileLoad() {
    assertThrows(IllegalArgumentException.class,
        () -> PropertyUtil.loadPropertyFiles("src/test/resources/"));
  }

  @Test
  void shouldDisallowNonDirectoryLoad() {
    assertThrows(IllegalArgumentException.class,
        () -> PropertyUtil.loadAllPropertyFiles("src/test/resources/test1.properties"));
  }

  @Test
  void shouldGetPropertiesAsSpecialisation() {
    PropertyUtil.loadPropertyResources("test1.properties");
    assertEquals(23, PropertyUtil.getPropertyAsInt("prop5", 0));
    assertTrue(PropertyUtil.getPropertyAsBoolean("prop2", false));
    assertFalse(PropertyUtil.getPropertyAsBoolean("prop3", true));
    assertEquals(List.of("one", "two", "three"),
        PropertyUtil.getPropertyAsList("prop4", List.of()));
    assertEquals("a-value", PropertyUtil.getProperty("prop1", ""));
  }

  @Test
  void shouldGetPropertiesAsDefaults() {
    assertEquals(0, PropertyUtil.getPropertyAsInt("prop5", 0));
    assertFalse(PropertyUtil.getPropertyAsBoolean("prop2", false));
    assertTrue(PropertyUtil.getPropertyAsBoolean("prop3", true));
    assertEquals(List.of(), PropertyUtil.getPropertyAsList("prop4", List.of()));
    assertEquals("", PropertyUtil.getProperty("prop1", ""));
  }

  @Test
  void shouldGetPropertiesAsSystemProps() {
    try {
      System.setProperty("prop1", "a-sys-value");
      System.setProperty("prop2", "true");
      System.setProperty("prop3", "false");
      System.setProperty("prop4", "a,b,c");
      System.setProperty("prop5", "21");
      assertEquals(21, PropertyUtil.getPropertyAsInt("prop5", 0));
      assertTrue(PropertyUtil.getPropertyAsBoolean("prop2", false));
      assertFalse(PropertyUtil.getPropertyAsBoolean("prop3", true));
      assertEquals(List.of("a", "b", "c"), PropertyUtil.getPropertyAsList("prop4", List.of()));
      assertEquals("a-sys-value", PropertyUtil.getProperty("prop1", ""));
    } finally {
      System.clearProperty("prop1");
      System.clearProperty("prop2");
      System.clearProperty("prop3");
      System.clearProperty("prop4");
      System.clearProperty("prop5");
    }
  }

  @Test
  void shouldSetProperty() {
    PropertyUtil.loadPropertyResources("test1.properties");
    assertEquals("a-value", PropertyUtil.getProperty("prop1", ""));
    String previousValue = PropertyUtil.setProperty("prop1", "a-different-value");
    assertEquals("a-value", previousValue);
    assertEquals("a-different-value", PropertyUtil.getProperty("prop1", ""));
    assertNull(PropertyUtil.setProperty("a-non-existant-key", "ciao"));
    assertEquals("ciao", PropertyUtil.getProperty("a-non-existant-key", ""));
  }

  @Test
  void shouldRemoveAProperty() {
    PropertyUtil.loadPropertyResources("test1.properties");
    assertEquals("a-value", PropertyUtil.getProperty("prop1", ""));
    String prev = PropertyUtil.removeProperty("prop1");
    assertFalse(PropertyUtil.getPropertiesAsMap().containsKey("prop1"));
    assertEquals("a-value", prev);
  }

  @Test
  void shouldStripSystemsProps() {
    try {
      System.setProperty("prop1", "a-sys-value");
      PropertyUtil.loadPropertyResources("test1.properties");
      assertEquals("a-sys-value", PropertyUtil.getProperty("prop1", ""));
      assertFalse(PropertyUtil.getPropertiesAsMap().containsKey("prop1"));
    } finally {
      System.clearProperty("prop1");
    }
  }

  @Test
  void shouldValidateDupicatePropertyKeys() {
    assertThrows(IllegalArgumentException.class,
        () -> PropertyUtil.loadPropertyResources("unhappy/test1.properties"));
    PropertyUtil.loadPropertyResources("test1.properties");
    assertThrows(IllegalStateException.class,
        () -> PropertyUtil.loadPropertyResources("unhappy/test2.properties"));
  }

  @Test
  void shouldValidateDupicatePropertyKeysWithMultiLoad() {
    assertThrows(IllegalStateException.class,
        () -> PropertyUtil.loadPropertyResources("test1.properties", "unhappy/test2.properties"));
  }

  @Test
  void shouldCheckIfResourceIsDirectory() {
    assertTrue(PropertyUtil.isResourceDir("happy"));
    assertTrue(PropertyUtil.isResourceDir("happy/"));
    assertFalse(PropertyUtil.isResourceDir("not-a-directory"));
    assertFalse(PropertyUtil.isResourceDir("config.properties"));
  }

  @Test
  void shouldGetPropertiesCopy() {
    Properties properties1 = PropertyUtil.getPropertiesCopy();
    Properties properties2 = PropertyUtil.getPropertiesCopy();
    assertNotSame(properties1, properties2);

    PropertyUtil.loadAllPropertyFiles("src/test/resources/happy");
    Map<String, String> propsMap = PropertyUtil.getPropertiesAsMap();
    Properties properties = PropertyUtil.getPropertiesCopy();
    assertEquals(propsMap, properties);
    assertNotEquals(properties1, properties);
    assertNotEquals(properties2, properties);

    PropertyUtil.setProperty("a-new-test-key", "a-value");
    assertNotEquals(properties, PropertyUtil.getPropertiesAsMap());
  }
}