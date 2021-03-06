package com.intellij.openapi.editor;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import javax.annotation.Nonnull;

/**
 * Defines contract for extending editor navigation functionality. 
 * 
 * @author Denis Zhdanov
 * @since 5/26/11 3:31 PM
 */
public interface EditorNavigationDelegate {

  ExtensionPointName<EditorNavigationDelegate> EP_NAME = ExtensionPointName.create("com.intellij.editorNavigation");
  
  enum Result {
    /**
     * Navigation request is completely handled by the current delegate and no further processing is required.
     */
    STOP,

    /**
     * Continue navigation request processing.
     */
    CONTINUE
  }

  @Nonnull
  Result navigateToLineEnd(@Nonnull Editor editor, @Nonnull DataContext dataContext);
}
