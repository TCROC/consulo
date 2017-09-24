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
package consulo.web.application.impl;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import consulo.ui.*;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;

/**
 * @author VISTALL
 * @since 24-Sep-17
 */
public class WebModalProgressIndicator extends EmptyProgressIndicator {
  private Window myWindow;

  private Label myLabel;

  @Override
  public void start() {
    super.start();

    invoke(() -> {
      myWindow = Windows.modalWindow("Consulo");
      myWindow.setClosable(false);
      myWindow.setResizable(false);
      myWindow.setContent(myLabel = Components.label("Loading"));
    });
  }

  private void invoke(@RequiredUIAccess Runnable runnable) {
    WebApplication webApplication = WebApplication.getInstance();
    assert webApplication != null;
    WebSession currentSession = webApplication.getCurrentSession();
    UIAccess access = currentSession == null ? null : currentSession.getAccess();

    if (access == null) {
      return;
    }

    access.give(runnable);
  }

  @Override
  public void setText2(String text) {
    super.setText2(text);

    if (myLabel != null) {
      invoke(() -> {
        myLabel.setText(text);
      });
    }
  }

  @Override
  public void stop() {
    super.stop();

    if (myWindow != null) {
      invoke(() -> {
        myWindow.close();
      });
    }
  }
}
