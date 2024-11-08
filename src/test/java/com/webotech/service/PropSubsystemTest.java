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
import java.util.Map;
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
  void shouldAllPropsFromDir() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      propSubsystem.start(new TestAppContext("test", new String[]{"config=happy/"}));
      String log = logStream.toString();
      assertEquals("Loading properties\n"
              + "Loading properties in files [/home/cmacki/git/service-base/target/test-classes/happy/test1.properties]\n"
              + "Loading properties in files [/home/cmacki/git/service-base/target/test-classes/happy/test2.properties]\n"
              + "4 properties loaded\n",
          log);
      assertEquals(
          Map.of("prop3", "value3", "prop4", "value4", "prop1", "value1", "prop2", "value2"),
          PropertyUtil.getProperties());
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