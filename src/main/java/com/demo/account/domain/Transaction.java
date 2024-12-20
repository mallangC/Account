package com.demo.account.domain;

import com.demo.account.type.TransactionResultType;
import com.demo.account.type.TransactionType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Transaction extends BaseEntity {
  @Enumerated(EnumType.STRING)
  private TransactionType transactionType;
  @Enumerated(EnumType.STRING)
  private TransactionResultType transactionResultType;

  @ManyToOne
  private Account account;
  private Long amount;
  private Long balanceSnapshot;

  private String transactionId;
  private LocalDateTime transactedAt;
}
