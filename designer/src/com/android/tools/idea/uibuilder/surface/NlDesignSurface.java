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
package com.android.tools.idea.uibuilder.surface;

import com.android.annotations.VisibleForTesting;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;
import com.android.tools.adtui.common.SwingCoordinate;
import com.android.tools.idea.configurations.Configuration;
import com.android.tools.idea.ddms.screenshot.DeviceArtPainter;
import com.android.tools.idea.gradle.project.BuildSettings;
import com.android.tools.idea.gradle.util.BuildMode;
import com.android.tools.idea.rendering.RenderErrorModelFactory;
import com.android.tools.idea.rendering.RenderResult;
import com.android.tools.idea.rendering.errors.ui.RenderErrorModel;
import com.android.tools.idea.uibuilder.api.ViewEditor;
import com.android.tools.idea.uibuilder.api.ViewHandler;
import com.android.tools.idea.uibuilder.editor.ActionManager;
import com.android.tools.idea.uibuilder.editor.NlActionManager;
import com.android.tools.idea.uibuilder.menu.NavigationViewSceneView;
import com.android.tools.idea.uibuilder.mockup.editor.MockupEditor;
import com.android.tools.idea.uibuilder.model.Coordinates;
import com.android.tools.idea.uibuilder.model.NlComponent;
import com.android.tools.idea.uibuilder.model.NlLayoutType;
import com.android.tools.idea.uibuilder.model.NlModel;
import com.android.tools.idea.uibuilder.property.NlPropertiesManager;
import com.android.tools.idea.uibuilder.property.inspector.NlInspectorProviders;
import com.android.tools.idea.uibuilder.scene.LayoutlibSceneManager;
import com.android.tools.idea.uibuilder.scene.Scene;
import com.android.tools.idea.uibuilder.scene.SceneManager;
import com.android.tools.idea.uibuilder.surface.ScreenView.ScreenViewType;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.ref.WeakReference;
import java.util.Objects;

import static com.android.SdkConstants.ATTR_SHOW_IN;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.tools.idea.uibuilder.graphics.NlConstants.*;

/**
 * The {@link DesignSurface} for the layout editor, which contains the full background, rulers, one
 * or more device renderings, etc
 */
public class NlDesignSurface extends DesignSurface {
  public enum ScreenMode {
    SCREEN_ONLY,
    BLUEPRINT_ONLY,
    BOTH;

    @VisibleForTesting
    @NotNull
    static final ScreenMode DEFAULT_SCREEN_MODE = BOTH;

    @NotNull
    public ScreenMode next() {
      ScreenMode[] values = values();
      return values[(ordinal() + 1) % values.length];
    }

    @VisibleForTesting
    static final String SCREEN_MODE_PROPERTY = "NlScreenMode";

    @NotNull
    public static ScreenMode loadPreferredMode() {
      String modeName = PropertiesComponent.getInstance().getValue(SCREEN_MODE_PROPERTY, DEFAULT_SCREEN_MODE.name());
      try {
        return valueOf(modeName);
      }
      catch (IllegalArgumentException e) {
        // If the code reach here, that means some of unexpected ScreenMode is saved as user's preference.
        // In this case, return the default mode instead.
        Logger.getInstance(NlDesignSurface.class)
          .warn("The mode " + modeName + " is not recognized, use default mode " + SCREEN_MODE_PROPERTY + " instead");
        return DEFAULT_SCREEN_MODE;
      }
    }

    public static void savePreferredMode(@NotNull ScreenMode mode) {
      PropertiesComponent.getInstance().setValue(SCREEN_MODE_PROPERTY, mode.name());
    }
  }

  @NotNull private static ScreenMode ourDefaultScreenMode = ScreenMode.loadPreferredMode();

  @NotNull private ScreenMode myScreenMode = ourDefaultScreenMode;
  @Nullable private ScreenView myBlueprintView;
  @SwingCoordinate private int myScreenX = RULER_SIZE_PX + DEFAULT_SCREEN_OFFSET_X;
  @SwingCoordinate private int myScreenY = RULER_SIZE_PX + DEFAULT_SCREEN_OFFSET_Y;
  private boolean myIsCanvasResizing = false;
  private boolean myStackVertically;
  private boolean myMockupVisible;
  private MockupEditor myMockupEditor;
  private boolean myCentered;
  @Nullable private ScreenView myScreenView;
  private final boolean myInPreview;
  private WeakReference<PanZoomPanel> myPanZoomPanel = new WeakReference<>(null);

