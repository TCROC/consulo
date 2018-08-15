/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.diagnostic;

import com.intellij.idea.ApplicationStarter;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ExceptionWithAttachments;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.util.ExceptionUtil;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike
 */
public class Log4JDialogAppender extends AppenderSkeleton {
  private static final int MAX_ASYNC_LOGGING_EVENTS = 5;

  private volatile Runnable myDialogRunnable = null;
  private final AtomicInteger myPendingAppendCounts = new AtomicInteger();

  @Override
  protected synchronized void append(@Nonnull final LoggingEvent event) {
    if (!event.getLevel().isGreaterOrEqual(Level.ERROR) || !ApplicationStarter.isLoaded()) {
      return;
    }

    if (myPendingAppendCounts.addAndGet(1) > MAX_ASYNC_LOGGING_EVENTS) {
      // Stop adding requests to the queue or we can get OOME on pending logging requests (IDEA-95327)
      myPendingAppendCounts.decrementAndGet(); // number of pending logging events should not increase
    }
    else {
      // Note, we MUST avoid SYNCHRONOUS invokeAndWait to prevent deadlocks
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        try {
          appendToLoggers(event);
        }
        finally {
          myPendingAppendCounts.decrementAndGet();
        }
      });
    }
  }

  void appendToLoggers(@Nonnull LoggingEvent event) {
    if (myDialogRunnable != null) {
      return;
    }

    final IdeaLoggingEvent ideaEvent;
    final Object message = event.getMessage();
    if (message instanceof IdeaLoggingEvent) {
      ideaEvent = (IdeaLoggingEvent)message;
    }
    else {
      ThrowableInformation info = event.getThrowableInformation();
      if (info == null) {
        return;
      }
      ideaEvent = extractLoggingEvent(message, info.getThrowable());
    }

    final DefaultIdeaErrorLogger logger = DefaultIdeaErrorLogger.INSTANCE;
    if (!logger.canHandle(ideaEvent)) {
      return;
    }
    myDialogRunnable = () -> {
      try {
        logger.handle(ideaEvent);
      }
      finally {
        myDialogRunnable = null;
      }
    };

    final Application app = ApplicationManager.getApplication();
    if (app == null) {
      new Thread(myDialogRunnable).start();
    }
    else {
      app.executeOnPooledThread(myDialogRunnable);
    }
  }

  private static IdeaLoggingEvent extractLoggingEvent(Object message, Throwable throwable) {
    //noinspection ThrowableResultOfMethodCallIgnored
    Throwable rootCause = ExceptionUtil.getRootCause(throwable);
    if (rootCause instanceof LogEventException) {
      return ((LogEventException)rootCause).getLogMessage();
    }

    String strMessage = message == null ? "<null> " : message.toString();
    ExceptionWithAttachments withAttachments = ExceptionUtil.findCause(throwable, ExceptionWithAttachments.class);
    if (withAttachments != null) {
      return LogMessageEx.createEvent(strMessage, ExceptionUtil.getThrowableText(throwable), withAttachments.getAttachments());
    }

    return new IdeaLoggingEvent(strMessage, throwable);
  }

  @TestOnly
  Runnable getDialogRunnable() {
    return myDialogRunnable;
  }

  @Override
  public boolean requiresLayout() {
    return false;
  }

  @Override
  public void close() {
  }
}

