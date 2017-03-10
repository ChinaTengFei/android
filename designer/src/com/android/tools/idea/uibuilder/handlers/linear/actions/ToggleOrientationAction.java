/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.uibuilder.handlers.linear.actions;

import com.android.tools.idea.uibuilder.api.ViewEditor;
import com.android.tools.idea.uibuilder.api.ViewHandler;
import com.android.tools.idea.uibuilder.api.actions.DirectViewAction;
import com.android.tools.idea.uibuilder.api.actions.ViewActionPresentation;
import com.android.tools.idea.uibuilder.handlers.linear.LinearLayoutHandler;
import com.android.tools.idea.uibuilder.model.NlComponent;
import icons.AndroidDesignerIcons;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import static com.android.SdkConstants.*;

/**
 * TODO Write documention
 */
public class ToggleOrientationAction extends DirectViewAction {
  @Override
  public void perform(@NotNull ViewEditor editor, @NotNull ViewHandler handler, @NotNull NlComponent component,
                      @NotNull List<NlComponent> selectedChildren,
                      @JdkConstants.InputEventMask int modifiers) {
    assert handler instanceof LinearLayoutHandler;
    LinearLayoutHandler linearLayoutHandler = (LinearLayoutHandler)handler;
    boolean isHorizontal = !linearLayoutHandler.isVertical(component);
    String value = isHorizontal ? VALUE_VERTICAL : null; // null: horizontal is the default
    component.setAttribute(ANDROID_URI, ATTR_ORIENTATION, value);
  }

  @Override
  public void updatePresentation(@NotNull ViewActionPresentation presentation,
                                 @NotNull ViewEditor editor,
                                 @NotNull ViewHandler handler,
                                 @NotNull NlComponent component,
                                 @NotNull List<NlComponent> selectedChildren,
                                 @JdkConstants.InputEventMask int modifiers) {
    assert handler instanceof LinearLayoutHandler;
    LinearLayoutHandler linearLayoutHandler = (LinearLayoutHandler)handler;
    boolean vertical = linearLayoutHandler.isVertical(component);

    presentation.setLabel("Convert orientation to " + (!vertical ? VALUE_VERTICAL : VALUE_HORIZONTAL));
    Icon icon = vertical ? AndroidDesignerIcons.SwitchVerticalLinear : AndroidDesignerIcons.SwitchHorizontalLinear;
    presentation.setIcon(icon);
  }
}
