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

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2019-03-07
 */
public class DisposerUtil {
  public static <T> void add(final T element, @Nonnull final Collection<T> result, @Nonnull final Disposable parentDisposable) {
    if (result.add(element)) {
      Disposer.register(parentDisposable, new Disposable() {
        @Override
        public void dispose() {
          result.remove(element);
        }
      });
    }
  }
}
