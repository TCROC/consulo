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
package consulo.web.gwt.shared;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UIComponent extends UIVariablesOwner {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Child extends UIVariablesOwner {
    private UIComponent myComponent;

    public UIComponent getComponent() {
      return myComponent;
    }

    public void setComponent(UIComponent component) {
      myComponent = component;
    }
  }

  private long myId;
  private String myType;
  private long eventBits;
  private List<Child> myChildren;

  public long getId() {
    return myId;
  }

  public void setId(long id) {
    myId = id;
  }

  public long getEventBits() {
    return eventBits;
  }

  public void setEventBits(long eventBits) {
    this.eventBits = eventBits;
  }

  public String getType() {
    return myType;
  }

  public void setType(String type) {
    myType = type;
  }

  @Nullable
  public List<Child> getChildren() {
    return myChildren;
  }

  public void setChildren(List<Child> children) {
    myChildren = children;
  }
}
