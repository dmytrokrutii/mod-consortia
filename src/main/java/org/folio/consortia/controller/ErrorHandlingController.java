package org.folio.consortia.controller;

import org.folio.consortia.domain.dto.Errors;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.folio.consortia.utils.ErrorHelper.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.consortia.utils.ErrorHelper.ErrorCode.VALIDATION_ERROR;
import static org.folio.consortia.utils.ErrorHelper.createInternalError;

@RestControllerAdvice
public class ErrorHandlingController {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(ResourceNotFoundException.class)
  public Errors handleNotFoundException(ResourceNotFoundException e) {
    return createInternalError(e.getMessage(), NOT_FOUND_ERROR);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentNotValidException.class,
    IllegalArgumentException.class
  })
  public Errors handleIllegalArgumentException(Exception e) {
    return createInternalError(e.getMessage(), VALIDATION_ERROR);
  }

}