  public NlDesignSurface(@NotNull Project project, boolean inPreview, @NotNull Disposable parentDisposable) {
    super(project, parentDisposable);
    myInPreview = inPreview;
  }

  public boolean isPreviewSurface() {
    return myInPreview;
  }

  /**
   * Tells this surface to resize mode. While on resizing mode, the views won't be auto positioned.
   * This can be disabled to avoid moving the screens around when the user is resizing the canvas. See {@link CanvasResizeInteraction}
   *
   * @param isResizing true to enable the resize mode
   */
  public void setResizeMode(boolean isResizing) {
    myIsCanvasResizing = isResizing;
  }

  /**
   * Returns whether this surface is currently in resize mode or not. See {@link #setResizeMode(boolean)}
   */
  public boolean isCanvasResizing() {
    return myIsCanvasResizing;
  }

  @Override
  public boolean isLayoutDisabled() {
    return myIsCanvasResizing;
  }

  @Override
  public void activate() {
    super.activate();
    showPanZoomPanelIfRequired();
  }

  @NotNull
  public ScreenMode getScreenMode() {
    return myScreenMode;
  }

  public void setScreenMode(@NotNull ScreenMode screenMode, boolean setAsDefault) {
    if (setAsDefault) {
      if (ourDefaultScreenMode != screenMode) {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        ourDefaultScreenMode = screenMode;

        ScreenMode.savePreferredMode(screenMode);
      }
    }

    if (screenMode != myScreenMode) {
      // If we're going from 1 screens to 2 or back from 2 to 1, must adjust the zoom
      // to-fit the screen(s) in the surface
      boolean adjustZoom = screenMode == ScreenMode.BOTH || myScreenMode == ScreenMode.BOTH;
      myScreenMode = screenMode;

      createSceneViews();
      if (myScreenView != null) {
        if (adjustZoom) {
          zoomToFit();
        }
      }
      // do request a render, as if coming from the blueprint mode we might not have the latest rendered image
      if (screenMode != ScreenMode.BLUEPRINT_ONLY) {
        SceneManager manager = getSceneManager();

        if (manager != null) {
          manager.requestRender();
        }
      }
    }
  }

  @NotNull
  @Override
  protected SceneManager createSceneManager(@NotNull NlModel model) {
    return new LayoutlibSceneManager(model, this);
  }

  private void addLayers() {
    myLayers.add(new MyBottomLayer());

    switch (myScreenMode) {
      case SCREEN_ONLY:
        addScreenLayers();
        break;
      case BLUEPRINT_ONLY:
        assert myScreenView != null;
        addBlueprintLayers(myScreenView);

        break;
      case BOTH:
        addScreenLayers();

        assert myBlueprintView != null;
        addBlueprintLayers(myBlueprintView);

        break;
      default:
        assert false : myScreenMode;
    }
  }

  private void addScreenLayers() {
    assert myScreenView != null;

    myLayers.add(new ScreenViewLayer(myScreenView));
    myLayers.add(new SelectionLayer(myScreenView));

    if (myScreenView.getModel().getType().isLayout()) {
      myLayers.add(new ConstraintsLayer(this, myScreenView, true));
    }

    SceneLayer sceneLayer = new SceneLayer(this, myScreenView, false);
    sceneLayer.setAlwaysShowSelection(true);
    myLayers.add(new WarningLayer(myScreenView));
    myLayers.add(sceneLayer);
    if (getLayoutType().isSupportedByDesigner()) {
      myLayers.add(new CanvasResizeLayer(this, myScreenView));
    }
  }

  private void addBlueprintLayers(@NotNull ScreenView view) {
    myLayers.add(new SelectionLayer(view));
    myLayers.add(new MockupLayer(view));
    myLayers.add(new CanvasResizeLayer(this, view));
    myLayers.add(new SceneLayer(this, view, true));
  }

  /**
   * Set the ConstraintsLayer and SceneLayer layers to paint,
   * even if they are set to paint only on mouse hover
   *
   * @param value if true, force painting
   */
  public void forceLayersPaint(boolean value) {
    for (Layer layer : myLayers) {
      if (layer instanceof ConstraintsLayer) {
        ConstraintsLayer constraintsLayer = (ConstraintsLayer)layer;
        constraintsLayer.setTemporaryShow(value);
        repaint();
      }
      if (layer instanceof SceneLayer) {
        SceneLayer sceneLayer = (SceneLayer)layer;
        sceneLayer.setTemporaryShow(value);
        repaint();
      }
    }
  }

