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

/*
 * User: anna
 * Date: 25-Jan-2008
 */
package com.intellij.codeEditor.printing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.LineMarkersPass;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.SeparatorPlacement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import consulo.annotation.access.RequiredReadAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileSeparatorUtil {
  @Nonnull
  @RequiredReadAction
  public static List<LineMarkerInfo> getFileSeparators(final PsiFile file, final Document document) {
    final List<LineMarkerInfo> result = new ArrayList<LineMarkerInfo>();
    for (LineMarkerInfo lineMarkerInfo : LineMarkersPass.queryLineMarkers(file, document)) {
      if (lineMarkerInfo.separatorColor != null) {
        result.add(lineMarkerInfo);
      }
    }

    Collections.sort(result, new Comparator<LineMarkerInfo>() {
      @Override
      public int compare(final LineMarkerInfo i1, final LineMarkerInfo i2) {
        return getDisplayLine(i1, document) - getDisplayLine(i2, document);
      }
    });
    return result;
  }

  public static int getDisplayLine(@Nonnull LineMarkerInfo lineMarkerInfo, @Nonnull Document document) {
    int offset = lineMarkerInfo.separatorPlacement == SeparatorPlacement.TOP ? lineMarkerInfo.startOffset : lineMarkerInfo.endOffset;
    return document.getLineNumber(Math.min(document.getTextLength(), Math.max(0, offset))) +
           (lineMarkerInfo.separatorPlacement == SeparatorPlacement.TOP ? 0 : 1);
  }
}