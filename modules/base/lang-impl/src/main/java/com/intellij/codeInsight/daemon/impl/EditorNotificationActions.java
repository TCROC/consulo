// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.intention.IntentionActionProvider;
import com.intellij.codeInsight.intention.IntentionActionWithOptions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import consulo.fileEditor.impl.text.TextEditorProvider;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.List;

class EditorNotificationActions implements IntentionMenuContributor {
  @Override
  public void collectActions(@Nonnull Editor hostEditor, @Nonnull PsiFile hostFile, @Nonnull ShowIntentionsPass.IntentionsInfo intentions, int passIdToShowIntentionsFor, int offset) {
    Project project = hostEditor.getProject();
    if (project == null) return;
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    if (!(fileEditorManager instanceof FileEditorManagerImpl)) return;
    TextEditor fileEditor = TextEditorProvider.getInstance().getTextEditor(hostEditor);
    List<JComponent> components = ((FileEditorManagerImpl)fileEditorManager).getTopComponents(fileEditor);
    for (JComponent component : components) {
      if (component instanceof IntentionActionProvider) {
        IntentionActionWithOptions action = ((IntentionActionProvider)component).getIntentionAction();
        if (action != null) {
          intentions.notificationActionsToShow.add(new HighlightInfo.IntentionActionDescriptor(action, action.getOptions(), null));
        }
      }
    }
  }
}