  @Nullable
  @Override
  public ScreenView getCurrentSceneView() {
    return myScreenView;
  }

  @Override
  @Nullable
  public SceneView getSceneView(@SwingCoordinate int x, @SwingCoordinate int y) {
    // Currently only a single screen view active in the canvas.
    if (myBlueprintView != null && x >= myBlueprintView.getX() && y >= myBlueprintView.getY()) {
      return myBlueprintView;
    }
    return myScreenView;
  }

  /**
   * Return the ScreenView under the given position
   *
   * @param x
   * @param y
   * @return the ScreenView, or null if we are not above one.
   */
  @Nullable
  @Override
  public ScreenView getHoverSceneView(@SwingCoordinate int x, @SwingCoordinate int y) {
    if (myBlueprintView != null
        && x >= myBlueprintView.getX() && x <= myBlueprintView.getX() + myBlueprintView.getSize().width
        && y >= myBlueprintView.getY() && y <= myBlueprintView.getY() + myBlueprintView.getSize().height) {
      return myBlueprintView;
    }
    if (myScreenView != null
        && x >= myScreenView.getX() && x <= myScreenView.getX() + myScreenView.getSize().width
        && y >= myScreenView.getY() && y <= myScreenView.getY() + myScreenView.getSize().height) {
      return myScreenView;
    }
    return null;
  }

  @NotNull
  @Override
  public NlInspectorProviders getInspectorProviders(@NotNull NlPropertiesManager propertiesManager, @NotNull Disposable parentDisposable) {
    return new NlInspectorProviders(propertiesManager, parentDisposable);
  }

  @Nullable
  public ScreenView getBlueprintView() {
    return myBlueprintView;
  }

  @Override
  public Dimension getScrolledAreaSize() {
    if (myScreenView == null) {
      return null;
    }
    Dimension size = myScreenView.getSize();
    // TODO: Account for the size of the blueprint screen too? I should figure out if I can automatically make it jump
    // to the side or below based on the form factor and the available size
    Dimension dimension = new Dimension(size.width + 2 * DEFAULT_SCREEN_OFFSET_X,
                                        size.height + 2 * DEFAULT_SCREEN_OFFSET_Y);
    if (myScreenMode == ScreenMode.BOTH) {
      if (isStackVertically()) {
        dimension.setSize(dimension.getWidth(),
                          dimension.getHeight() + size.height + SCREEN_DELTA);
      }
      else {
        dimension.setSize(dimension.getWidth() + size.width + SCREEN_DELTA,
                          dimension.getHeight());
      }
    }
    return dimension;
  }

  /**
   * Returns true if we want to arrange screens vertically instead of horizontally
   */
  private static boolean isVerticalScreenConfig(int availableWidth, int availableHeight, @NotNull Dimension preferredSize) {
    boolean stackVertically = preferredSize.width > preferredSize.height;
    if (availableWidth > 10 && availableHeight > 3 * availableWidth / 2) {
      stackVertically = true;
    }
    return stackVertically;
  }

  public void setCentered(boolean centered) {
    myCentered = centered;
  }

  @NotNull
  @Override
  protected ActionManager createActionManager() {
    return new NlActionManager(this);
  }

  /**
   * <p>
   * If type is {@link ZoomType#IN}, zoom toward the given x and y coordinates
   * (relative to {@link #getLayeredPane()})
   * </p><p>
   * If x or y are negative, zoom toward the selected component if there is one otherwise
   * zoom toward the center of the viewport.
   * </p><p>
   * For all other types of zoom see {@link DesignSurface#zoom(ZoomType, int, int)}
   * </p>
   *
   * @param type Type of zoom to execute
   * @param x    Coordinate where the zoom will be centered
   * @param y    Coordinate where the zoom will be centered
   * @see DesignSurface#zoom(ZoomType, int, int)
   */
  @Override
  public void zoom(@NotNull ZoomType type, int x, int y) {
    if (type == ZoomType.IN && (x < 0 || y < 0)
        && myScreenView != null && !myScreenView.getSelectionModel().isEmpty()) {
      NlComponent component = myScreenView.getSelectionModel().getPrimary();
      if (component != null) {
        x = Coordinates.getSwingX(myScreenView, component.getMidpointX());
        y = Coordinates.getSwingY(myScreenView, component.getMidpointY());
      }
    }
    super.zoom(type, x, y);
  }

