package com.demo.account.exception;

import com.demo.account.type.ErrorCode;
import lombok.*;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountException extends RuntimeException{
  private ErrorCode errorCode;
  private String errorMessage;

  public AccountException(ErrorCode errorCode){
    this.errorCode = errorCode;
    this.errorMessage = errorCode.getDescription();
  }
}
