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

package com.intellij.openapi.components;

import com.intellij.util.xmlb.annotations.Attribute;
import consulo.container.plugin.PluginDescriptor;
import consulo.extensions.PluginAware;

public class ServiceDescriptor implements PluginAware {
  @Attribute("serviceInterface")
  public String serviceInterface;

  @Attribute("serviceImplementation")
  public String serviceImplementation;

  @Attribute("lazy")
  public boolean lazy = true;

  private PluginDescriptor myPluginDescriptor;

  public String getInterface() {
    return serviceInterface != null ? serviceInterface : getImplementation();
  }

  public String getImplementation() {
    return serviceImplementation;
  }

  public boolean isLazy() {
    return lazy;
  }

  @Override
  public void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
    myPluginDescriptor = pluginDescriptor;
  }

  public PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }
}