  @Override
  protected void layoutContent() {
    if (myScreenView == null) {
      return;
    }
    Dimension screenViewSize = myScreenView.getSize();

    // Position primary screen
    int availableWidth = myScrollPane.getWidth();
    int availableHeight = myScrollPane.getHeight();
    myStackVertically = isVerticalScreenConfig(availableWidth, availableHeight, screenViewSize);

    // If we are resizing the canvas, do not relocate the primary screen
    if (!myIsCanvasResizing) {
      if (myCentered && availableWidth > 10 && availableHeight > 10) {
        int requiredWidth = screenViewSize.width;
        if (myScreenMode == ScreenMode.BOTH && !myStackVertically) {
          requiredWidth += SCREEN_DELTA;
          requiredWidth += screenViewSize.width;
        }
        myScreenX = Math.max((availableWidth - requiredWidth) / 2, RULER_SIZE_PX + DEFAULT_SCREEN_OFFSET_X);

        int requiredHeight = screenViewSize.height;
        if (myScreenMode == ScreenMode.BOTH && myStackVertically) {
          requiredHeight += SCREEN_DELTA;
          requiredHeight += screenViewSize.height;
        }
        myScreenY = Math.max((availableHeight - requiredHeight) / 2, RULER_SIZE_PX + DEFAULT_SCREEN_OFFSET_Y);
      }
      else {
        if (myDeviceFrames) {
          myScreenX = RULER_SIZE_PX + 2 * DEFAULT_SCREEN_OFFSET_X;
          myScreenY = RULER_SIZE_PX + 2 * DEFAULT_SCREEN_OFFSET_Y;
        }
        else {
          myScreenX = RULER_SIZE_PX + DEFAULT_SCREEN_OFFSET_X;
          myScreenY = RULER_SIZE_PX + DEFAULT_SCREEN_OFFSET_Y;
        }
      }
    }
    myScreenView.setLocation(myScreenX, myScreenY);

    // Position blueprint view
    if (myBlueprintView != null) {

      if (myStackVertically) {
        // top/bottom stacking
        myBlueprintView.setLocation(myScreenX, myScreenY + screenViewSize.height + SCREEN_DELTA);
      }
      else {
        // left/right ordering
        myBlueprintView.setLocation(myScreenX + screenViewSize.width + SCREEN_DELTA, myScreenY);
      }
    }
    if (myScreenView != null) {
      Scene scene = myScreenView.getScene();
      scene.needsRebuildList();
    }
    if (myBlueprintView != null) {
      Scene scene = myBlueprintView.getScene();
      scene.needsRebuildList();
    }
  }

  @Override
  @SwingCoordinate
  protected int getContentOriginX() {
    return myScreenX;
  }

  @Override
  @SwingCoordinate
  protected int getContentOriginY() {
    return myScreenY;
  }

  public boolean isStackVertically() {
    return myStackVertically;
  }

  @Override
  protected void doCreateSceneViews() {
    myLayers.clear();

    myScreenView = null;
    myBlueprintView = null;

    if (myModel == null) {
      return;
    }

    NlLayoutType type = myModel.getType();

    if (type.equals(NlLayoutType.MENU)) {
      doCreateSceneViewsForMenu();
      return;
    }

    if (type.equals(NlLayoutType.PREFERENCE_SCREEN)) {
      myScreenMode = ScreenMode.SCREEN_ONLY;
    }

    switch (myScreenMode) {
      case SCREEN_ONLY:
        myScreenView = new ScreenView(this, ScreenViewType.NORMAL, myModel);
        break;
      case BLUEPRINT_ONLY:
        myScreenView = new ScreenView(this, ScreenViewType.BLUEPRINT, myModel);
        break;
      case BOTH:
        myScreenView = new ScreenView(this, ScreenViewType.NORMAL, myModel);

        myBlueprintView = new ScreenView(this, ScreenViewType.BLUEPRINT, myModel);
        myBlueprintView.setLocation(myScreenX + myScreenView.getPreferredSize().width + 10, myScreenY);

        break;
    }

    updateErrorDisplay();
    getLayeredPane().setPreferredSize(myScreenView.getPreferredSize());

    addLayers();
    layoutContent();
  }

