/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ide.plugins;

import com.intellij.ide.plugins.sorters.SortByDownloadsAction;
import com.intellij.ide.plugins.sorters.SortByRatingAction;
import com.intellij.ide.plugins.sorters.SortByUpdatedAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.container.plugin.PluginDescriptor;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * User: anna
 */
public class AvailablePluginsManagerMain extends PluginManagerMain {
  public static final String MANAGE_REPOSITORIES = "Manage repositories...";
  public static final String N_A = "N/A";

  public AvailablePluginsManagerMain() {
    super();
    init();
    /*final JButton manageRepositoriesBtn = new JButton(MANAGE_REPOSITORIES);
    if (myVendorFilter == null) {
      manageRepositoriesBtn.setMnemonic('m');
      manageRepositoriesBtn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (ShowSettingsUtil.getInstance().editConfigurable(myActionsPanel, new PluginHostsConfigurable())) {
            final List<String> pluginHosts = UpdateSettings.getInstance().getStoredPluginHosts();
            if (!pluginHosts.contains(((AvailablePluginsTableModel)pluginsModel).getRepository())) {
              ((AvailablePluginsTableModel)pluginsModel).setRepository(AvailablePluginsTableModel.ALL, myFilter.getFilter().toLowerCase());
            }
            loadAvailablePlugins();
          }
        }
      });
      myActionsPanel.add(manageRepositoriesBtn, BorderLayout.EAST);
    } */

   /* final JButton httpProxySettingsButton = new JButton(IdeBundle.message("button.http.proxy.settings"));
    httpProxySettingsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (HttpConfigurable.editConfigurable(getMainPanel())) {
          loadAvailablePlugins();
        }
      }
    });
    myActionsPanel.add(httpProxySettingsButton, BorderLayout.WEST);    */
  }

  @Nonnull
  @Override
  protected PluginTable createTable() {
    myPluginsModel = new AvailablePluginsTableModel();
    PluginTable table = new PluginTable(myPluginsModel);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    return table;
  }

  @Override
  protected void addCustomFilters(Consumer<JComponent> adder) {
    LabelPopup categoryPopup = new LabelPopup("Category:", this::createCategoryFilters);

    adder.accept(categoryPopup);

    updateCategoryPopup(categoryPopup);
  }

  private void updateCategoryPopup(LabelPopup labelPopup) {
    String category = ((AvailablePluginsTableModel)myPluginsModel).getCategory();
    if (category == null) {
      category = N_A;
    }
    labelPopup.setPrefixedText(category);
  }

  @Nonnull
  private DefaultActionGroup createCategoryFilters(LabelPopup labelPopup) {
    final TreeSet<String> availableCategories = ((AvailablePluginsTableModel)myPluginsModel).getAvailableCategories();
    final DefaultActionGroup gr = new DefaultActionGroup();
    gr.addAction(createFilterByCategoryAction(AvailablePluginsTableModel.ALL, labelPopup));
    final boolean noCategory = availableCategories.remove(N_A);
    for (final String availableCategory : availableCategories) {
      gr.addAction(createFilterByCategoryAction(availableCategory, labelPopup));
    }
    if (noCategory) {
      gr.addAction(createFilterByCategoryAction(N_A, labelPopup));
    }
    return gr;
  }

  private AnAction createFilterByCategoryAction(final String availableCategory, LabelPopup labelPopup) {
    return new DumbAwareAction(availableCategory) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        final String filter = myFilter.getFilter().toLowerCase(Locale.ROOT);
        ((AvailablePluginsTableModel)myPluginsModel).setCategory(availableCategory, filter);
        updateCategoryPopup(labelPopup);
      }
    };
  }

  @Override
  protected DefaultActionGroup createSortersGroup() {
    final DefaultActionGroup group = super.createSortersGroup();
    group.addAction(new SortByDownloadsAction(myPluginTable, myPluginsModel));
    group.addAction(new SortByRatingAction(myPluginTable, myPluginsModel));
    group.addAction(new SortByUpdatedAction(myPluginTable, myPluginsModel));
    return group;
  }

  @Override
  public void reset() {
    UiNotifyConnector.doWhenFirstShown(getPluginTable(), this::loadAvailablePlugins);
    super.reset();
  }

  @Override
  public ActionGroup getActionGroup() {
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.addAction(new RefreshAction());
    return actionGroup;
  }

  @Override
  protected void propagateUpdates(List<PluginDescriptor> list) {
    getInstalled().modifyPluginsList(list);
  }
}
