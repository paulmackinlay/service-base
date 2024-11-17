/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.service.support;

import com.webotech.statemachine.util.Threads;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Detects deadlocks on a scheduled basis with a configurable period
 */
public class DeadlockDetector {

  private static final Logger logger = LogManager.getLogger(DeadlockDetector.class);
  private static final DeadlockDetectTask deadlockDetectTask = new DeadlockDetectTask(
      ManagementFactory.getThreadMXBean());
  private ScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture<?> detectionFuture;

  public void startDetecting(String iso8601Period) {
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        Threads.newNamedDaemonThreadFactory("deadlock-detect",
            (t, e) -> logger.error("Uncaught exception in thread {}", t, e)));
    long periodMills = Duration.parse(iso8601Period).toMillis();
    logger.info("Will schedule deadlock detection every {} millis", periodMills);
    detectionFuture = scheduledExecutorService.scheduleAtFixedRate(deadlockDetectTask, 0,
        periodMills, TimeUnit.MILLISECONDS);
  }

  public void stopDetecting(String iso8601TerminationTimeout) {
    logger.info("Shutting down deadlock detection with timeout {}", iso8601TerminationTimeout);
    detectionFuture.cancel(true);
    scheduledExecutorService.shutdownNow();
    try {
      boolean success = scheduledExecutorService.awaitTermination(
          Duration.parse(iso8601TerminationTimeout).toMillis(), TimeUnit.MILLISECONDS);
      if (!success) {
        logger.warn("Deadlock detection executor timed out before terminating");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static class DeadlockDetectTask implements Runnable {

    private final ThreadMXBean threadMxBean;

    DeadlockDetectTask(ThreadMXBean threadMxBean) {
      this.threadMxBean = threadMxBean;
    }

    @Override
    public void run() {
      long[] deadlockedThreadIds = threadMxBean.findDeadlockedThreads();
      if (deadlockedThreadIds != null) {
        ThreadInfo[] threadInfos = threadMxBean.getThreadInfo(deadlockedThreadIds, true, true);
        if (logger.isErrorEnabled()) {
          logger.error("Deadlock detected:\n{}", threadDump(threadInfos));
        }
      }
    }

    private String threadDump(ThreadInfo[] threadInfos) {
      return Arrays.stream(threadInfos).map(Object::toString).collect(Collectors.joining());
    }
  }
}