  private void doCreateSceneViewsForMenu() {
    myScreenMode = ScreenMode.SCREEN_ONLY;
    XmlTag tag = myModel.getFile().getRootTag();

    // TODO See if there's a better way to trigger the NavigationViewSceneView. Perhaps examine the view objects?
    if (tag != null && Objects.equals(tag.getAttributeValue(ATTR_SHOW_IN, TOOLS_URI), NavigationViewSceneView.SHOW_IN_ATTRIBUTE_VALUE)) {
      myScreenView = new NavigationViewSceneView(this, myModel);
      myLayers.add(new ScreenViewLayer(myScreenView));
    }
    else {
      myScreenView = new ScreenView(this, ScreenViewType.NORMAL, myModel);
      addScreenLayers();
    }

    updateErrorDisplay();
    getLayeredPane().setPreferredSize(myScreenView.getPreferredSize());
    layoutContent();
  }

  @Override
  @NotNull
  public Dimension getContentSize(@Nullable Dimension dimension) {
    if (dimension == null) {
      dimension = new Dimension();
    }
    if (myScreenMode == ScreenMode.BOTH
        && myScreenView != null && myBlueprintView != null) {
      if (isStackVertically()) {
        dimension.setSize(
          myScreenView.getSize().getWidth(),
          myScreenView.getSize().getHeight() + myBlueprintView.getSize().getHeight()
        );
      }
      else {
        dimension.setSize(
          myScreenView.getSize().getWidth() + myBlueprintView.getSize().getWidth(),
          myScreenView.getSize().getHeight()
        );
      }
    }
    else if (getCurrentSceneView() != null) {
      dimension.setSize(
        getCurrentSceneView().getSize().getWidth(),
        getCurrentSceneView().getSize().getHeight());
    }
    return dimension;
  }

  @Override
  protected Dimension getDefaultOffset() {
    return new Dimension(2 * DEFAULT_SCREEN_OFFSET_X + RULER_SIZE_PX, 2 * DEFAULT_SCREEN_OFFSET_Y + RULER_SIZE_PX);
  }

  @SwingCoordinate
  @Override
  protected Dimension getPreferredContentSize(int availableWidth, int availableHeight) {
    assert myScreenView != null;
    Dimension preferredSize = myScreenView.getPreferredSize();

    int requiredWidth = preferredSize.width;
    int requiredHeight = preferredSize.height;
    if (myScreenMode == ScreenMode.BOTH) {
      if (isVerticalScreenConfig(availableWidth, availableHeight, preferredSize)) {
        requiredHeight *= 2;
        requiredHeight += SCREEN_DELTA;
      }
      else {
        requiredWidth *= 2;
        requiredWidth += SCREEN_DELTA;
      }
    }

    return new Dimension(requiredWidth, requiredHeight);
  }

  @Override
  public void notifyComponentActivate(@NotNull NlComponent component) {
    ViewHandler handler = component.getViewHandler();
    ViewEditor editor = getViewEditor();

    if (handler != null && editor != null) {
      handler.onActivateInComponentTree(editor, component);
    }

    super.notifyComponentActivate(component);
  }

  @Override
  public void notifyComponentActivate(@NotNull NlComponent component, int x, int y) {
    ViewHandler handler = component.getViewHandler();
    ViewEditor editor = getViewEditor();

    if (handler != null && editor != null) {
      handler.onActivateInDesignSurface(editor, component, x, y);
    }
    super.notifyComponentActivate(component, x, y);
  }

  private class MyBottomLayer extends Layer {

    private boolean myPaintedFrame;

    @Override
    public void paint(@NotNull Graphics2D g2d) {
      Composite oldComposite = g2d.getComposite();

      assert myScreenView != null;
      RenderResult result = myScreenView.getResult();

      myPaintedFrame = false;
      if (myDeviceFrames && result != null && result.hasImage()) {
        Configuration configuration = myScreenView.getConfiguration();
        Device device = configuration.getDevice();
        State deviceState = configuration.getDeviceState();
        DeviceArtPainter painter = DeviceArtPainter.getInstance();
        if (device != null && painter.hasDeviceFrame(device) && deviceState != null) {
          myPaintedFrame = true;
          g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
          painter.paintFrame(g2d, device, deviceState.getOrientation(), true, myScreenX, myScreenY,
                             (int)(myScale * result.getRenderedImage().getHeight()));
        }
      }

      g2d.setComposite(oldComposite);

      if (!getLayoutType().isSupportedByDesigner()) {
        return;
      }

      if (!myPaintedFrame) {
        // Only show bounds dashed lines when there's no device
        paintBorder(g2d);
      }
    }

