package com.demo.account.controller;

import com.demo.account.domain.Account;
import com.demo.account.dto.AccountInfo;
import com.demo.account.dto.CreateAccount;
import com.demo.account.dto.DeleteAccount;
import com.demo.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AccountController {
  private final AccountService accountService;

  @PostMapping("/account")
  public CreateAccount.Response createAccount(
          @RequestBody @Valid CreateAccount.Request request
  ) {
    return CreateAccount.Response.from(
            accountService.createAccount(
            request.getUserId(),
            request.getInitialBalance()
            )
    );
  }

  @DeleteMapping("/account")
  public DeleteAccount.Response deleteAccount(
          @RequestBody @Valid DeleteAccount.Request request
  ) {
    return DeleteAccount.Response.from(
            accountService.deleteAccount(
                    request.getUserId(),
                    request.getAccountNumber()
            )
    );
  }

  @GetMapping("/account")
  public List<AccountInfo> getAccountByUserId(
          @RequestParam("user_id") Long userId
  ){
    return accountService.getAccountsByUserId(userId)
            .stream().map(accountDto ->
                    AccountInfo.builder()
                    .accountNumber(accountDto.getAccountNumber())
                    .balance(accountDto.getBalance())
                    .build())
            .collect(Collectors.toList());
  }

  @GetMapping("/account/{id}")
  public Account getAccount(@PathVariable Long id) {
    return accountService.getAccount(id);
  }
}
