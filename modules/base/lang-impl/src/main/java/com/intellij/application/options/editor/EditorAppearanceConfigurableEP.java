/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.application.options.editor;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.options.UnnamedConfigurable;
import consulo.options.ApplicationConfigurableEP;

import jakarta.inject.Inject;

/**
 * To provide additional options in Editor | Appearance section register implementation of
 * {@link com.intellij.openapi.options.UnnamedConfigurable} in the plugin.xml:
 * <p/>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;editorAppearanceConfigurable instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p>
 * A new instance of the specified class will be created each time then the Settings dialog is opened
 *
 * @author nik
 */
public class EditorAppearanceConfigurableEP extends ApplicationConfigurableEP<UnnamedConfigurable> {
  @Inject
  public EditorAppearanceConfigurableEP(Application application) {
    super(application);
  }
}
