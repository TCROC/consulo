// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import com.intellij.psi.codeStyle.modifier.CodeStyleStatusBarUIContributor;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class IndentStatusBarUIContributor implements CodeStyleStatusBarUIContributor {
  private final IndentOptions myIndentOptions;

  public IndentStatusBarUIContributor(IndentOptions options) {
    myIndentOptions = options;
  }

  public IndentOptions getIndentOptions() {
    return myIndentOptions;
  }

  /**
   * Returns a short, usually one-word, string to indicate the source of the given indent options.
   *
   * @return The indent options source hint or {@code null} if not available.
   */
  @Nullable
  public abstract String getHint();

  @Nullable
  @Override
  public String getTooltip() {
    return createTooltip(getIndentInfo(myIndentOptions), getHint());
  }

  @Nls
  @Nonnull
  public static String getIndentInfo(@Nonnull IndentOptions indentOptions) {
    return indentOptions.USE_TAB_CHARACTER ? LangBundle.message("indent.status.bar.tab") : LangBundle.message("indent.status.bar.spaces", indentOptions.INDENT_SIZE);
  }

  /**
   * @return True if "Configure indents for [Language]" action should be available when the provider is active (returns its own indent
   * options), false otherwise.
   */
  public boolean isShowFileIndentOptionsEnabled() {
    return true;
  }

  @Nonnull
  public static
  String createTooltip(@Nls String indentInfo, String hint) {
    HtmlBuilder builder = new HtmlBuilder();
    builder.append(LangBundle.message("indent.status.bar.indent.tooltip")).append(indentInfo);
    if (hint != null) {
      builder.nbsp(2).append(HtmlChunk.span("color:" + ColorUtil.toHtmlColor(JBColor.GRAY)).addText(hint));
    }
    return builder.wrapWith("html").toString();
  }

  @Nonnull
  @Override
  public String getStatusText(@Nonnull PsiFile psiFile) {
    String widgetText = getIndentInfo(myIndentOptions);
    IndentOptions projectIndentOptions = CodeStyle.getSettings(psiFile.getProject()).getLanguageIndentOptions(psiFile.getLanguage());
    if (!projectIndentOptions.equals(myIndentOptions)) {
      widgetText += "*";
    }
    return widgetText;
  }
}

