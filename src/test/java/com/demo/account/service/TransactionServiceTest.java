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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.demo.account.type.AccountStatus.IN_USE;
import static com.demo.account.type.ErrorCode.*;
import static com.demo.account.type.TransactionResultType.F;
import static com.demo.account.type.TransactionResultType.S;
import static com.demo.account.type.TransactionType.CANCEL;
import static com.demo.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private AccountUserRepository accountUserRepository;

  @InjectMocks
  private TransactionService transactionService;

  @Test
  void successUseBalance() {
    //given
    AccountUser user = AccountUser.builder()
            .name("Pobi").build();
    user.setId(12L);
    Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();

    given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
    given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                    .account(account)
                    .transactionType(USE)
                    .transactionResultType(S)
                    .transactionId("transactionId")
                    .transactedAt(LocalDateTime.now())
                    .amount(1000L)
                    .balanceSnapshot(9000L)
                    .build());

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

    //when
    TransactionDto transactionDto = transactionService.useBalance(
            1L, "1000000000", 200L);
    //then
    //저장을 하는지 확인
    verify(transactionRepository, times(1)).save(captor.capture());
    //값이 맞는지 확인 -> captor에 있는건 when에 있는 값
    assertEquals(200L, captor.getValue().getAmount());
    assertEquals(9800L, captor.getValue().getBalanceSnapshot());
    assertEquals(S, transactionDto.getTransactionResultType());
    assertEquals(USE, transactionDto.getTransactionType());
    assertEquals(9000L, transactionDto.getBalanceSnapshot());
    assertEquals(1000L, transactionDto.getAmount());
  }

  @Test
  @DisplayName("해당 유저 없음 - 잔액 사용 실패")
  void useBalance_UserNotFound() {
    //given
    given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.empty());

    //when
    AccountException exception =assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1000000000", 1000L));

    //then
    assertEquals(USER_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("해당 계좌 없음 - 잔액 사용 실패")
  void useBalance_AccountNotFound() {
    //given
    AccountUser user = AccountUser.builder()
            .name("Pobi").build();
    user.setId(12L);
    given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());

    //when
    AccountException exception =assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1000000000", 1000L));

    //then
    assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
  }


  @Test
  @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
  void useBalance_userUnMatch() {
    //given
    AccountUser pobi = AccountUser.builder()
            .name("Pobi").build();
    pobi.setId(12L);
    AccountUser harry = AccountUser.builder()
            .name("Harry").build();
    harry.setId(13L);
    given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                    .accountUser(harry)
                    .balance(0L)
                    .accountNumber("1000000012").build()));

    //when
    AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1000000000", 1000L));

    //then
    assertEquals(USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
  }

  @Test
  @DisplayName("해지 계좌는 거래할 수 없다")
  void useBalance_alreadyUnregistered() {
    //given
    AccountUser pobi = AccountUser.builder()
            .name("Pobi").build();
    pobi.setId(12L);
    given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(pobi));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(Account.builder()
                    .accountUser(pobi)
                    .accountStatus(AccountStatus.UNREGISTERED)
                    .balance(0L)
                    .accountNumber("1000000012").build()));

    //when
    AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1000000000", 1000L));

    //then
    assertEquals(ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
  }

  @Test
  @DisplayName("거래 금액이 잔액보다 큰 경우")
  void useBalance_exceedAmount() {
    //given
    AccountUser user = AccountUser.builder()
            .name("Pobi").build();
    user.setId(12L);
    Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(100L)
            .accountNumber("1000000012").build();

    given(accountUserRepository.findById(anyLong()))
            .willReturn(Optional.of(user));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));

    //when
    //then
    AccountException exception = assertThrows(AccountException.class,
            () -> transactionService.useBalance(1L, "1000000000", 1000L));

    assertEquals(AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    verify(transactionRepository, times(0)).save(any());
  }

  @Test
  @DisplayName("실패 트랜잭션 저장 성공")
  void saveFailedUseTransaction() {
    //given
    AccountUser user = AccountUser.builder()
            .name("Pobi").build();
    user.setId(12L);
    Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();

    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
    given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                    .account(account)
                    .transactionType(USE)
                    .transactionResultType(S)
                    .transactionId("transactionId")
                    .transactedAt(LocalDateTime.now())
                    .amount(1000L)
                    .balanceSnapshot(9000L)
                    .build());

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

    //when
    transactionService.saveFailedUseTransaction("1000000000", 200L);
    //then
    verify(transactionRepository, times(1)).save(captor.capture());
    assertEquals(200L, captor.getValue().getAmount());
    assertEquals(10000L, captor.getValue().getBalanceSnapshot());
    assertEquals(F, captor.getValue().getTransactionResultType());
  }


  @Test
  void successCancelBalance() {
    //given
    AccountUser user = AccountUser.builder()
            .name("Pobi").build();
    user.setId(12L);
    Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();
    Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now())
            .amount(200L)
            .balanceSnapshot(9000L)
            .build();

    given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));
    given(transactionRepository.save(any()))
            .willReturn(Transaction.builder()
                    .account(account)
                    .transactionType(CANCEL)
                    .transactionResultType(S)
                    .transactionId("transactionIdForCancel")
                    .transactedAt(LocalDateTime.now())
                    .amount(200L)
                    .balanceSnapshot(10000L)
                    .build());

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

    //when
    TransactionDto transactionDto = transactionService.cancelBalance(
            "transactionId", "1000000000", 200L);
    //then
    //저장을 하는지 확인
    verify(transactionRepository, times(1)).save(captor.capture());
    assertEquals(200L, captor.getValue().getAmount());
    assertEquals(10000L + 200L, captor.getValue().getBalanceSnapshot());
    assertEquals(S, transactionDto.getTransactionResultType());
    assertEquals(CANCEL, transactionDto.getTransactionType());
    assertEquals(10000L, transactionDto.getBalanceSnapshot());
    assertEquals(200L, transactionDto.getAmount());
  }

  @Test
  @DisplayName("해당 계좌 없음 - 잔액 사용 취소 실패")
  void cancelTransaction_AccountNotFound() {
    //given

    given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(Transaction.builder()
                    .transactionType(USE)
                    .transactionResultType(S)
                    .transactionId("transactionId")
                    .transactedAt(LocalDateTime.now())
                    .amount(200L)
                    .balanceSnapshot(9000L)
                    .build()));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.empty());

    //when
    AccountException exception =assertThrows(AccountException.class,
            () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

    //then
    assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
  }


  @Test
  @DisplayName("원 사용 거래 없음 - 잔액 사용 취소 실패")
  void cancelTransaction_transactionNotFound() {
    //given
    given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.empty());

    //when
    AccountException exception =assertThrows(AccountException.class,
            () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

    //then
    assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
  }


  @Test
  @DisplayName("거래와 계좌 매칭 실패 - 잔액 사용 취소 실패")
  void cancelTransaction_transactionAccountUnMatch() {
    //given
    AccountUser user = AccountUser.builder()
            .name("Pobi").build();
    user.setId(12L);
    Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();
    account.setId(1L);
    Account accountNotUse = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();
    accountNotUse.setId(2L);
    Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now())
            .amount(200L)
            .balanceSnapshot(9000L)
            .build();
    given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(accountNotUse));

    //when
    AccountException exception =assertThrows(AccountException.class,
            () -> transactionService.cancelBalance(
                    "transactionId",
                    "1000000000",
                    200L));

    //then
    assertEquals(TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
  }

  @Test
  @DisplayName("거래금액과 취소금액이 다름 - 잔액 사용 취소 실패")
  void cancelTransaction_cancelMustFully() {
    //given
    AccountUser user = AccountUser.builder()
            .name("Pobi").build();
    user.setId(12L);
    Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();
    account.setId(1L);
    Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now())
            .amount(1200L)
            .balanceSnapshot(9000L)
            .build();
    given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));

    //when
    AccountException exception =assertThrows(AccountException.class,
            () -> transactionService.cancelBalance(
                    "transactionId",
                    "1000000000",
                    200L));

    //then
    assertEquals(CANCEL_MUST_FULLY, exception.getErrorCode());
  }


  @Test
  @DisplayName("취소는 1년까지만 가능 - 잔액 사용 취소 실패")
  void cancelTransaction_tooOldOrder() {
    //given
    AccountUser user = AccountUser.builder()
            .name("Pobi").build();
    user.setId(12L);
    Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();
    account.setId(1L);
    Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
            .amount(200L)
            .balanceSnapshot(9000L)
            .build();
    given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));
    given(accountRepository.findByAccountNumber(anyString()))
            .willReturn(Optional.of(account));

    //when
    AccountException exception =assertThrows(AccountException.class,
            () -> transactionService.cancelBalance(
                    "transactionId",
                    "1000000000",
                    200L));

    //then
    assertEquals(TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
  }

  @Test
  void successQueryTransaction() {
    //given
    AccountUser user = AccountUser.builder()
            .name("Pobi").build();
    user.setId(12L);
    Account account = Account.builder()
            .accountUser(user)
            .accountStatus(IN_USE)
            .balance(10000L)
            .accountNumber("1000000012").build();
    account.setId(1L);
    Transaction transaction = Transaction.builder()
            .account(account)
            .transactionType(USE)
            .transactionResultType(S)
            .transactionId("transactionId")
            .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
            .amount(200L)
            .balanceSnapshot(9000L)
            .build();
    given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.of(transaction));

    //when
    TransactionDto transactionDto = transactionService.queryTransaction("trxId");

    //then
    assertEquals(USE, transactionDto.getTransactionType());
    assertEquals(S, transactionDto.getTransactionResultType());
    assertEquals(200L, transactionDto.getAmount());
    assertEquals("transactionId", transactionDto.getTransactionId());
  }


  @Test
  @DisplayName("원 거래 없음 - 거래 조회 실패")
  void queryTransaction_transactionNotFound() {
    //given
    given(transactionRepository.findByTransactionId(anyString()))
            .willReturn(Optional.empty());

    //when
    AccountException exception =assertThrows(AccountException.class,
            () -> transactionService.queryTransaction("transactionId"));

    //then
    assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
  }
}