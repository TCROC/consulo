/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.merge;

import com.intellij.diff.fragments.MergeLineFragment;
import com.intellij.diff.tools.simple.MergeInnerDifferences;
import com.intellij.diff.tools.simple.ThreesideDiffChangeBase;
import com.intellij.diff.util.*;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diff.DiffBundle;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.annotation.access.RequiredWriteAction;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextMergeChange extends ThreesideDiffChangeBase {
  private static final String CTRL_CLICK_TO_RESOLVE = "Ctrl+click to resolve conflict";

  @Nonnull
  private final TextMergeViewer myMergeViewer;
  @Nonnull
  private final TextMergeViewer.MyThreesideViewer myViewer;

  @Nonnull
  private final List<MyGutterOperation> myOperations = new ArrayList<>();

  private final int myIndex;
  @Nonnull
  private final MergeLineFragment myFragment;

  private final boolean[] myResolved = new boolean[2];
  private boolean myOnesideAppliedConflict;

  @Nullable private MergeInnerDifferences myInnerFragments; // warning: might be out of date

  @RequiredUIAccess
  public TextMergeChange(int index, @Nonnull MergeLineFragment fragment, @Nonnull MergeConflictType conflictType, @Nonnull TextMergeViewer viewer) {
    super(conflictType);
    myMergeViewer = viewer;
    myViewer = viewer.getViewer();

    myIndex = index;
    myFragment = fragment;

    reinstallHighlighters();
  }

  @RequiredUIAccess
  public void destroy() {
    destroyHighlighters();
    destroyOperations();
    destroyInnerHighlighters();
  }

  @RequiredUIAccess
  public void reinstallHighlighters() {
    destroyHighlighters();
    installHighlighters();

    destroyOperations();
    installOperations();

    myViewer.repaintDividers();
  }

  //
  // Getters
  //

  public int getIndex() {
    return myIndex;
  }

  @RequiredUIAccess
  void setResolved(@Nonnull Side side, boolean value) {
    myResolved[side.getIndex()] = value;

    if (isResolved()) {
      destroyInnerHighlighters();
    }
    else {
      // Destroy only resolved side to reduce blinking
      Document document = myViewer.getEditor(side.select(ThreeSide.LEFT, ThreeSide.RIGHT)).getDocument();
      for (RangeHighlighter highlighter : myInnerHighlighters) {
        if (document.equals(highlighter.getDocument())) {
          highlighter.dispose(); // it's OK to call dispose() few times
        }
      }
    }
  }

  public boolean isResolved() {
    return myResolved[0] && myResolved[1];
  }

  public boolean isResolved(@Nonnull Side side) {
    return side.select(myResolved);
  }

  public boolean isOnesideAppliedConflict() {
    return myOnesideAppliedConflict;
  }

  public void markOnesideAppliedConflict() {
    myOnesideAppliedConflict = true;
  }

  @Override
  public boolean isResolved(@Nonnull ThreeSide side) {
    switch (side) {
      case LEFT:
        return isResolved(Side.LEFT);
      case BASE:
        return isResolved();
      case RIGHT:
        return isResolved(Side.RIGHT);
      default:
        throw new IllegalArgumentException(side.toString());
    }
  }

  public int getStartLine() {
    return myViewer.getModel().getLineStart(myIndex);
  }

  public int getEndLine() {
    return myViewer.getModel().getLineEnd(myIndex);
  }

  @Override
  public int getStartLine(@Nonnull ThreeSide side) {
    if (side == ThreeSide.BASE) return getStartLine();
    return myFragment.getStartLine(side);
  }

  @Override
  public int getEndLine(@Nonnull ThreeSide side) {
    if (side == ThreeSide.BASE) return getEndLine();
    return myFragment.getEndLine(side);
  }

  @Nonnull
  @Override
  protected Editor getEditor(@Nonnull ThreeSide side) {
    return myViewer.getEditor(side);
  }

  @Nullable
  @Override
  protected MergeInnerDifferences getInnerFragments() {
    return myInnerFragments;
  }

  @Nonnull
  public MergeLineFragment getFragment() {
    return myFragment;
  }

  @RequiredWriteAction
  public void setInnerFragments(@Nullable MergeInnerDifferences innerFragments) {
    if (myInnerFragments == null && innerFragments == null) return;
    myInnerFragments = innerFragments;

    reinstallHighlighters();

    destroyInnerHighlighters();
    installInnerHighlighters();
  }

  //
  // Gutter actions
  //

  @RequiredUIAccess
  private void installOperations() {
    ContainerUtil.addIfNotNull(myOperations, createOperation(ThreeSide.BASE, OperationType.RESOLVE));
    ContainerUtil.addIfNotNull(myOperations, createOperation(ThreeSide.LEFT, OperationType.APPLY));
    ContainerUtil.addIfNotNull(myOperations, createOperation(ThreeSide.LEFT, OperationType.IGNORE));
    ContainerUtil.addIfNotNull(myOperations, createOperation(ThreeSide.RIGHT, OperationType.APPLY));
    ContainerUtil.addIfNotNull(myOperations, createOperation(ThreeSide.RIGHT, OperationType.IGNORE));
  }

  @RequiredUIAccess
  private void destroyOperations() {
    for (MyGutterOperation operation : myOperations) {
      operation.dispose();
    }
    myOperations.clear();
  }

  @Nullable
  private MyGutterOperation createOperation(@Nonnull ThreeSide side, @Nonnull OperationType type) {
    if (isResolved(side)) return null;

    EditorEx editor = myViewer.getEditor(side);
    Document document = editor.getDocument();

    int line = getStartLine(side);
    int offset = line == DiffUtil.getLineCount(document) ? document.getTextLength() : document.getLineStartOffset(line);

    RangeHighlighter highlighter =
            editor.getMarkupModel().addRangeHighlighter(offset, offset, HighlighterLayer.ADDITIONAL_SYNTAX, null, HighlighterTargetArea.LINES_IN_RANGE);
    return new MyGutterOperation(side, highlighter, type);
  }

  public void updateGutterActions(boolean force) {
    for (MyGutterOperation operation : myOperations) {
      operation.update(force);
    }
  }

  private class MyGutterOperation {
    @Nonnull
    private final ThreeSide mySide;
    @Nonnull
    private final RangeHighlighter myHighlighter;
    @Nonnull
    private final OperationType myType;

    private boolean myCtrlPressed;
    private boolean myShiftPressed;

    private MyGutterOperation(@Nonnull ThreeSide side, @Nonnull RangeHighlighter highlighter, @Nonnull OperationType type) {
      mySide = side;
      myHighlighter = highlighter;
      myType = type;

      update(true);
    }

    public void dispose() {
      myHighlighter.dispose();
    }

    public void update(boolean force) {
      if (!force && !areModifiersChanged()) {
        return;
      }
      if (myHighlighter.isValid()) myHighlighter.setGutterIconRenderer(createRenderer());
    }

    private boolean areModifiersChanged() {
      return myCtrlPressed != myViewer.getModifierProvider().isCtrlPressed() || myShiftPressed != myViewer.getModifierProvider().isShiftPressed();
    }

    @Nullable
    public GutterIconRenderer createRenderer() {
      myCtrlPressed = myViewer.getModifierProvider().isCtrlPressed();
      myShiftPressed = myViewer.getModifierProvider().isShiftPressed();

      if (mySide == ThreeSide.BASE) {
        switch (myType) {
          case RESOLVE:
            if (!Registry.is("diff.merge.resolve.conflict.action.visible")) return null;
            return createResolveRenderer();
          default:
            throw new IllegalArgumentException(myType.name());
        }
      }
      else {
        Side versionSide = mySide.select(Side.LEFT, null, Side.RIGHT);
        assert versionSide != null;

        if (!isChange(versionSide)) return null;

        switch (myType) {
          case APPLY:
            return createApplyRenderer(versionSide, myCtrlPressed);
          case IGNORE:
            return createIgnoreRenderer(versionSide, myCtrlPressed);
          default:
            throw new IllegalArgumentException(myType.name());
        }
      }
    }
  }

  @javax.annotation.Nullable
  private GutterIconRenderer createApplyRenderer(@Nonnull final Side side, final boolean modifier) {
    if (isResolved(side)) return null;
    Image icon = isOnesideAppliedConflict() ? DiffUtil.getArrowDownIcon(side) : DiffUtil.getArrowIcon(side);
    return createIconRenderer(DiffBundle.message("merge.dialog.apply.change.action.name"), icon, isConflict(), () -> {
      myViewer.executeMergeCommand("Accept change", Collections.singletonList(this), () -> {
        myViewer.replaceChange(this, side, modifier);
      });
    });
  }

  @Nullable
  private GutterIconRenderer createIgnoreRenderer(@Nonnull final Side side, final boolean modifier) {
    if (isResolved(side)) return null;
    return createIconRenderer(DiffBundle.message("merge.dialog.ignore.change.action.name"), AllIcons.Diff.Remove, isConflict(), () -> {
      myViewer.executeMergeCommand("Ignore change", Collections.singletonList(this), () -> {
        myViewer.ignoreChange(this, side, modifier);
      });
    });
  }

  @javax.annotation.Nullable
  private GutterIconRenderer createResolveRenderer() {
    if (myViewer.resolveConflictUsingInnerDifferences(this) == null) return null;

    return createIconRenderer(DiffBundle.message("merge.dialog.resolve.change.action.name"), AllIcons.Actions.Checked, false, () -> {
      myViewer.executeMergeCommand("Resolve conflict", Collections.singletonList(this), () -> {
        myViewer.resolveConflictedChange(this);
      });
    });
  }

  @Nonnull
  private GutterIconRenderer createIconRenderer(@Nonnull final String text,
                                                @Nonnull final Image icon,
                                                boolean ctrlClickVisible,
                                                @Nonnull final Runnable perform) {
    final String tooltipText = DiffUtil.createTooltipText(text, ctrlClickVisible ? CTRL_CLICK_TO_RESOLVE : null);
    return new DiffGutterRenderer(icon, tooltipText) {
      @Override
      protected void performAction(AnActionEvent e) {
        perform.run();
      }
    };
  }

  private enum OperationType {
    APPLY, IGNORE, RESOLVE
  }

  //
  // State
  //

  @Nonnull
  State storeState() {
    return new State(myIndex, getStartLine(), getEndLine(),

                     myResolved[0], myResolved[1],

                     myOnesideAppliedConflict);
  }

  void restoreState(@Nonnull State state) {
    myResolved[0] = state.myResolved1;
    myResolved[1] = state.myResolved2;

    myOnesideAppliedConflict = state.myOnesideAppliedConflict;
  }

  public static class State extends MergeModelBase.State {
    private final boolean myResolved1;
    private final boolean myResolved2;

    private final boolean myOnesideAppliedConflict;

    public State(int index, int startLine, int endLine, boolean resolved1, boolean resolved2, boolean onesideAppliedConflict) {
      super(index, startLine, endLine);
      myResolved1 = resolved1;
      myResolved2 = resolved2;
      myOnesideAppliedConflict = onesideAppliedConflict;
    }
  }
}
