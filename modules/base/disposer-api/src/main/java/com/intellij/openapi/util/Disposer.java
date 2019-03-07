/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.openapi.util;

import com.intellij.openapi.Disposable;
import consulo.disposer.internal.DisposerInternal;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Disposer {
  private static final DisposerInternal ourInternal = DisposerInternal.ourInstance;

  public static TraceableDisposable newTraceDisposable(boolean debug) {
    return ourInternal.newTraceDisposable(debug);
  }

  @Nonnull
  public static Disposable newDisposable() {
    return newDisposable(null);
  }

  @Nonnull
  public static Disposable newDisposable(@Nullable final String debugName) {
    return new Disposable() {
      @Override
      public void dispose() {
      }

      @Override
      public String toString() {
        return debugName == null ? super.toString() : debugName;
      }
    };
  }

  @Nullable
  public static Disposable get(@Nonnull String key) {
    return ourInternal.get(key);
  }

  public static void register(@Nonnull Disposable parent, @Nonnull Disposable child) {
    register(parent, child, null);
  }

  public static void register(@Nonnull Disposable parent, @Nonnull Disposable child, @NonNls @Nullable final String key) {
    ourInternal.register(parent, child, key);
  }

  public static boolean isDisposed(@Nonnull Disposable disposable) {
    return ourInternal.isDisposed(disposable);
  }

  public static void dispose(@Nonnull Disposable disposable) {
    dispose(disposable, true);
  }

  public static void dispose(@Nonnull Disposable disposable, boolean processUnregistered) {
    ourInternal.dispose(disposable, processUnregistered);
  }

  public static void disposeChildAndReplace(@Nonnull Disposable toDispose, @Nonnull Disposable toReplace) {
    ourInternal.disposeChildAndReplace(toDispose, toReplace);
  }

  public static boolean isDebugMode() {
    return ourInternal.isDebugMode();
  }

  public static boolean setDebugMode(boolean mode) {
    return ourInternal.setDebugMode(mode);
  }

  public static void assertIsEmpty() {
    ourInternal.assertIsEmpty();
  }
}
