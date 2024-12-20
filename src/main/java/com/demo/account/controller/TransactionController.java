package com.demo.account.controller;

import com.demo.account.aop.AccountLock;
import com.demo.account.dto.CancelBalance;
import com.demo.account.dto.QueryTransactionResponse;
import com.demo.account.dto.UseBalance;
import com.demo.account.exception.AccountException;
import com.demo.account.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔핵 사용 취소
 * 3. 거래 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
  private final TransactionService transactionService;

  @PostMapping("/transaction/use")
  @AccountLock
  public UseBalance.Response useBalance(
          @Valid @RequestBody UseBalance.Request request
  ) {
    try {
      return UseBalance.Response.from(
              transactionService.useBalance(request.getUserId(),
                      request.getAccountNumber(), request.getAmount())
      );
    }catch (AccountException e){
      log.error("failed to use balance.");
      transactionService.saveFailedUseTransaction(
              request.getAccountNumber(),
              request.getAmount()
      );

      throw e;
    }
  }

  @PostMapping("/transaction/cancel")
  @AccountLock
  public CancelBalance.Response cancelBalance(
          @Valid @RequestBody CancelBalance.Request request
  ){
    try {
      return CancelBalance.Response.from(
              transactionService.cancelBalance(request.getTransactionId(),
                      request.getAccountNumber(), request.getAmount())
      );
    }catch (AccountException e){
      log.error("failed to cancel balance.");

      transactionService.saveFailedCancelTransaction(
              request.getAccountNumber(),
              request.getAmount()

      );

      throw e;
    }
  }

  @GetMapping("/transaction/{transactionId}")
  public QueryTransactionResponse querryTransaction(
          @PathVariable String transactionId
  ){
    return QueryTransactionResponse.from(
            transactionService.queryTransaction(transactionId)
    );
  }

}
