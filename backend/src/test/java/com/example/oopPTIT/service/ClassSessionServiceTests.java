package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.CreateClassSessionRequest;
import com.example.oopPTIT.entity.ClassSession;
import com.example.oopPTIT.entity.CourseClass;
import com.example.oopPTIT.repository.ClassSessionRepository;
import com.example.oopPTIT.repository.CourseClassRepository;
import com.example.oopPTIT.security.CourseAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTests {
    @Mock private ClassSessionRepository sessions;
    @Mock private CourseClassRepository classes;
    @Mock private CourseAccess courseAccess;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private ClassSessionService service;
    private CourseClass courseClass;

    @BeforeEach
    void setUp() {
        courseClass = new CourseClass();
        courseClass.setId(10L);
        when(classes.findByIdForUpdate(10L)).thenReturn(Optional.of(courseClass));
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
    }

    @Test
    void retriesClassNumberCollisionInANewTransaction() {
        when(sessions.findMaxSessionNoByCourseClassId(10L)).thenReturn(2, 3);
        when(sessions.saveAndFlush(any(ClassSession.class)))
                .thenThrow(new DataIntegrityViolationException("Concurrent session number"))
                .thenAnswer(invocation -> {
                    ClassSession session = invocation.getArgument(0);
                    session.setId(20L);
                    return session;
                });

        var result = service.createSession(10L, request());

        assertThat(result.sessionNo()).isEqualTo(4);
        assertThat(result.classId()).isEqualTo(10L);
        var saved = ArgumentCaptor.forClass(ClassSession.class);
        verify(sessions, times(2)).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues()).extracting(ClassSession::getSessionNo).containsExactly(3, 4);
        verify(transactionManager, times(2)).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW
                        && definition.getIsolationLevel() == TransactionDefinition.ISOLATION_READ_COMMITTED));
        verify(transactionManager).rollback(any());
        verify(transactionManager).commit(any());
        verify(courseAccess, times(2)).requireAssignedLecturer(courseClass);
    }

    @Test
    void allocationRetriesAreBoundedAndReturnConflict() {
        when(sessions.findMaxSessionNoByCourseClassId(10L)).thenReturn(2);
        when(sessions.saveAndFlush(any(ClassSession.class)))
                .thenThrow(new DataIntegrityViolationException("Concurrent session number"));

        assertThatThrownBy(() -> service.createSession(10L, request()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(sessions, times(3)).saveAndFlush(any());
        verify(transactionManager, times(3)).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void retriesTransientLockFailureBeforeCreatingSession() {
        when(classes.findByIdForUpdate(10L)).thenThrow(new CannotAcquireLockException("Concurrent writer"))
                .thenReturn(Optional.of(courseClass));
        when(sessions.findMaxSessionNoByCourseClassId(10L)).thenReturn(0);
        when(sessions.saveAndFlush(any(ClassSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.createSession(10L, request()).sessionNo()).isEqualTo(1);

        verify(classes, times(2)).findByIdForUpdate(10L);
        verify(sessions).saveAndFlush(any());
        verify(transactionManager).rollback(any());
        verify(transactionManager).commit(any());
    }

    private CreateClassSessionRequest request() {
        return new CreateClassSessionRequest(LocalDate.of(2026, 10, 28), LocalTime.of(9, 0), LocalTime.of(11, 0));
    }
}
