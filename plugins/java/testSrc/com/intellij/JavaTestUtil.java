/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.ProjectSdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectSdkTableImpl;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.util.text.StringUtil;
import org.consulo.java.platform.module.extension.JavaModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 * @author Konstantin Bulenkov
 */
public class JavaTestUtil {

  public static String getJavaTestDataPath() {
    return PathManagerEx.getTestDataPath();
  }

  public static String getRelativeJavaTestDataPath() {
    final String absolute = getJavaTestDataPath();
    return StringUtil.trimStart(absolute, PathManager.getHomePath());
  }

  public static void setupTestJDK() {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        Sdk jdk = ProjectSdkTable.getInstance().findSdk("JDK");
        if (jdk != null) {
          ProjectSdkTable.getInstance().removeSdk(jdk);
        }

        ProjectSdkTable.getInstance().addSdk(getTestJdk());
      }
    });
  }

  private static Sdk getTestJdk() {
    try {
      ProjectJdkImpl jdk = (ProjectJdkImpl)JavaAwareProjectSdkTableImpl.getInstanceEx().getInternalJdk().clone();
      jdk.setName("JDK");
      return jdk;
    }
    catch (CloneNotSupportedException e) {
      //LOG.error(e);
      return null;
    }
  }

  @Nullable
  public static Sdk getSdk(@NotNull Module module) {
    return ModuleUtilCore.getSdk(module, JavaModuleExtension.class);
  }
}
