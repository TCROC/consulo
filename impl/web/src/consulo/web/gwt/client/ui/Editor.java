/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwt.client.ui;

import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.service.EditorColorSchemeService;
import consulo.web.gwt.client.util.GwtStyleUtil;
import consulo.web.gwt.client.util.GwtUtil;
import consulo.web.gwt.client.util.ReportableCallable;
import consulo.web.gwt.shared.transport.*;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class Editor extends SimplePanel {
  private class CodeLinePanel extends FlowPanel {

    public CodeLinePanel() {
      sinkEvents(Event.ONCLICK);
      refreshStyles();
    }

    public void refreshStyles() {
      setDefaultColors(this);
    }

    @Override
    public void onBrowserEvent(Event event) {
      switch (DOM.eventGetType(event)) {
        case Event.ONCLICK:
          changeLine(this);
          break;
        default:
          event.preventDefault();
          break;
      }
    }
  }

  public static final int ourLexerFlag = 1 << 1;
  public static final int ourEditorFlag = 1 << 2;

  public static final int ourSelectFlag = 1 << 24;

  private final EditorSegmentBuilder myBuilder;

  private final int myLineCount;

  private EditorCaretHandler myCaretHandler;

  private int myLastCaretOffset = -1;

  private final EditorTabPanel myEditorTabPanel;

  private final String myFileUrl;

  private GwtTextRange myLastCursorPsiElementTextRange;

  private GwtNavigateInfo myLastNavigationInfo;

  private CodeLinePanel myLastLine;

  private GwtEditorColorScheme myScheme;

  public Editor(EditorTabPanel editorTabPanel, String fileUrl, String text) {
    myEditorTabPanel = editorTabPanel;
    myFileUrl = fileUrl;
    myBuilder = new EditorSegmentBuilder(text);
    myLineCount = myBuilder.getLineCount();

    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS);

    final EditorColorSchemeService editorColorSchemeService = GwtUtil.get(EditorColorSchemeService.KEY);
    myScheme = editorColorSchemeService.getScheme();

    setDefaultColors(this);
    GwtUtil.fill(this);
  }

  private void setDefaultColors(Widget widget) {
    GwtTextAttributes textAttr = myScheme.getAttributes(GwtEditorColorScheme.TEXT);
    if (textAttr != null) {
      GwtColor background = textAttr.getBackground();
      if (background != null) {
        widget.getElement().getStyle().setBackgroundColor(GwtStyleUtil.toString(background));
      }

      GwtColor foreground = textAttr.getForeground();
      if (foreground != null) {
        widget.getElement().getStyle().setColor(GwtStyleUtil.toString(foreground));
      }
    }
  }
  @Override
  public void onBrowserEvent(final Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONMOUSEOVER:
        com.google.gwt.dom.client.Element element = DOM.eventGetToElement(event);

        Object range = element == null ? null : element.getPropertyObject("range");
        if (!(range instanceof GwtTextRange)) {
          myLastCaretOffset = -1;
          return;
        }


        int startOffset = ((GwtTextRange)range).getStartOffset();
        if (startOffset == myLastCaretOffset) {
          return;
        }
        myLastCaretOffset = startOffset;

        if (event.getCtrlKey()) {
          getElement().getStyle().setCursor(Style.Cursor.POINTER);

          GwtUtil.rpc().getNavigationInfo(myFileUrl, myLastCaretOffset, new ReportableCallable<GwtNavigateInfo>() {
            @Override
            public void onSuccess(GwtNavigateInfo result) {
              if (result == null) {
                return;
              }

              event.getRelatedEventTarget();

              GwtTextRange resultElementRange = result.getRange();
              if (myLastCursorPsiElementTextRange != null && myLastCursorPsiElementTextRange.containsRange(resultElementRange)) {
                return;
              }
              myLastCursorPsiElementTextRange = resultElementRange;
              GwtHighlightInfo highlightInfo =
                      new GwtHighlightInfo(myScheme.getAttributes(GwtEditorColorScheme.HYPERLINK_ATTRIBUTES), resultElementRange, Integer.MAX_VALUE);

              myLastNavigationInfo = result;

              addHighlightInfos(Arrays.asList(highlightInfo), ourSelectFlag);
            }
          });
        }
        event.preventDefault();
        break;
      case Event.ONMOUSEOUT:
        onMouseOut();
        event.preventDefault();
        break;
      case Event.ONCLICK:
        if (event.getCtrlKey()) {
          if (myLastNavigationInfo != null) {
            List<GwtNavigatable> navigates = myLastNavigationInfo.getNavigates();

            GwtNavigatable navigatable = navigates.get(0);

            onMouseOut();

            myEditorTabPanel.openFileInEditor(navigatable.getFile(), navigatable.getOffset());
          }
        }
        else {
          if (myCaretHandler != null) {
            myCaretHandler.caretPlaced(myLastCaretOffset);
          }
        }
        event.preventDefault();
        break;
      default:
        super.onBrowserEvent(event);
        break;
    }
  }

  private void onMouseOut() {
    if (myLastCursorPsiElementTextRange != null) {
      getElement().getStyle().clearCursor();

      myBuilder.removeHighlightByRange(myLastCursorPsiElementTextRange, ourSelectFlag);

      myLastCursorPsiElementTextRange = null;
      myLastNavigationInfo = null;
    }
  }

  private void build() {
    Grid gridPanel = GwtUtil.fillAndReturn(new Grid(1, 2));
    setDefaultColors(gridPanel);

    // try to fill area by code
    gridPanel.getColumnFormatter().getElement(1).getStyle().setWidth(100, Style.Unit.PCT);

    VerticalPanel editorLinePanel = new VerticalPanel();
    gridPanel.setWidget(0, 0, editorLinePanel);

    editorLinePanel.addStyleName("noselectable");

    editorLinePanel.getElement().getStyle().setProperty("borderRightColor", GwtStyleUtil.toString(myScheme.getColor(GwtEditorColorScheme.TEARLINE_COLOR)));
    editorLinePanel.getElement().getStyle().setProperty("borderRightStyle", "solid");
    editorLinePanel.getElement().getStyle().setProperty("borderRightWidth", "1px");
    editorLinePanel.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);
    editorLinePanel.getElement().getStyle().setBackgroundColor(GwtStyleUtil.toString(myScheme.getColor(GwtEditorColorScheme.GUTTER_BACKGROUND)));

    for (int i = 0; i < myLineCount; i++) {
      final Grid panel = GwtUtil.fillAndReturn(new Grid(1, 5)); // 5 fake size
      // place lines to right
      panel.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT);

      panel.getCellFormatter().getElement(0, 0).getStyle().setPaddingLeft(5, Style.Unit.PX);
      panel.getCellFormatter().getElement(0, 4).getStyle().setPaddingRight(5, Style.Unit.PX);

      // try fill line number as primary panel
      panel.getColumnFormatter().getElement(0).getStyle().setWidth(100, Style.Unit.PCT);

      InlineHTML lineSpan = new InlineHTML(String.valueOf(i + 1));
      lineSpan.addStyleName("editorLine");
      lineSpan.addStyleName("editorGutterLine" + i);
      lineSpan.getElement().getStyle().setColor(GwtStyleUtil.toString(myScheme.getColor(GwtEditorColorScheme.LINE_NUMBERS_COLOR)));

      panel.setWidget(0, 0, lineSpan);

      editorLinePanel.add(panel);
    }

    VerticalPanel editorCodePanel = new VerticalPanel() {
      {
        sinkEvents(Event.ONCHANGE | Event.ONPASTE | Event.KEYEVENTS);
      }

      @Override
      public void onBrowserEvent(Event event) {
        event.preventDefault();
      }
    };
    gridPanel.setWidget(0, 1, editorCodePanel);

    GwtUtil.fill(editorCodePanel);

    // dont provide red code
    editorCodePanel.getElement().setAttribute("spellcheck", "false");
    // editable
    editorCodePanel.getElement().setAttribute("contenteditable", "true");
    // disable border
    editorCodePanel.addStyleName("noFocusBorder");

    int lineCount = 0;
    FlowPanel lineElement = null;

    for (EditorSegmentBuilder.Fragment fragment : myBuilder.getFragments()) {
      if (lineElement == null) {
        lineElement = new CodeLinePanel();

        lineElement.setWidth("100%");
        lineElement.addStyleName("editorLine");
        lineElement.addStyleName("gen_Line_" + lineCount);
      }

      lineElement.add(fragment.widget);

      if (fragment.lineWrap) {
        editorCodePanel.add(lineElement);
        lineElement = null;

        lineCount++;
      }
    }

    ScrollPanel scrollPanel = new ScrollPanel(gridPanel);
    GwtUtil.fill(scrollPanel);

    DockPanel panel = new DockPanel();
    panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    panel.setWidth("100%");
    panel.add(scrollPanel, DockPanel.CENTER);

    setWidget(panel);
  }

  private void setupTooltip(Widget w, String message) {
    Tooltip tooltip = new Tooltip();
    tooltip.setWidget(w);
    tooltip.setText(message);
    tooltip.setPlacement(Placement.BOTTOM);
    tooltip.reconfigure();
  }

  public void setCaretHandler(EditorCaretHandler caretHandler) {
    myCaretHandler = caretHandler;
  }

  public Widget getComponent() {
    build();

    return this;
  }

  public void addHighlightInfos(List<GwtHighlightInfo> result, int flag) {
    myBuilder.addHighlights(result, flag);
  }

  public void setCaretOffset(int offset) {
    myLastCaretOffset = offset;

    focusOffset(offset);

    if (myCaretHandler != null) {
      myCaretHandler.caretPlaced(myLastCaretOffset);
    }
  }

  public void focusOffset(int offset) {
    myLastCaretOffset = offset;
    for (EditorSegmentBuilder.Fragment fragment : myBuilder.getFragments()) {
      if (fragment.range.containsRange(offset, offset)) {
        fragment.widget.getElement().focus();
        fragment.widget.getElement().scrollIntoView();

        set(fragment.widget.getElement());
        Widget parent = fragment.widget.getParent();
        if (parent instanceof CodeLinePanel) {
          changeLine((CodeLinePanel)parent);
        }

        break;
      }
    }
  }

  public void changeLine(CodeLinePanel widget) {
    if (myLastLine == widget) {
      return;
    }
    if (myLastLine != null) {
      myLastLine.refreshStyles();
    }

    widget.getElement().getStyle().setBackgroundColor(GwtStyleUtil.toString(myScheme.getColor(GwtEditorColorScheme.CARET_ROW_COLOR)));
    myLastLine = widget;
  }

  public native void set(Element element) /*-{
    var range = $doc.createRange();
    var sel = $wnd.getSelection();

    console.log(element);

    range.setStart(element, 1);
    range.setEnd(element, 1);
    range.collapse(true);
    sel.removeAllRanges();
    sel.addRange(range);
    element.focus();
  }-*/;
}
