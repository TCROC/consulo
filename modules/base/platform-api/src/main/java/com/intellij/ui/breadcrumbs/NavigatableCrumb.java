// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.breadcrumbs;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.components.breadcrumbs.Crumb;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A breadcrumb that supports navigation and highlighting.
 *
 * @author yole
 */
public interface NavigatableCrumb extends Crumb {
  @Nullable
  TextRange getHighlightRange();

  default int getAnchorOffset() {
    return -1;
  }

  void navigate(@Nonnull Editor editor, boolean withSelection);
}
