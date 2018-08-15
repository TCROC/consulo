/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.boot.util.logger;

import com.intellij.openapi.diagnostic.Logger;
import consulo.util.logging.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 16-May-17
 */
public class WebLoggerFactory implements LoggerFactory {
  @Nonnull
  @Override
  public Logger getLoggerInstance(String category) {
    return new WebLogger();
  }

  @Override
  public int getPriority() {
    return DEFAULT_PRIORITY;
  }

  @Override
  public void shutdown() {

  }
}
