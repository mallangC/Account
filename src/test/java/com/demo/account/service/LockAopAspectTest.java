package com.demo.account.service;

import com.demo.account.dto.UseBalance;
import com.demo.account.exception.AccountException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.demo.account.type.ErrorCode.ACCOUNT_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {
  @Mock
  private LockService lockService;

  @Mock
  private ProceedingJoinPoint proceedingJoinPoint;

  @InjectMocks
  private LockAopAspect lockAopAspect;

  @Test
  void lockAndUnlock() throws Throwable {
    //given
    ArgumentCaptor<String> lockArgsCaptor =
            ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> unlockArgsCaptor =
            ArgumentCaptor.forClass(String.class);
    UseBalance.Request request = new UseBalance.Request(123L, "1234444444",1000L);

    //when
    lockAopAspect.aroundMethod(proceedingJoinPoint, request);
    //then
    verify(lockService, times(1))
            .lock(lockArgsCaptor.capture());
    verify(lockService, times(1))
            .unlock(unlockArgsCaptor.capture());
    assertEquals("1234444444", lockArgsCaptor.getValue());
    assertEquals("1234444444", unlockArgsCaptor.getValue());
  }

  @Test
  void lockAndUnlock_evenIfThrow() throws Throwable {
    //given
    ArgumentCaptor<String> lockArgsCaptor =
            ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> unlockArgsCaptor =
            ArgumentCaptor.forClass(String.class);
    UseBalance.Request request = new UseBalance.Request(123L, "54321",1000L);

    given(proceedingJoinPoint.proceed())
            .willThrow(new AccountException(ACCOUNT_NOT_FOUND));
    //when
    assertThrows(AccountException.class, () ->
            lockAopAspect.aroundMethod(proceedingJoinPoint, request));
    //then
    verify(lockService, times(1))
            .lock(lockArgsCaptor.capture());
    verify(lockService, times(1))
            .unlock(unlockArgsCaptor.capture());
    assertEquals("54321", lockArgsCaptor.getValue());
    assertEquals("54321", unlockArgsCaptor.getValue());
  }

}