/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service.support;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.webotech.TestingUtil;
import com.webotech.statemachine.util.Threads;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DeadlockDetectorTest {

  private DeadlockDetector deadlockDetector;

  @BeforeEach
  void setup() {
    deadlockDetector = new DeadlockDetector();
  }

  @Test
  void shouldStartAndStopDetecting() throws InterruptedException {
    ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
    ScheduledFuture detectionFuture = mock(ScheduledFuture.class);

    when(scheduledExecutorService.scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(10000L),
        eq(TimeUnit.MILLISECONDS))).thenReturn(detectionFuture);
    try (MockedStatic<Executors> mockExecutors = Mockito.mockStatic(Executors.class)) {
      mockExecutors.when(() -> Executors.newSingleThreadScheduledExecutor(any(ThreadFactory.class)))
          .thenReturn(scheduledExecutorService);
      deadlockDetector.startDetecting("PT10S");
      verify(scheduledExecutorService, times(1)).scheduleAtFixedRate(any(Runnable.class), eq(0L),
          eq(10000L), eq(TimeUnit.MILLISECONDS));
      deadlockDetector.stopDetecting("PT0.2S");
      verify(scheduledExecutorService, times(1)).shutdownNow();
      verify(scheduledExecutorService, times(1)).awaitTermination(eq(200L),
          eq(TimeUnit.MILLISECONDS));
    }
  }

  @Test
  void detectDeadLock() throws InterruptedException, IOException {
    Object lock1 = new Object();
    Object lock2 = new Object();
    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);

    ExecutorService executor = Executors.newFixedThreadPool(2,
        Threads.newNamedDaemonThreadFactory("deadlock-task", (t, e) -> {
        }));
    executor.execute(new DeadlockTask(lock1, lock2, latch1, latch2));
    latch1.await(2, TimeUnit.SECONDS);
    executor.execute(new DeadlockTask(lock2, lock1, latch2, latch1));
    latch2.await(2, TimeUnit.SECONDS);
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      deadlockDetector.startDetecting("PT0.2S");
      TimeUnit.MILLISECONDS.sleep(300);
      String log = logStream.toString();
      assertTrue(
          log.startsWith("Will schedule deadlock detection every 200 millis\nDeadlock detected:"));
      assertTrue(log.contains("\"deadlock-task-0\""));
      assertTrue(log.contains("\"deadlock-task-1\""));
    }
  }

  private static class DeadlockTask implements Runnable {

    private final Object lock1;
    private final Object lock2;
    private final CountDownLatch latch1;
    private final CountDownLatch latch2;

    public DeadlockTask(Object lock1, Object lock2,
        CountDownLatch latch1, CountDownLatch latch2) {
      this.lock1 = lock1;
      this.lock2 = lock2;
      this.latch1 = latch1;
      this.latch2 = latch2;
    }

    @Override
    public void run() {
      try {
        synchronized (lock1) {
          latch1.countDown();
          latch2.await();
          synchronized (lock2) {
            lock2.wait();
          }
        }
      } catch (Exception e) {
        fail("Deadlock task failed due to exception");
        throw new IllegalStateException(e);
      }
    }
  }

}