package com.demo.account.exception;

import com.demo.account.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.demo.account.type.ErrorCode.INTERNAL_SERVER_ERROR;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccountException.class)
  public ErrorResponse handleAccountException(AccountException e) {
    log.error("{} is occurred.",e.getErrorCode());

    return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
  }

  @ExceptionHandler(Exception.class)
  public ErrorResponse handleException(Exception e) {
    log.error("Exception is occurred.",e);

    return new ErrorResponse(
            INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR.getDescription()
    );
  }
}
