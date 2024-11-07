/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ArgUtilTest {

  private static final String[] ARGS = new String[]{"arg1=value1", "arg2"};

  @Test
  void shouldGetArgValue() {
    assertEquals("value1", ArgUtil.getArgValue(ARGS, "arg1"));
    assertNull(ArgUtil.getArgValue(ARGS, "arg2"));
    assertNull(ArgUtil.getArgValue(ARGS, "a-non-existant-arg"));
  }

}