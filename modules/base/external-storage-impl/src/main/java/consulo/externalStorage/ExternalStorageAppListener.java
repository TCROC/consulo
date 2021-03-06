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
package consulo.externalStorage;

import com.intellij.ide.ApplicationLoadListener;
import com.intellij.openapi.application.Application;
import consulo.components.impl.stores.IApplicationStore;
import consulo.components.impl.stores.storage.StateStorageManager;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 11-Feb-17
 */
public class ExternalStorageAppListener implements ApplicationLoadListener {
  private final Application myApplication;
  private final IApplicationStore myApplicationStore;

  @Inject
  public ExternalStorageAppListener(@Nonnull Application application, IApplicationStore applicationStore) {
    myApplicationStore = applicationStore;
    myApplication = application;
  }

  @Override
  public void beforeApplicationLoaded() {
    StateStorageManager stateStorageManager = myApplicationStore.getStateStorageManager();

    stateStorageManager.setStreamProvider(new ExternalStorageStreamProvider(myApplication, myApplicationStore));
  }
}
