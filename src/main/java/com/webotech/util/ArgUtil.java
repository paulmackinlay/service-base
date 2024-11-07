/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.util;

public class ArgUtil {

  private ArgUtil() {
    // Not for instanciation outside this class
  }

  /**
   * Searches through args for the first one for which everything below is true:
   * <ul>
   * <li>contains an equals sign</li>
   * <li>the text to the left of the equals sign is equal to key</li>
   * </ul>
   * then it will return the text that is to the right of the equals sign.
   * <p>
   * It can be used for extracting <b>config.properties</b> from an arg like
   * <b>config=config.properties</b>
   */
  public static String getArgValue(String[] args, String key) {
    for (String arg : args) {
      String[] kv = arg.split("=", 2);
      if (kv.length == 2 && kv[0].equals(key)) {
        return kv[1];
      }
    }
    return null;
  }
}
