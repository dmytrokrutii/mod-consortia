package org.folio.consortia.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.folio.consortia.domain.dto.Error;
import org.folio.consortia.domain.dto.Errors;
import org.folio.consortia.exception.ConsortiumClientException;
import org.folio.consortia.exception.PrimaryAffiliationException;
import org.folio.consortia.exception.ResourceAlreadyExistException;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.folio.consortia.utils.ErrorHelper.ErrorCode.DUPLICATE_ERROR;
import static org.folio.consortia.utils.ErrorHelper.ErrorCode.NOT_FOUND_ERROR;
import static org.folio.consortia.utils.ErrorHelper.ErrorCode.VALIDATION_ERROR;
import static org.folio.consortia.utils.ErrorHelper.createInternalError;

@RestControllerAdvice
@Log4j2
public class ErrorHandlingController {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(ResourceNotFoundException.class)
  public Errors handleNotFoundException(ResourceNotFoundException e) {
    return createInternalError(e.getMessage(), NOT_FOUND_ERROR);
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler(ResourceAlreadyExistException.class)
  public Errors handleResourceAlreadyExistException(ResourceAlreadyExistException e) {
    return createInternalError(e.getMessage(), DUPLICATE_ERROR);
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler(DataIntegrityViolationException.class)
  public Errors handleDataIntegrityViolationException(DataIntegrityViolationException e) {
    log.error("Handle data integrity violation", e);

    /*
    org.springframework.dao.DataIntegrityViolationException :-
    this is a generic data exception typically thrown by the Spring exception translation mechanism when dealing with lower level persistence exceptions.
    So to get clear error message we need to find rootCause first.
    */
    return createInternalError(Objects.requireNonNull(e.getRootCause()).getMessage(), VALIDATION_ERROR);
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler(IllegalStateException.class)
  public Errors handleIllegalStateException(IllegalStateException e) {
    log.error("Handle illegal state", e);
    return createInternalError(e.getMessage(), VALIDATION_ERROR);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
    ConsortiumClientException.class,
    PrimaryAffiliationException.class
  })
  public Errors handleValidationErrors(Exception e) {
    log.error("Handle validation errors", e);
    return createInternalError(e.getMessage(), VALIDATION_ERROR);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<List<Error>> handleConstraintViolation(ConstraintViolationException ex) {
    log.error("Handle constraint violation", ex);

    // Extract the error message and validation errors from the ConstraintViolationException
    List<String> validationErrors = ex.getConstraintViolations().stream()
      .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
      .toList();

    // Create an Error object containing the error message and validation errors
    List<Error> errorList = new ArrayList<>();
    for (String validationError : validationErrors) {
      var error = new Error();
      error.setMessage(validationError);
      error.setType("-1");
      error.setCode(String.valueOf(VALIDATION_ERROR));
      errorList.add(error);
    }
    Errors errors = new Errors();
    errors.setErrors(errorList);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors.getErrors());
  }
}
