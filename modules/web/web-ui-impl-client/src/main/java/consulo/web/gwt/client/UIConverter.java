/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt.client;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Window;
import consulo.web.gwt.client.ui.*;
import consulo.web.gwt.client.util.BitUtil;
import consulo.web.gwt.shared.UIComponent;
import consulo.web.gwt.shared.ui.InternalEventTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class UIConverter {
  private static final UIFactory ourFactory = GWT.create(UIFactory.class);

  private static Map<Long, InternalGwtComponent> ourCache = new HashMap<>();

  public static InternalGwtComponent create(WebSocketProxy proxy, UIComponent uiComponent) {
    final String type = uiComponent.getType();
    InternalGwtComponent widget = ourFactory.create(type);
    if (widget == null) {
      Window.alert("Type " + type + " is not handled.");

      return null;
    }

    ourCache.put(uiComponent.getId(), widget);

    final Map<String, Object> variables = uiComponent.getVariables();

    if (widget instanceof InternalGwtComponentWithChildren) {
      final List<UIComponent.Child> children = uiComponent.getChildren();
      ((InternalGwtComponentWithChildren)widget).addChildren(proxy, children == null ? Collections.<UIComponent.Child>emptyList() : children);
    }

    final Map<String, Object> map = variables == null ? Collections.<String, Object>emptyMap() : variables;
    DefaultVariables.updateState(map, widget);
    widget.updateState(map);
    if (widget instanceof InternalGwtComponentWithListeners) {
      ((InternalGwtComponentWithListeners)widget).setupListeners(proxy, uiComponent.getId());
    }

    if (BitUtil.isSet(uiComponent.getEventBits(), InternalEventTypes.SHOW)) {
      widget.asWidget().addAttachHandler(event -> {
        proxy.sendFireListener(uiComponent.getId(), InternalEventTypes.SHOW, vars -> {
        });
      });
    }

    return widget;
  }

  public static InternalGwtComponent get(long id) {
    return ourCache.get(id);
  }
}
