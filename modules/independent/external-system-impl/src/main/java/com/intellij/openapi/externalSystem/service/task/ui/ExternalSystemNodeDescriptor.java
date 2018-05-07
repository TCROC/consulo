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
package com.intellij.openapi.externalSystem.service.task.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Denis Zhdanov
 * @since 5/14/13 2:32 PM
 */
public class ExternalSystemNodeDescriptor<T> extends PresentableNodeDescriptor<T> {

  @Nonnull
  private final T myElement;
  @Nonnull
  private final String myDescription;

  public ExternalSystemNodeDescriptor(@Nonnull T element, @Nonnull String name, @Nonnull String description, @Nullable Image icon) {
    super(null, null);
    myElement = element;
    myName = name;
    setIcon(TargetAWT.to(icon));
    myDescription = description;
    getPresentation().setTooltip(description);
  }

  public void setName(@Nonnull String name) {
    myName = name;
  }
  
  @Override
  protected void update(PresentationData presentation) {
    presentation.setPresentableText(myName);
    presentation.setIcon(TargetAWT.to(getIcon()));
    presentation.setTooltip(myDescription);
  }
  
  @Nonnull
  @Override
  public T getElement() {
    return myElement;
  }
}
