package org.folio.consortia.utils;

import org.apache.commons.lang3.StringUtils;

public class HelperUtils {

  private HelperUtils() {}

  public static void checkIdenticalOrThrow(String firstString, String secondString, String errorMsg) {
    if (!StringUtils.equals(firstString, secondString)) {
      throw new IllegalArgumentException(errorMsg);
    }
  }
}
