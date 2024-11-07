/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
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
import java.util.regex.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropSubsystemTest {

  private PropSubsystem<TestAppContext> propSubsystem;

  @BeforeEach
  void setup() {
    PropertyUtil.getPropertyAsBoolean("any", false);
    propSubsystem = new PropSubsystem<>();
    System.getProperties().keySet().stream().forEach(k -> System.clearProperty(String.valueOf(k)));
    PropertyUtil.getProperties().keySet().stream().forEach(k -> PropertyUtil.removeProperty(k));
  }

  @Test
  void shouldLoadPropsAndLogThemWithExclusions() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      System.setProperty("config", "test3.properties");
      propSubsystem.start(new TestAppContext("test", new String[0]));
      String log = logStream.toString();
      assertEquals("Loading properties\n"
              + "Loading properties from resource [test3.properties]\n"
              + "8 properties loaded\n"
              + "com.webotech.service.PropSubsystem.excludePropLogForKeysContainingCsv=secret,password,passwd,credentials\n"
              + "com.webotech.service.PropSubsystem.logPropValuesAfterLoad=true\n"
              + "db.passwd=***\n"
              + "prop1=a-value\n"
              + "prop2=true\n"
              + "prop3=false\n"
              + "prop4=one,two,three\n"
              + "prop5=23\n",
          log);
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
      String log = logStream.toString();
      assertEquals("Loading properties\n"
              + "Loading properties from resource [a-non-existant-prop-file.properties]\n"
              + "Properties stream does not exist\n"
              + "0 properties loaded\n",
          log);
    } finally {
      System.clearProperty("config");
    }
  }

  @Test
  void shouldLoadNoProps() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      propSubsystem.start(new TestAppContext("test", new String[0]));
      String log = logStream.toString();
      assertEquals("Loading properties\n"
              + "Loading properties from resource [config.properties]\n"
              + "0 properties loaded\n",
          log);
    }
  }

  @Test
  void shouldMatchSinglePropertyFile() {
    Matcher matcher = PropSubsystem.argPattern.matcher("config=config.properties");
    assertMatches(matcher, true, 1, "config.properties");
    matcher = PropSubsystem.argPattern.matcher("config=dir/config.properties");
    assertMatches(matcher, true, 1, "dir/config.properties");
    matcher = PropSubsystem.argPattern.matcher("config=config1.properties,config2.properties");
    assertMatches(matcher, false, 0);
  }

  @Test
  void shouldMatchMultiplePropertyFiles() {
    Matcher matcher = PropSubsystem.csvArgPattern.matcher(
        "config=config1.properties,config2.properties");
    assertMatches(matcher, true, 2, "config1.properties", "config2.properties");
    matcher = PropSubsystem.csvArgPattern.matcher(
        "config=dir/config1.properties,dir/config2.properties");
    assertMatches(matcher, true, 2, "dir/config1.properties", "dir/config2.properties");
    matcher = PropSubsystem.csvArgPattern.matcher("config=config1.properties");
    assertMatches(matcher, false, 0);
  }

  @Test
  void shouldParseSysProp() {
    List<String> parsed = PropSubsystem.parse("config.properties", true);
    assertEquals(List.of("config.properties"), parsed);
    parsed = PropSubsystem.parse("config1.properties,config2.properties", true);
    assertEquals(List.of("config1.properties", "config2.properties"), parsed);
    parsed = PropSubsystem.parse("dir/config.properties", true);
    assertEquals(List.of("dir/config.properties"), parsed);
    parsed = PropSubsystem.parse("dir/config1.properties,dir/config2.properties", true);
    assertEquals(List.of("dir/config1.properties", "dir/config2.properties"), parsed);
  }


  @Test
  void shouldParseArgs() {
    List<String> parsed = PropSubsystem.parse("config=config.properties", false);
    assertEquals(List.of("config.properties"), parsed);
    parsed = PropSubsystem.parse("config=config1.properties,config2.properties", false);
    assertEquals(List.of("config1.properties", "config2.properties"), parsed);
    parsed = PropSubsystem.parse("config=dir/config.properties", false);
    assertEquals(List.of("dir/config.properties"), parsed);
    parsed = PropSubsystem.parse("config=dir/config1.properties,dir/config2.properties", false);
    assertEquals(List.of("dir/config1.properties", "dir/config2.properties"), parsed);
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