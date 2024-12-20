package com.demo.account.service;

import com.demo.account.domain.Account;
import com.demo.account.domain.AccountUser;
import com.demo.account.domain.Transaction;
import com.demo.account.dto.TransactionDto;
import com.demo.account.exception.AccountException;
import com.demo.account.repository.AccountRepository;
import com.demo.account.repository.AccountUserRepository;
import com.demo.account.repository.TransactionRepository;
import com.demo.account.type.AccountStatus;
import com.demo.account.type.TransactionResultType;
import com.demo.account.type.TransactionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.demo.account.type.ErrorCode.*;
import static com.demo.account.type.TransactionResultType.F;
import static com.demo.account.type.TransactionResultType.S;
import static com.demo.account.type.TransactionType.CANCEL;
import static com.demo.account.type.TransactionType.USE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
  private final TransactionRepository transactionRepository;
  private final AccountUserRepository accountUserRepository;
  private final AccountRepository accountRepository;

  @Transactional
  public TransactionDto useBalance(Long userId, String accountNumber, Long amount){
    AccountUser user = accountUserRepository.findById(userId)
            .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
    Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    validateUseBalance(user, account, amount);

    account.useBalance(amount);

    return TransactionDto.fromEntity(saveAndGetTransaction(USE, S, account, amount));
  }

  private void validateUseBalance(AccountUser user, Account account, Long amount) {
    if (!Objects.equals(user.getId(), account.getAccountUser().getId())) {
      throw new AccountException(USER_ACCOUNT_UN_MATCH);
    }
    if (account.getAccountStatus() != AccountStatus.IN_USE){
      throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
    }
    if (account.getBalance() < amount){
      throw new AccountException(AMOUNT_EXCEED_BALANCE);
    }
  }

  @Transactional
  public void saveFailedUseTransaction(String accountNumber, Long amount) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    saveAndGetTransaction(USE, F, account, amount);
  }

  private Transaction saveAndGetTransaction(
          TransactionType transactionType,
          TransactionResultType transactionResultType,
          Account account,
          Long amount) {
    return transactionRepository.save(
            Transaction.builder()
                    .transactionType(transactionType)
                    .transactionResultType(transactionResultType)
                    .account(account)
                    .amount(amount)
                    .balanceSnapshot(account.getBalance())
                    .transactionId(UUID.randomUUID().toString().replace("-", ""))
                    .transactedAt(LocalDateTime.now())
                    .build()
    );
  }


  @Transactional
  public TransactionDto cancelBalance(
          String transactionId,
          String accountNumber,
          Long amount
  ) {
    Transaction transaction = transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));
    Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    validateCancelBalance(transaction, account, amount);

    account.cancelBalance(amount);

    return TransactionDto.fromEntity(
            saveAndGetTransaction(CANCEL, S, account, amount)
    );
  }


  private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
    if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
      throw new AccountException(TRANSACTION_ACCOUNT_UN_MATCH);
    }
    if (!Objects.equals(transaction.getAmount(), amount)){
      throw new AccountException(CANCEL_MUST_FULLY);
    }
    if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))){
      throw new AccountException(TOO_OLD_ORDER_TO_CANCEL);
    }
  }


  @Transactional
  public void saveFailedCancelTransaction(String accountNumber, Long amount) {
    Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

    saveAndGetTransaction(CANCEL, F, account, amount);
  }

  public TransactionDto queryTransaction(String transactionId) {
    return TransactionDto.fromEntity(transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND)));
  }
}
