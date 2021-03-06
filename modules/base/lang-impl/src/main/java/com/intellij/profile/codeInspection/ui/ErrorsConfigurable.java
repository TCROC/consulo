/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.profile.codeInspection.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil;
import com.intellij.openapi.project.Project;
import com.intellij.profile.Profile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * Marker interface for the configurable which is used to configure the current inspection profile. 
 *
 * @author yole
 */
public interface ErrorsConfigurable extends Configurable {
  void selectProfile(final Profile profile);

  void selectInspectionTool(final String selectedToolShortName);

  void setFilterLanguages(@Nonnull Collection<Language> languages);

  @Nullable
  Object getSelectedObject();

  class SERVICE {
    private SERVICE() {
    }

    @Nullable
    public static ErrorsConfigurable createConfigurable(@Nonnull Project project) {
      Configurable configurable = ConfigurableExtensionPointUtil.createProjectConfigurableForProvider(project, ErrorsConfigurableProvider.class);
      if (configurable == null) {
        configurable = ConfigurableExtensionPointUtil.createApplicationConfigurableForProvider(ErrorsConfigurableProvider.class);
      }
      return (ErrorsConfigurable)configurable;
    }
  }
}