    private void paintBorder(Graphics2D g2d) {
      if (myScreenView == null) {
        return;
      }

      Shape screenShape = myScreenView.getScreenShape();
      if (screenShape != null) {
        g2d.draw(screenShape);
        return;
      }

      ScreenView.BorderPainter.paint(g2d, myScreenView);
      if (myScreenMode == ScreenMode.BOTH) {
        ScreenView.BorderPainter.paint(g2d, myBlueprintView);
      }
    }
  }

  public void setMockupVisible(boolean mockupVisible) {
    myMockupVisible = mockupVisible;
    repaint();
  }

  public boolean isMockupVisible() {
    return myMockupVisible;
  }

  public void setMockupEditor(@Nullable MockupEditor mockupEditor) {
    myMockupEditor = mockupEditor;
  }

  @Nullable
  public MockupEditor getMockupEditor() {
    return myMockupEditor;
  }

  private void setPanZoomPanel(@Nullable PanZoomPanel panZoomPanel) {
    myPanZoomPanel = new WeakReference<>(panZoomPanel);
  }

  @Nullable
  public PanZoomPanel getPanZoomPanel() {
    return myPanZoomPanel.get();
  }

  /**
   * Shows the {@link PanZoomPanel} if the {@link PropertiesComponent} {@link PanZoomPanel#PROP_OPEN} is true
   */
  private void showPanZoomPanelIfRequired() {
    if (PanZoomPanel.isPropertyComponentOpen()) {
      setPanZoomPanelVisible(true);
    }
  }

  /**
   * If show is true, displays the {@link PanZoomPanel}.
   *
   * If the {@link DesignSurface} is not shows yet, it register a callback that will show the {@link PanZoomPanel}
   * once the {@link DesignSurface} is visible, otherwise it shows it directly.
   *
   * @param show
   */
  public void setPanZoomPanelVisible(boolean show) {
    PanZoomPanel panel = myPanZoomPanel.get();
    if (show) {
      if (panel == null) {
        panel = new PanZoomPanel(this);
      }
      setPanZoomPanel(panel);
      if (isShowing()) {
        panel.showPopup();
      }
      else {
        PanZoomPanel finalPanel = panel;
        ComponentAdapter adapter = new ComponentAdapter() {
          @Override
          public void componentShown(ComponentEvent e) {
            finalPanel.showPopup();
            removeComponentListener(this);
          }
        };
        addComponentListener(adapter);
      }
    }
    else if (panel != null) {
      panel.closePopup();
    }
  }


  /**
   * Notifies the design surface that the given screen view (which must be showing in this design surface)
   * has been rendered (possibly with errors)
   */
  public void updateErrorDisplay() {
    assert ApplicationManager.getApplication().isDispatchThread() ||
           !ApplicationManager.getApplication().isReadAccessAllowed() : "Do not hold read lock when calling updateErrorDisplay!";

    getErrorQueue().cancelAllUpdates();
    getErrorQueue().queue(new Update("errors") {
      @Override
      public void run() {
        // Look up *current* result; a newer one could be available
        final RenderResult result = getCurrentSceneView() != null ? getCurrentSceneView().getResult() : null;
        if (result == null) {
          return;
        }

        BuildMode gradleBuildMode = BuildSettings.getInstance(getProject()).getBuildMode();
        RenderErrorModel model = gradleBuildMode != null && result.getLogger().hasErrors()
                                 ? RenderErrorModel.STILL_BUILDING_ERROR_MODEL
                                 : RenderErrorModelFactory
                                   .createErrorModel(result, DataManager.getInstance().getDataContext(getIssuePanel()));
        getIssueModel().setRenderErrorModel(model);
      }

      @Override
      public boolean canEat(Update update) {
        return true;
      }
    });
  }

  @Override
  protected void modelRendered(@NotNull NlModel model) {
    if (getCurrentSceneView() != null) {
      updateErrorDisplay();
    }
    super.modelRendered(model);
  }

  @Override
  protected boolean useSmallProgressIcon() {
    return getCurrentSceneView() != null && getCurrentSceneView().getResult() != null;
  }
}
