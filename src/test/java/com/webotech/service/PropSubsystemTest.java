/*
 * Copyright (c) 2024-2025 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.webotech.TestingUtil;
import com.webotech.util.PropertyUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropSubsystemTest {

  private PropSubsystem<TestAppContext> propSubsystem;

  @BeforeEach
  void setup() {
    PropertyUtil.getPropertyAsBoolean("any", false);
    propSubsystem = new PropSubsystem<>(new String[0], false);
    System.getProperties().stringPropertyNames().forEach(System::clearProperty);
    PropSubsystem.reset();
  }

  @Test
  void shouldLoadPropsAndLogThemWithExclusions() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      System.setProperty("config", "test3.properties");
      propSubsystem.start(new TestAppContext("test", new String[0]));
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("""
          Loading properties
          Loading properties from resource [test3.properties]
          8 properties loaded
          com.webotech.service.PropSubsystem.excludePropLogForKeysContainingCsv=secret,password,passwd,credentials
          com.webotech.service.PropSubsystem.logPropValuesAfterLoad=true
          db.passwd=***
          prop1=a-value
          prop2=true
          prop3=false
          prop4=one,two,three
          prop5=23
          """, log);
      assertEquals(List.of("one", "two", "three"),
          PropertyUtil.getPropertyAsList("prop4", List.of()));
    } finally {
      System.clearProperty("config");
    }
  }

  @Test
  void shouldLoadPropsAndLogThemWithExclusionsImmediately() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      System.setProperty("config", "test3.properties");
      new PropSubsystem<>(new String[0]);
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("""
          Loading properties
          Loading properties from resource [test3.properties]
          8 properties loaded
          com.webotech.service.PropSubsystem.excludePropLogForKeysContainingCsv=secret,password,passwd,credentials
          com.webotech.service.PropSubsystem.logPropValuesAfterLoad=true
          db.passwd=***
          prop1=a-value
          prop2=true
          prop3=false
          prop4=one,two,three
          prop5=23
          """, log);
      assertEquals(List.of("one", "two", "three"),
          PropertyUtil.getPropertyAsList("prop4", List.of()));
    } finally {
      System.clearProperty("config");
    }
  }

  @Test
  void shouldLoadNoPropsUsingSystemKey() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      System.setProperty("config", "a-non-existant-prop-file.properties");
      propSubsystem.start(new TestAppContext("test", new String[0]));
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("""
          Loading properties
          Loading properties from resource [a-non-existant-prop-file.properties]
          Properties stream does not exist
          0 properties loaded
          """, log);
    } finally {
      System.clearProperty("config");
    }
  }

  @Test
  void shouldLoadNoPropsUsingSystemKeyImmediately() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      System.setProperty("config", "a-non-existant-prop-file.properties");
      new PropSubsystem<TestAppContext>(new String[0]);
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("""
          Loading properties
          Loading properties from resource [a-non-existant-prop-file.properties]
          Properties stream does not exist
          0 properties loaded
          """, log);
    } finally {
      System.clearProperty("config");
    }
  }

  @Test
  void shouldLoadStandardProps() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      propSubsystem.start(new TestAppContext("test", new String[0]));
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("""
          Loading properties
          Loading properties from resource [config.properties]
          1 properties loaded
          key=ok
          """, log);
    }
  }

  @Test
  void shouldLoadStandardPropsImmediately() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      new PropSubsystem<TestAppContext>(new String[0]);
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("""
          Loading properties
          Loading properties from resource [config.properties]
          1 properties loaded
          key=ok
          """, log);
    }
  }

  @Test
  void shouldLoadAllPropsFromDir() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      propSubsystem.start(new TestAppContext("test", new String[]{"config=happy/"}));
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertTrue(log.startsWith("Loading properties\nLoading properties in files "));
      assertTrue(log.contains("]\n4 properties loaded\n"));
      assertTrue(log.contains("happy/test1.properties"));
      assertTrue(log.contains("happy/test2.properties"));
      assertEquals(
          Map.of("prop3", "value3", "prop4", "value4", "prop1", "value1", "prop2", "value2"),
          PropertyUtil.getPropertiesAsMap());
      assertTrue(log.contains("""
          prop1=value1
          prop2=value2
          prop3=value3
          prop4=value4
          """));
    }
  }

  @Test
  void shouldLoadAllPropsFromDirImmediately() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      new PropSubsystem<TestAppContext>(new String[]{"config=happy/"});
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertTrue(log.startsWith("Loading properties\nLoading properties in files "));
      assertTrue(log.contains("]\n4 properties loaded\n"));
      assertTrue(log.contains("happy/test1.properties"));
      assertTrue(log.contains("happy/test2.properties"));
      assertEquals(
          Map.of("prop3", "value3", "prop4", "value4", "prop1", "value1", "prop2", "value2"),
          PropertyUtil.getPropertiesAsMap());
      assertTrue(log.contains("""
          prop1=value1
          prop2=value2
          prop3=value3
          prop4=value4
          """));
    }
  }

  @Test
  void shouldMatchSinglePropertyFile() {
    Matcher matcher = PropSubsystem.propPattern.matcher("config.properties");
    assertMatches(matcher, true, 1, "config.properties");
    matcher = PropSubsystem.propPattern.matcher("dir/config.properties");
    assertMatches(matcher, true, 1, "dir/config.properties");
    matcher = PropSubsystem.propPattern.matcher("config1.properties,config2.properties");
    assertMatches(matcher, false, 0);
  }

  @Test
  void shouldParse() {
    List<String> parsed = PropSubsystem.parse("config.properties");
    assertEquals(List.of("config.properties"), parsed);
    parsed = PropSubsystem.parse("config1.properties,config2.properties");
    assertEquals(List.of("config1.properties", "config2.properties"), parsed);
    parsed = PropSubsystem.parse("dir/config.properties");
    assertEquals(List.of("dir/config.properties"), parsed);
    parsed = PropSubsystem.parse("dir/config1.properties,dir/config2.properties");
    assertEquals(List.of("dir/config1.properties", "dir/config2.properties"), parsed);
  }

  @Test
  void shouldLoadAndUnloadProps() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      TestAppContext appContext = new TestAppContext("test", new String[0]);
      propSubsystem.start(appContext);
      assertEquals("ok", PropertyUtil.getProperty("key", ""));
      propSubsystem.stop(appContext);
      assertEquals("", PropertyUtil.getProperty("key", ""));
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("""
          Loading properties
          Loading properties from resource [config.properties]
          1 properties loaded
          key=ok
          Unloading properties
          Property with key [key] has been removed
          """, log);
    }
  }

  @Test
  void shouldNotUnloadImmediatelyInitProps() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      TestAppContext appContext = new TestAppContext("test", new String[0]);
      PropSubsystem<TestAppContext> subsystem = new PropSubsystem<>(new String[0]);
      assertEquals("ok", PropertyUtil.getProperty("key", ""));
      subsystem.start(appContext);
      subsystem.stop(appContext);
      assertEquals("ok", PropertyUtil.getProperty("key", ""));
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("""
          Loading properties
          Loading properties from resource [config.properties]
          1 properties loaded
          key=ok
          """, log);
    }
  }

  @Test
  void shouldNotUnloadExternallyInitProps() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      PropSubsystem.initProps(new String[0]);
      TestAppContext appContext = new TestAppContext("test", new String[0]);
      PropSubsystem<TestAppContext> subsystem = new PropSubsystem<>(new String[0]);
      assertEquals("ok", PropertyUtil.getProperty("key", ""));
      subsystem.start(appContext);
      subsystem.stop(appContext);
      assertEquals("ok", PropertyUtil.getProperty("key", ""));
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertEquals("""
          Loading properties
          Loading properties from resource [config.properties]
          1 properties loaded
          key=ok
          """, log);
    }
  }

  private void assertMatches(Matcher matcher, boolean isMatch, int noGroups, String... groups) {
    boolean isRealMatch = matcher.matches();
    int noRealGroups = matcher.groupCount();
    if (isMatch) {
      assertTrue(isRealMatch);
      assertEquals(noGroups, noRealGroups);
      assertEquals(noGroups, groups.length);
      int i = 1;
      for (String group : groups) {
        String realGroup = matcher.group(i++);
        assertEquals(realGroup, group);
      }
    } else {
      assertFalse(isRealMatch);
    }
  }
}