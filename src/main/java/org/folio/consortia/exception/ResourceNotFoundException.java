package org.folio.consortia.exception;

public class ResourceNotFoundException extends RuntimeException {

  public static final String NOT_FOUND_MSG_TEMPLATE = "Objects with %s [%s] not found";

  public ResourceNotFoundException(String attribute, String value) {
    super(String.format(NOT_FOUND_MSG_TEMPLATE, attribute, value));
  }

  public ResourceNotFoundException(String errorMsg) {
    super(errorMsg);
  }
}
