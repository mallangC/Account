package com.demo.account.dto;

import com.demo.account.type.ErrorCode;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
  private ErrorCode errorCode;
  private String errorMessage;
}
