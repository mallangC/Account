package com.demo.account.dto;

import com.demo.account.domain.Account;
import com.demo.account.domain.Transaction;
import com.demo.account.type.TransactionResultType;
import com.demo.account.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
  private String accountNumber;
  private TransactionType transactionType;
  private TransactionResultType transactionResultType;
  private Account account;
  private Long amount;
  private Long balanceSnapshot;
  private String transactionId;
  private LocalDateTime transactedAt;

  public static TransactionDto fromEntity(Transaction transaction) {
    return TransactionDto.builder()
            .accountNumber(transaction.getAccount().getAccountNumber())
            .transactionType(transaction.getTransactionType())
            .transactionResultType(transaction.getTransactionResultType())
            .amount(transaction.getAmount())
            .balanceSnapshot(transaction.getBalanceSnapshot())
            .transactionId(transaction.getTransactionId())
            .transactedAt(transaction.getTransactedAt())
            .build();
  }
}
