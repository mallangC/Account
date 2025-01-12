package com.demo.account.service;

import com.demo.account.domain.Account;
import com.demo.account.domain.AccountUser;
import com.demo.account.dto.AccountDto;
import com.demo.account.exception.AccountException;
import com.demo.account.repository.AccountRepository;
import com.demo.account.repository.AccountUserRepository;
import com.demo.account.type.AccountStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.demo.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
  private final AccountRepository accountRepository;
  private final AccountUserRepository accountUserRepository;

  /**
   *사용자가 있는지 조회
   *계좌의 번호를 생성하고
   *계좌를 저장하고, 그 정보를 넘긴다
   */
  @Transactional
  public AccountDto createAccount(Long userId, Long initialBalance) {
    AccountUser accountUser = getAccountUser(userId);

    validateCreateAccount(accountUser);

    String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
            .map(account ->(Integer.parseInt(account.getAccountNumber())) + 1 + "")
            .orElse("1000000000");

    return AccountDto.fromEntity(accountRepository.save(
            Account.builder()
                    .accountUser(accountUser)
                    .accountStatus(AccountStatus.IN_USE)
                    .accountNumber(newAccountNumber)
                    .balance(initialBalance)
                    .registeredAt(LocalDateTime.now())
                    .build())
    );
  }

  private void validateCreateAccount(AccountUser accountUser) {
    if (accountRepository.countByAccountUser(accountUser) == 10) {
      throw new AccountException(MAX_ACCOUNT_PER_USER_10);
    }
  }

  @Transactional
  public Account getAccount(Long id) {
    return accountRepository.findById(id).get();
  }

  @Transactional
  public AccountDto deleteAccount(Long userId, String accountNumber) {
    AccountUser accountUser = getAccountUser(userId);
    Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    validateDeleteAccount(accountUser, account);

    account.setAccountStatus(AccountStatus.UNREGISTERED);
    account.setUnRegisteredAt(LocalDateTime.now());

    accountRepository.save(account);

    return AccountDto.fromEntity(account);
  }

  private void validateDeleteAccount(AccountUser accountUser, Account account) {
    if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())){
      throw new AccountException(USER_ACCOUNT_UN_MATCH);
    }
    if (account.getAccountStatus() == AccountStatus.UNREGISTERED){
      throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
    }
    if (account.getBalance() > 0){
      throw new AccountException(BALANCE_NOT_EMPTY);
    }
  }

  @Transactional
  public List<AccountDto> getAccountsByUserId(Long userId) {
    AccountUser accountUser = getAccountUser(userId);
    List<Account> accounts = accountRepository.findByAccountUser(accountUser);

    return accounts.stream()
            .map(AccountDto::fromEntity)
            .collect(Collectors.toList());
  }

  private AccountUser getAccountUser(Long userId) {
    AccountUser accountUser = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
    return accountUser;
  }
}
