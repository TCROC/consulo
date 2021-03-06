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
package com.intellij.openapi.vfs.newvfs;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.ArchiveHandler;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.util.Consumer;
import com.intellij.util.PairFunction;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.vfs.impl.archive.ArchiveFileSystemBase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.openapi.util.Pair.pair;
import static com.intellij.util.containers.ContainerUtil.newTroveMap;

public class VfsImplUtil {
  private static final Logger LOG = Logger.getInstance(VfsImplUtil.class);

  private static final String FILE_SEPARATORS = "/" + File.separator;

  private VfsImplUtil() {
  }

  @Nullable
  public static NewVirtualFile findFileByPath(@Nonnull NewVirtualFileSystem vfs, @Nonnull String path) {
    Pair<NewVirtualFile, Iterable<String>> data = prepare(vfs, path);
    if (data == null) return null;

    NewVirtualFile file = data.first;
    for (String pathElement : data.second) {
      if (pathElement.isEmpty() || ".".equals(pathElement)) continue;
      if ("..".equals(pathElement)) {
        if (file.is(VFileProperty.SYMLINK)) {
          final NewVirtualFile canonicalFile = file.getCanonicalFile();
          file = canonicalFile != null ? canonicalFile.getParent() : null;
        }
        else {
          file = file.getParent();
        }
      }
      else {
        file = file.findChild(pathElement);
      }

      if (file == null) return null;
    }

    return file;
  }

  @Nullable
  public static NewVirtualFile findFileByPathIfCached(@Nonnull NewVirtualFileSystem vfs, @Nonnull String path) {
    return findCachedFileByPath(vfs, path).first;
  }

  @Nonnull
  public static Pair<NewVirtualFile, NewVirtualFile> findCachedFileByPath(@Nonnull NewVirtualFileSystem vfs, @Nonnull String path) {
    Pair<NewVirtualFile, Iterable<String>> data = prepare(vfs, path);
    if (data == null) return Pair.empty();

    NewVirtualFile file = data.first;
    for (String pathElement : data.second) {
      if (pathElement.isEmpty() || ".".equals(pathElement)) continue;

      NewVirtualFile last = file;
      if ("..".equals(pathElement)) {
        if (file.is(VFileProperty.SYMLINK)) {
          String canonicalPath = file.getCanonicalPath();
          NewVirtualFile canonicalFile = canonicalPath != null ? findCachedFileByPath(vfs, canonicalPath).first : null;
          file = canonicalFile != null ? canonicalFile.getParent() : null;
        }
        else {
          file = file.getParent();
        }
      }
      else {
        file = file.findChildIfCached(pathElement);
      }

      if (file == null) return pair(null, last);
    }

    return pair(file, null);
  }

  @Nullable
  public static NewVirtualFile refreshAndFindFileByPath(@Nonnull NewVirtualFileSystem vfs, @Nonnull String path) {
    Pair<NewVirtualFile, Iterable<String>> data = prepare(vfs, path);
    if (data == null) return null;

    NewVirtualFile file = data.first;
    for (String pathElement : data.second) {
      if (pathElement.isEmpty() || ".".equals(pathElement)) continue;
      if ("..".equals(pathElement)) {
        if (file.is(VFileProperty.SYMLINK)) {
          final String canonicalPath = file.getCanonicalPath();
          final NewVirtualFile canonicalFile = canonicalPath != null ? refreshAndFindFileByPath(vfs, canonicalPath) : null;
          file = canonicalFile != null ? canonicalFile.getParent() : null;
        }
        else {
          file = file.getParent();
        }
      }
      else {
        file = file.refreshAndFindChild(pathElement);
      }

      if (file == null) return null;
    }

    return file;
  }

  @Nullable
  private static Pair<NewVirtualFile, Iterable<String>> prepare(@Nonnull NewVirtualFileSystem vfs, @Nonnull String path) {
    String normalizedPath = normalize(vfs, path);
    if (StringUtil.isEmptyOrSpaces(normalizedPath)) {
      return null;
    }

    String basePath = vfs.extractRootPath(normalizedPath);
    if (basePath.length() > normalizedPath.length()) {
      LOG.error(vfs + " failed to extract root path '" + basePath + "' from '" + normalizedPath + "' (original '" + path + "')");
      return null;
    }

    NewVirtualFile root = ManagingFS.getInstance().findRoot(basePath, vfs);
    if (root == null || !root.exists()) {
      return null;
    }

    Iterable<String> parts = StringUtil.tokenize(normalizedPath.substring(basePath.length()), FILE_SEPARATORS);
    return Pair.create(root, parts);
  }

  public static void refresh(@Nonnull NewVirtualFileSystem vfs, boolean asynchronous) {
    VirtualFile[] roots = ManagingFS.getInstance().getRoots(vfs);
    if (roots.length > 0) {
      RefreshQueue.getInstance().refresh(asynchronous, true, null, roots);
    }
  }

  @Nullable
  public static String normalize(@Nonnull NewVirtualFileSystem vfs, @Nonnull String path) {
    return vfs.normalize(path);
  }

