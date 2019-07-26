/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.impl.parser;

import com.intellij.ide.plugins.PluginBean;
import com.intellij.ide.plugins.PluginDependency;
import com.intellij.ide.plugins.PluginHelpSet;
import com.intellij.ide.plugins.PluginVendor;
import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.util.text.StringUtilRt;
import consulo.util.nodep.xml.node.SimpleXmlElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author VISTALL
 * @since 2019-03-24
 */
public class PluginBeanParser {
  private static Set<String> ourAllowedRootTags = new HashSet<String>(Arrays.asList("consulo-plugin", "idea-plugin"));

  @Nullable
  public static PluginBean parseBean(@Nonnull SimpleXmlElement rootTag, @Nullable String pluginId) {
    String rootTagName = rootTag.getName();
    if (!ourAllowedRootTags.contains(rootTagName)) {
      return null;
    }

    PluginBean pluginBean = new PluginBean();
    pluginBean.id = rootTag.getChildText("id");
    if (StringUtilRt.isEmpty(pluginBean.id)) {
      pluginBean.id = pluginId;
    }

    pluginBean.name = rootTag.getChildText("name");
    pluginBean.description = rootTag.getChildText("description");
    pluginBean.pluginVersion = rootTag.getChildText("version");
    pluginBean.platformVersion = rootTag.getChildText("platformVersion");
    pluginBean.category = rootTag.getChildText("category");
    pluginBean.resourceBundle = rootTag.getChildText("resource-bundle");
    pluginBean.changeNotes = rootTag.getChildText("change-notes");
    pluginBean.url = rootTag.getAttributeValue("url");

    List<SimpleXmlElement> anImport = rootTag.getChildren("import");
    if (!anImport.isEmpty()) {
      List<String> imports = new ArrayList<String>();
      for (SimpleXmlElement element : anImport) {
        imports.add(element.getAttributeValue("path"));
      }
      pluginBean.imports = imports;
    }

    pluginBean.extensionPoints = expandChildren(rootTag, "extensionPoints");

    List<SimpleXmlElement> extensionsTag = rootTag.getChildren("extensions");

    List<ExtensionInfo> extensionInfos = new ArrayList<ExtensionInfo>();

    for (SimpleXmlElement extensionElement : extensionsTag) {
      // FIXME [VISTALL] rename later to pluginId?
      String defaultExtensionNs = extensionElement.getAttributeValue("defaultExtensionNs");
      if (defaultExtensionNs == null) {
        defaultExtensionNs = pluginBean.id;
      }

      for (SimpleXmlElement childExtension : extensionElement.getChildren()) {
        extensionInfos.add(new ExtensionInfo(defaultExtensionNs, childExtension));
      }
    }

    pluginBean.extensions = extensionInfos;

    pluginBean.actions = expandChildren(rootTag, "actions");

    SimpleXmlElement vendorElement = rootTag.getChild("vendor");
    if (vendorElement != null) {
      PluginVendor vendor = new PluginVendor();
      vendor.name = vendorElement.getText();
      vendor.url = vendorElement.getAttributeValue("url");
      vendor.email = vendorElement.getAttributeValue("email");

      pluginBean.vendor = vendor;
    }

    List<PluginHelpSet> pluginHelpSets = new ArrayList<PluginHelpSet>();
    for (SimpleXmlElement helpsetTag : rootTag.getChildren("helpset")) {
      PluginHelpSet pluginHelpSet = new PluginHelpSet();
      pluginHelpSet.file = helpsetTag.getAttributeValue("file");
      pluginHelpSet.path = helpsetTag.getAttributeValue("path");

      pluginHelpSets.add(pluginHelpSet);
    }

    if (!pluginHelpSets.isEmpty()) {
      pluginBean.helpSets = pluginHelpSets;
    }

    List<PluginDependency> pluginDependencies = new ArrayList<PluginDependency>();
    for (SimpleXmlElement dependsElement : rootTag.getChildren("depends")) {
      PluginDependency pluginDependency = new PluginDependency();
      pluginDependency.configFile = dependsElement.getAttributeValue("config-file");
      pluginDependency.optional = Boolean.parseBoolean(dependsElement.getAttributeValue("config-file", "false"));
      pluginDependency.pluginId = dependsElement.getText();
    }

    if (!pluginDependencies.isEmpty()) {
      pluginBean.dependencies = pluginDependencies;
    }

    // region deprecated stuff
    List<ComponentConfig> appComponents = new ArrayList<ComponentConfig>();
    for (SimpleXmlElement appComponentElements : rootTag.getChildren("application-components")) {
      parseComponent(appComponentElements, appComponents);
    }

    if (!appComponents.isEmpty()) {
      pluginBean.applicationComponents = appComponents;
    }

    List<ComponentConfig> projectComponents = new ArrayList<ComponentConfig>();
    for (SimpleXmlElement appComponentElements : rootTag.getChildren("project-components")) {
      parseComponent(appComponentElements, projectComponents);
    }

    if (!projectComponents.isEmpty()) {
      pluginBean.projectComponents = projectComponents;
    }

    // endregion

    return pluginBean;
  }

  @Nonnull
  private static List<SimpleXmlElement> expandChildren(SimpleXmlElement element, String childTag) {
    List<SimpleXmlElement> list = Collections.emptyList();

    for (SimpleXmlElement child : element.getChildren(childTag)) {
      List<SimpleXmlElement> children = child.getChildren();
      if (!children.isEmpty()) {
        if (list.isEmpty()) {
          list = new ArrayList<SimpleXmlElement>();
        }

        list.addAll(children);
      }
    }

    return list;
  }

  private static void parseComponent(SimpleXmlElement componentsParent, List<ComponentConfig> configConsumer) {
    for (SimpleXmlElement componentElement : componentsParent.getChildren("component")) {
      ComponentConfig componentConfig = new ComponentConfig();
      componentConfig.setHeadlessImplementationClass(componentElement.getChildText("headless-implementation-class"));
      componentConfig.setImplementationClass(componentElement.getChildText("implementation-class"));
      componentConfig.setInterfaceClass(componentElement.getChildText("interface-class"));

      for (SimpleXmlElement optionElement : componentElement.getChildren("option")) {
        String name = optionElement.getAttributeValue("name");
        String value = optionElement.getAttributeValue("value");

        componentConfig.options.put(name, value);
      }

      configConsumer.add(componentConfig);
    }
  }
}
