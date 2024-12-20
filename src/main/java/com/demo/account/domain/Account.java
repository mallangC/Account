package com.demo.account.domain;

import com.demo.account.exception.AccountException;
import com.demo.account.type.AccountStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.time.LocalDateTime;

import static com.demo.account.type.ErrorCode.AMOUNT_EXCEED_BALANCE;
import static com.demo.account.type.ErrorCode.INVALID_REQUEST;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity  //자동 테이블 생성
public class Account extends BaseEntity{

  @ManyToOne
  private AccountUser accountUser;
  private String accountNumber;

  @Enumerated(EnumType.STRING)
  private AccountStatus accountStatus;
  private Long balance;

  private LocalDateTime registeredAt;
  private LocalDateTime unRegisteredAt;

  public void useBalance(Long amount) {
    if (amount > this.balance) {
      throw new AccountException(AMOUNT_EXCEED_BALANCE);
    }
    this.balance -= amount;
  }

  public void cancelBalance(Long amount) {
    if (amount < 0) {
      throw new AccountException(INVALID_REQUEST);
    }
    this.balance += amount;
  }

}