  private static final AtomicBoolean ourSubscribed = new AtomicBoolean(false);
  private static final Object ourLock = new Object();
  private static final Map<String, Pair<ArchiveFileSystem, ArchiveHandler>> ourHandlers = newTroveMap(FileUtil.PATH_HASHING_STRATEGY);
  private static final Map<String, Set<String>> ourDominatorsMap = newTroveMap(FileUtil.PATH_HASHING_STRATEGY);

  @Nonnull
  public static <T extends ArchiveHandler> T getHandler(@Nonnull ArchiveFileSystem vfs,
                                                        @Nonnull VirtualFile entryFile,
                                                        @Nonnull PairFunction<String, ArchiveFileSystemBase, T> producer) {
    String localPath = vfs.extractLocalPath(vfs.extractRootPath(entryFile.getPath()));
    return getHandler(vfs, localPath, producer);
  }

  @Nonnull
  public static <T extends ArchiveHandler> T getHandler(@Nonnull ArchiveFileSystem vfs,
                                                        @Nonnull String localPath,
                                                        @Nonnull PairFunction<String, ArchiveFileSystemBase, T> producer) {
    checkSubscription();

    ArchiveHandler handler;

    synchronized (ourLock) {
      Pair<ArchiveFileSystem, ArchiveHandler> record = ourHandlers.get(localPath);
      if (record == null) {
        handler = producer.fun(localPath, (ArchiveFileSystemBase)vfs);
        record = Pair.create(vfs, handler);
        ourHandlers.put(localPath, record);

        final String finalRootPath = localPath;
        forEachDirectoryComponent(localPath, containingDirectoryPath -> {
          Set<String> handlers = ourDominatorsMap.get(containingDirectoryPath);
          if (handlers == null) {
            ourDominatorsMap.put(containingDirectoryPath, handlers = ContainerUtil.newTroveSet());
          }
          handlers.add(finalRootPath);
        });
      }
      handler = record.second;
    }

    @SuppressWarnings("unchecked") T t = (T)handler;
    return t;
  }

  private static void forEachDirectoryComponent(String rootPath, Consumer<String> consumer) {
    int index = rootPath.lastIndexOf('/');
    while (index > 0) {
      String containingDirectoryPath = rootPath.substring(0, index);
      consumer.consume(containingDirectoryPath);
      index = rootPath.lastIndexOf('/', index - 1);
    }
  }

  private static void checkSubscription() {
    if (ourSubscribed.getAndSet(true)) return;

    Application app = ApplicationManager.getApplication();
    app.getMessageBus().connect(app).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        InvalidationState state = null;

        synchronized (ourLock) {
          for (VFileEvent event : events) {
            if (!(event.getFileSystem() instanceof LocalFileSystem)) continue;

            if (event instanceof VFileCreateEvent) continue; // created file should not invalidate + getFile is costly

            if (event instanceof VFilePropertyChangeEvent && !VirtualFile.PROP_NAME.equals(((VFilePropertyChangeEvent)event).getPropertyName())) {
              continue;
            }

            String path = event.getPath();
            if (event instanceof VFilePropertyChangeEvent) {
              path = ((VFilePropertyChangeEvent)event).getOldPath();
            }
            else if (event instanceof VFileMoveEvent) {
              path = ((VFileMoveEvent)event).getOldPath();
            }

            VirtualFile file = event.getFile();
            if (file == null || !file.isDirectory()) {
              state = InvalidationState.invalidate(state, path);
            }
            else {
              Collection<String> affectedPaths = ourDominatorsMap.get(path);
              if (affectedPaths != null) {
                affectedPaths = ContainerUtil.newArrayList(affectedPaths);  // defensive copying; original may be updated on invalidation
                for (String affectedPath : affectedPaths) {
                  state = InvalidationState.invalidate(state, affectedPath);
                }
              }
            }
          }
        }

        if (state != null) state.scheduleRefresh();
      }
    });
  }

  private static class InvalidationState {
    private Set<NewVirtualFile> myRootsToRefresh;

    @Nullable
    static InvalidationState invalidate(@Nullable InvalidationState state, final String path) {
      Pair<ArchiveFileSystem, ArchiveHandler> handlerPair = ourHandlers.remove(path);
      if (handlerPair != null) {
        handlerPair.second.dispose();
        forEachDirectoryComponent(path, containingDirectoryPath -> {
          Set<String> handlers = ourDominatorsMap.get(containingDirectoryPath);
          if (handlers != null && handlers.remove(path) && handlers.isEmpty()) {
            ourDominatorsMap.remove(containingDirectoryPath);
          }
        });

        if (state == null) state = new InvalidationState();
        state.registerPathToRefresh(path, handlerPair.first);
      }

      return state;
    }

    private void registerPathToRefresh(String path, ArchiveFileSystem vfs) {
      NewVirtualFile root = ManagingFS.getInstance().findRoot(vfs.composeRootPath(path), vfs);
      if (root != null) {
        if (myRootsToRefresh == null) myRootsToRefresh = ContainerUtil.newHashSet();
        myRootsToRefresh.add(root);
      }
    }

    void scheduleRefresh() {
      if (myRootsToRefresh != null) {
        for (NewVirtualFile root : myRootsToRefresh) {
          root.markDirtyRecursively();
        }
        boolean async = !ApplicationManager.getApplication().isUnitTestMode();
        RefreshQueue.getInstance().refresh(async, true, null, myRootsToRefresh);
      }
    }
  }
}