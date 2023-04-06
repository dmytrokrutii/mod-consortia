package org.folio.consortia.exception;

public class ConsortiumClientException extends RuntimeException {

  public ConsortiumClientException(String msg, Exception exception) {
    super(msg, exception);
  }
}
