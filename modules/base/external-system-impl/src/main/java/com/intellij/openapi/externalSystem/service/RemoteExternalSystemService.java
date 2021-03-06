package com.intellij.openapi.externalSystem.service;

import com.intellij.openapi.externalSystem.service.internal.ExternalSystemTaskAware;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import javax.annotation.Nonnull;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Generic interface with common functionality for all remote services that work with external system.
 * 
 * @author Denis Zhdanov
 * @since 8/9/11 3:19 PM
 */
public interface RemoteExternalSystemService<S extends ExternalSystemExecutionSettings> extends Remote, ExternalSystemTaskAware {

  /**
   * Provides the service settings to use.
   * 
   * @param settings  settings to use
   * @throws RemoteException      as required by RMI
   */
  void setSettings(@Nonnull S settings) throws RemoteException;

  /**
   * Allows to define notification callback to use within the current service
   * 
   * @param notificationListener  notification listener to use with the current service
   * @throws RemoteException      as required by RMI
   */
  void setNotificationListener(@Nonnull ExternalSystemTaskNotificationListener notificationListener) throws RemoteException;
}
