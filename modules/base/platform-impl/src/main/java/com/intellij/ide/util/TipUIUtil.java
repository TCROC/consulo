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
package com.intellij.ide.util;

import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.ui.LafManager;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.impl.DefaultKeymap;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ResourceUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ide.util.URLDictionatyLoader;
import consulo.ui.style.StyleManager;
import consulo.wm.util.IdeFrameUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

/**
 * @author dsl
 * @author Konstantin Bulenkov
 */
public class TipUIUtil {
  private static final Logger LOG = Logger.getInstance(TipUIUtil.class);
  private static final String SHORTCUT_ENTITY = "&shortcut:";

  private TipUIUtil() {
  }

  @Nonnull
  public static String getPoweredByText(@Nonnull TipAndTrickBean tip) {
    IdeaPluginDescriptor descriptor = tip.getPluginDescriptor();
    return !PluginManagerCore.CORE_PLUGIN_ID.equals(descriptor.getPluginId().getIdString()) ? descriptor.getName() : "";
  }

  public static void openTipInBrowser(String tipFileName, JEditorPane browser, Class providerClass) {
    TipAndTrickBean tip = TipAndTrickBean.findByFileName(tipFileName);
    if (tip == null && StringUtil.isNotEmpty(tipFileName)) {
      tip = new TipAndTrickBean();
      tip.fileName = tipFileName;
    }
    openTipInBrowser(tip, browser);
  }

  public static void openTipInBrowser(@Nullable TipAndTrickBean tip, JEditorPane browser) {
    if (tip == null) return;
    try {
      IdeaPluginDescriptor pluginDescriptor = tip.getPluginDescriptor();
      ClassLoader tipLoader = pluginDescriptor == null ? TipUIUtil.class.getClassLoader() : ObjectUtils.notNull(pluginDescriptor.getPluginClassLoader(), TipUIUtil.class.getClassLoader());

      URL url = ResourceUtil.getResource(tipLoader, "/tips/", tip.fileName);

      if (url == null) {
        setCantReadText(browser, tip);
        return;
      }

      StringBuilder text = new StringBuilder(ResourceUtil.loadText(url));
      updateShortcuts(text);
      updateImages(text, tipLoader, browser);
      String replaced = text.toString().replace("&productName;", ApplicationNamesInfo.getInstance().getFullProductName());
      String major = ApplicationInfo.getInstance().getMajorVersion();
      replaced = replaced.replace("&majorVersion;", major);
      String minor = ApplicationInfo.getInstance().getMinorVersion();
      replaced = replaced.replace("&minorVersion;", minor);
      replaced = replaced.replace("&majorMinorVersion;", major + ("0".equals(minor) ? "" : ("." + minor)));
      replaced = replaced.replace("&settingsPath;", CommonBundle.settingsActionPath());
      replaced = replaced.replaceFirst("<link rel=\"stylesheet\".*tips\\.css\">", ""); // don't reload the styles
      if (browser.getUI() == null) {
        browser.updateUI();
        boolean succeed = browser.getUI() != null;
        String message = "reinit JEditorPane.ui: " + (succeed ? "OK" : "FAIL") + ", laf=" + LafManager.getInstance().getCurrentLookAndFeel();
        if (succeed) {
          LOG.warn(message);
        }
        else {
          LOG.error(message);
        }
      }
      browser.read(new StringReader(replaced), url);
    }
    catch (IOException e) {
      setCantReadText(browser, tip);
    }
  }

  private static void setCantReadText(JEditorPane browser, TipAndTrickBean bean) {
    try {
      String plugin = getPoweredByText(bean);
      String product = ApplicationNamesInfo.getInstance().getFullProductName();
      if (!plugin.isEmpty()) {
        product += " and " + plugin + " plugin";
      }
      String message = IdeBundle.message("error.unable.to.read.tip.of.the.day", bean.fileName, product);
      browser.read(new StringReader(message), null);
    }
    catch (IOException ignored) {
    }
  }

