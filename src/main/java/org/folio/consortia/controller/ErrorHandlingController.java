package org.folio.consortia.controller;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.consortia.utils.ErrorHelper.ErrorCode.*;
import static org.folio.consortia.utils.ErrorHelper.createExternalError;
import static org.folio.consortia.utils.ErrorHelper.createInternalError;
import static org.folio.consortia.utils.ErrorHelper.createPermissionError;

import java.util.List;
import java.util.Objects;

import feign.FeignException;
import org.folio.consortia.domain.dto.Error;
import org.folio.consortia.domain.dto.Errors;
import org.folio.consortia.exception.ConsortiumClientException;
import org.folio.consortia.exception.PrimaryAffiliationException;
import org.folio.consortia.exception.ResourceAlreadyExistException;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.utils.ErrorHelper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class ErrorHandlingController {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(ResourceNotFoundException.class)
  public Errors handleNotFoundException(ResourceNotFoundException e) {
    return createExternalError(e.getMessage(), NOT_FOUND_ERROR);
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler(ResourceAlreadyExistException.class)
  public Errors handleResourceAlreadyExistException(ResourceAlreadyExistException e) {
    return createExternalError(e.getMessage(), DUPLICATE_ERROR);
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler({DataIntegrityViolationException.class})
  public Errors handleDataIntegrityViolationException(DataIntegrityViolationException e) {
    log.error("Handle data integrity violation", e);

    /*
    org.springframework.dao.DataIntegrityViolationException :-
    this is a generic data exception typically thrown by the Spring exception translation mechanism when dealing with lower level persistence exceptions.
    So to get clear error message we need to find rootCause first.
    */
    return createExternalError(Objects.requireNonNull(e.getRootCause()).getMessage(), VALIDATION_ERROR);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class
  })
  public Errors handleValidationErrors(Exception e) {
    log.error("Handle validation errors", e);
    return createExternalError(e.getMessage(), VALIDATION_ERROR);
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(PrimaryAffiliationException.class)
  public Errors handlePrimaryAffiliationException(Exception e) {
    return createExternalError(e.getMessage(), HAS_PRIMARY_AFFILIATION_ERROR);
  }

  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler(ConsortiumClientException.class)
  public Errors handleConsortiumClientException(FeignException e) {
    log.error("Handle consortium client exception", e);
    return createPermissionError(e, PERMISSION_REQUIRED);
  }

  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  @ExceptionHandler(IllegalStateException.class)
  public Errors handleIllegalStateException(IllegalStateException e) {
    log.error("Handle illegal state exception", e);
    return createInternalError(e.getMessage(), BAD_GATEWAY);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public Errors handleConstraintViolation(ConstraintViolationException ex) {
    log.error("Handle constraint violation", ex);

    List<Error> errorList = ex.getConstraintViolations()
      .stream()
      .map(violation -> {
        var customCode = (violation.getRootBeanClass() != null ? violation.getRootBeanClass().getSimpleName() : EMPTY) + "ValidationError";
        return new Error()
          // Extract the error message and validation errors from the ConstraintViolationException
          .message(String.format("'%s' validation failed. %s", violation.getPropertyPath(), violation.getMessage()))
          .type(ErrorHelper.ErrorType.EXTERNAL.getTypeCode())
          .code(customCode);
      })
      .toList();

    return new Errors().errors(errorList);
  }
}
