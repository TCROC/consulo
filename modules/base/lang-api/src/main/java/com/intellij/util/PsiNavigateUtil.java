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

package com.intellij.util;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nullable;

public class PsiNavigateUtil {
  public static void navigate(@Nullable final PsiElement psiElement) {
    if (psiElement != null && psiElement.isValid()) {
      final PsiElement navigationElement = psiElement.getNavigationElement();
      final int offset = navigationElement instanceof PsiFile ? -1 : navigationElement.getTextOffset();
      final VirtualFile virtualFile = navigationElement.getContainingFile().getVirtualFile();
      if (virtualFile != null && virtualFile.isValid()) {
        new OpenFileDescriptor(navigationElement.getProject(), virtualFile, offset).navigate(true);
      }
    }
  }
}