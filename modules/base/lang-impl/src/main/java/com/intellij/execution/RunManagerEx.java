/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class RunManagerEx extends RunManager {
  public static RunManagerEx getInstanceEx(final Project project) {
    return (RunManagerEx)project.getComponent(RunManager.class);
  }

  //public abstract boolean isTemporary(@NotNull RunnerAndConfigurationSettings configuration);

  /**
   * @deprecated use {@link #setSelectedConfiguration(RunnerAndConfigurationSettings)} instead
   */
  @Deprecated
  public void setActiveConfiguration(@Nullable RunnerAndConfigurationSettings configuration) {
    setSelectedConfiguration(configuration);
  }

  public abstract void setTemporaryConfiguration(@Nullable RunnerAndConfigurationSettings tempConfiguration);

  public abstract RunManagerConfig getConfig();

  /**
   * @param name
   * @param type
   * @return
   * @deprecated use {@link RunManager#createRunConfiguration(String, ConfigurationFactory)} instead
   */
  @Nonnull
  public abstract RunnerAndConfigurationSettings createConfiguration(String name, ConfigurationFactory type);

  public abstract void addConfiguration(RunnerAndConfigurationSettings settings, boolean isShared, List<BeforeRunTask> tasks, boolean addTemplateTasksIfAbsent);

  public abstract boolean isConfigurationShared(RunnerAndConfigurationSettings settings);

  @Nonnull
  public abstract List<BeforeRunTask> getBeforeRunTasks(RunConfiguration settings);

  public abstract void setBeforeRunTasks(RunConfiguration runConfiguration, List<BeforeRunTask> tasks, boolean addEnabledTemplateTasksIfAbsent);

  @Nonnull
  public abstract <T extends BeforeRunTask> List<T> getBeforeRunTasks(RunConfiguration settings, Key<T> taskProviderID);

  @Nonnull
  public abstract <T extends BeforeRunTask> List<T> getBeforeRunTasks(Key<T> taskProviderID);

  public abstract RunnerAndConfigurationSettings findConfigurationByName(@Nullable final String name);

  @Nonnull
  public abstract Image getConfigurationIcon(@Nonnull RunnerAndConfigurationSettings settings);

  @Nonnull
  public abstract Collection<RunnerAndConfigurationSettings> getSortedConfigurations();

  public abstract void removeConfiguration(@Nullable RunnerAndConfigurationSettings settings);

  /**
   * @deprecated Use {@link RunManagerListener#TOPIC} instead.
   */
  @Deprecated
  public void addRunManagerListener(RunManagerListener listener) {
  }

  @Nonnull
  public abstract Map<String, List<RunnerAndConfigurationSettings>> getStructure(@Nonnull ConfigurationType type);

  public static void disableTasks(Project project, RunConfiguration settings, Key<? extends BeforeRunTask>... keys) {
    for (Key<? extends BeforeRunTask> key : keys) {
      List<? extends BeforeRunTask> tasks = getInstanceEx(project).getBeforeRunTasks(settings, key);
      for (BeforeRunTask task : tasks) {
        task.setEnabled(false);
      }
    }
  }

  public static int getTasksCount(Project project, RunConfiguration settings, Key<? extends BeforeRunTask>... keys) {
    return Arrays.stream(keys).mapToInt(key -> getInstanceEx(project).getBeforeRunTasks(settings, key).size()).sum();
  }
}