  private static void updateImages(StringBuilder text, ClassLoader tipLoader, JEditorPane browser) {
    final boolean dark = StyleManager.get().getCurrentStyle().isDark();

    IdeFrame af = IdeFrameUtil.findActiveRootIdeFrame();
    Component comp = af != null ? TargetAWT.to(af.getWindow()) : browser;
    int index = text.indexOf("<img", 0);
    while (index != -1) {
      final int end = text.indexOf(">", index + 1);
      if (end == -1) return;
      final String img = text.substring(index, end + 1).replace('\r', ' ').replace('\n', ' ');
      final int srcIndex = img.indexOf("src=");
      final int endIndex = img.indexOf(".png", srcIndex);
      if (endIndex != -1) {
        String path = img.substring(srcIndex + 5, endIndex);
        if (!path.endsWith("_dark") && !path.endsWith("@2x")) {
          boolean hidpi = JBUI.isPixHiDPI(comp);
          path += (hidpi ? "@2x" : "") + (dark ? "_dark" : "") + ".png";
          URL url = ResourceUtil.getResource(tipLoader, "/tips/", path);
          if (url != null) {
            String newImgTag = "<img src=\"" + path + "\" ";
            try {
              BufferedImage image = ImageIO.read(url.openStream());
              int w = image.getWidth();
              int h = image.getHeight();
              if (UIUtil.isJreHiDPI(comp)) {
                // compensate JRE scale
                float sysScale = JBUI.sysScale(comp);
                w = (int)(w / sysScale);
                h = (int)(h / sysScale);
              }
              else {
                // compensate image scale
                float imgScale = hidpi ? 2f : 1f;
                w = (int)(w / imgScale);
                h = (int)(h / imgScale);
              }
              // fit the user scale
              w = (int)(JBUI.scale((float)w));
              h = (int)(JBUI.scale((float)h));

              newImgTag += "width=\"" + w + "\" height=\"" + h + "\"";
            }
            catch (Exception ignore) {
              newImgTag += "width=\"400\" height=\"200\"";
            }
            newImgTag += "/>";
            text.replace(index, end + 1, newImgTag);
          }
        }
      }
      index = text.indexOf("<img", index + 1);
    }
  }

  private static void updateShortcuts(StringBuilder text) {
    int lastIndex = 0;
    while (true) {
      lastIndex = text.indexOf(SHORTCUT_ENTITY, lastIndex);
      if (lastIndex < 0) return;
      final int actionIdStart = lastIndex + SHORTCUT_ENTITY.length();
      int actionIdEnd = text.indexOf(";", actionIdStart);
      if (actionIdEnd < 0) {
        return;
      }
      final String actionId = text.substring(actionIdStart, actionIdEnd);
      String shortcutText = getShortcutText(actionId, KeymapManager.getInstance().getActiveKeymap());
      if (shortcutText == null) {
        Keymap defKeymap = KeymapManager.getInstance().getKeymap(DefaultKeymap.getInstance().getDefaultKeymapName());
        if (defKeymap != null) {
          shortcutText = getShortcutText(actionId, defKeymap);
          if (shortcutText != null) {
            shortcutText += " in default keymap";
          }
        }
      }
      if (shortcutText == null) {
        shortcutText = "<no shortcut for action " + actionId + ">";
      }
      text.replace(lastIndex, actionIdEnd + 1, shortcutText);
      lastIndex += shortcutText.length();
    }
  }

  @Nullable
  private static String getShortcutText(String actionId, Keymap keymap) {
    for (final Shortcut shortcut : keymap.getShortcuts(actionId)) {
      if (shortcut instanceof KeyboardShortcut) {
        return KeymapUtil.getShortcutText(shortcut);
      }
    }
    return null;
  }

  @Nonnull
  public static JEditorPane createTipBrowser() {
    JEditorPane browser = new JEditorPane() {
      @Override
      public void setDocument(Document document) {
        super.setDocument(document);
        document.putProperty("imageCache", new URLDictionatyLoader());
      }
    };
    browser.setEditable(false);
    browser.setBackground(UIUtil.getTextFieldBackground());
    browser.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        BrowserUtil.browse(e.getURL());
      }
    });
    URL resource = ResourceUtil.getResource(TipUIUtil.class, "/tips/css/", UIUtil.isUnderDarcula() ? "tips_darcula.css" : "tips.css");
    HTMLEditorKit kit = UIUtil.getHTMLEditorKit(false);
    kit.getStyleSheet().addStyleSheet(UIUtil.loadStyleSheet(resource));
    browser.setEditorKit(kit);
    return browser;
  }
}