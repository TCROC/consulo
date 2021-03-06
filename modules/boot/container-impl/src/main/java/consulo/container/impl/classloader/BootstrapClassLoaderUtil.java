/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.container.impl.classloader;

import consulo.container.StartupError;
import consulo.container.boot.ContainerStartup;
import consulo.container.boot.internal.PathManagerHolder;
import consulo.container.impl.ContainerLogger;
import consulo.container.impl.PluginDescriptorImpl;
import consulo.container.impl.PluginDescriptorLoader;
import consulo.container.impl.PluginHolderModificator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.util.StatCollector;
import consulo.util.nodep.SystemInfoRt;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * @author max
 */
public class BootstrapClassLoaderUtil {
  private static final String CONSULO_PLATFORM_BASE = "consulo.platform.base";

  @Nonnull
  public static ContainerStartup buildContainerStartup(Map<String, Object> args, File modulesDirectory, ContainerLogger containerLogger) throws Exception {
    StatCollector stat = (StatCollector)args.get(ContainerStartup.STAT_COLLECTOR);

    Runnable bootInitialize = stat.mark("boot.classloader.initialize");

    Runnable mark = stat.mark(CONSULO_PLATFORM_BASE + ".initalize");
    PluginDescriptorImpl base = initalizePlatformBase(modulesDirectory, containerLogger);
    mark.run();

    List<PluginDescriptorImpl> descriptors = new ArrayList<PluginDescriptorImpl>();
    descriptors.add(base);

    File[] files = modulesDirectory.listFiles();
    assert files != null;
    for (File moduleDirectory : files) {
      if (CONSULO_PLATFORM_BASE.equals(moduleDirectory.getName())) {
        continue;
      }

      mark = stat.mark(moduleDirectory.getName() + ".load");
      PluginDescriptorImpl descriptor = PluginDescriptorLoader.loadDescriptor(moduleDirectory, false, true, containerLogger);

      if (descriptor == null) {
        mark.run();
        continue;
      }

      ClassLoader[] parentClassLoaders = {base.getPluginClassLoader()};

      ClassLoader loader = PluginClassLoaderFactory.create(filesToUrls(descriptor.getClassPath()), parentClassLoaders, descriptor.getPluginId(), null, moduleDirectory);

      if (SystemInfoRt.IS_AT_LEAST_JAVA9) {
        descriptor.setModuleLayer(Java9ModuleInitializer.initializeEtcModules(Collections.singletonList(base.getModuleLayer()), descriptor.getClassPath(), loader));
      }

      descriptors.add(descriptor);

      descriptor.setLoader(loader);
      mark.run();
    }

    PluginHolderModificator.initialize(descriptors);

    PluginLoadStatistics.initialize(false);

    for (PluginDescriptor pluginDescriptor : PluginHolderModificator.getPlugins()) {
      ServiceLoader<ContainerStartup> loader = ServiceLoader.load(ContainerStartup.class, pluginDescriptor.getPluginClassLoader());

      Iterator<ContainerStartup> iterator = loader.iterator();

      if (iterator.hasNext()) {
        bootInitialize.run();

        ContainerStartup startup = iterator.next();

        PathManagerHolder.setInstance(startup.createPathManager(args));

        return startup;
      }
    }

    throw new StartupError("Instance of ContainerStartup not found");
  }

  @Nonnull
  private static PluginDescriptorImpl initalizePlatformBase(File modulesDirectory, ContainerLogger containerLogger) throws Exception {
    File platformBaseDirectory = new File(modulesDirectory, CONSULO_PLATFORM_BASE);

    PluginDescriptorImpl platformBasePlugin = PluginDescriptorLoader.loadDescriptor(platformBaseDirectory, false, true, containerLogger);

    if (platformBasePlugin == null) {
      throw new StartupError("No base module. Broken installation");
    }

    ClassLoader[] parentClassLoaders = getParentClassLoaders();

    ClassLoader loader = PluginClassLoaderFactory.create(filesToUrls(platformBasePlugin.getClassPath()), parentClassLoaders, platformBasePlugin.getPluginId(), null, platformBaseDirectory);

    if (SystemInfoRt.IS_AT_LEAST_JAVA9) {
      platformBasePlugin.setModuleLayer(Java9ModuleInitializer.initializeBaseModules(platformBasePlugin.getClassPath(), loader, containerLogger));
    }

    platformBasePlugin.setLoader(loader);

    Thread.currentThread().setContextClassLoader(loader);

    return platformBasePlugin;
  }

  private static List<URL> filesToUrls(List<File> files) throws Exception {
    List<URL> urls = new ArrayList<URL>(files.size());

    for (int i = 0; i < files.size(); i++) {
      urls.add(files.get(i).toURI().toURL());
    }
    return urls;
  }

  @Nonnull
  public static ClassLoader[] getParentClassLoaders() {
    // in ideal world we don't need this (after migration to java9 also)
    // tools jar we don't use in java plugin
    // without this code, it will throw access error to java.sql module
    // in java 9 we need add requires to java.sql and we can remove this hack
    if (SystemInfoRt.IS_AT_LEAST_JAVA9) {
      // on Java 8, 'tools.jar' is on a classpath; on Java 9, its classes are available via the platform loader
      try {
        ClassLoader platformCl = (ClassLoader)ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
        return new ClassLoader[]{platformCl, BootstrapClassLoaderUtil.class.getClassLoader()};
      }
      catch (Exception e) {
        throw new StartupError(e);
      }
    }
    else {
      ClassLoader classLoader = BootstrapClassLoaderUtil.class.getClassLoader();
      return new ClassLoader[]{classLoader};
    }
  }


  @Nonnull
  public static File getModulesDirectory() throws Exception {
    Class<BootstrapClassLoaderUtil> aClass = BootstrapClassLoaderUtil.class;

    URL url = aClass.getResource("/" + aClass.getName().replace('.', '/') + ".class");

    String file = url.getFile();

    int i = file.indexOf("!/");
    if (i == -1) {
      throw new IllegalArgumentException("Wrong path: " + file);
    }

    String jarUrlPath = file.substring(0, i);

    File jarFile = new File(new URL(jarUrlPath).toURI().getSchemeSpecificPart());

    File bootDirectory = jarFile.getParentFile();

    return new File(bootDirectory.getParentFile(), "modules");
  }
}