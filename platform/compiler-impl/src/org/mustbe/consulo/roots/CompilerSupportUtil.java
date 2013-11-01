/*
 * Copyright 2013 must-be.org
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
package org.mustbe.consulo.roots;

import gnu.trove.THashMap;

import java.util.Map;

/**
 * @author VISTALL
 * @since 23:42/31.10.13
 */
public class CompilerSupportUtil {
  private static Map<ContentFolderTypeProvider, ContentFolderTypeProvider> ourCache =
    new THashMap<ContentFolderTypeProvider, ContentFolderTypeProvider>();

  public static boolean isCompilingSupported(ContentFolderTypeProvider contentFolderTypeProvider) {

  }

  public static ContentFolderTypeProvider getCompileRuleProvider(ContentFolderTypeProvider contentFolderTypeProvider) {
    if(ourCache.containsKey(contentFolderTypeProvider)) {
      return ourCache.get(contentFolderTypeProvider);
    }

    CompilerSupport annotation = contentFolderTypeProvider.getClass().getAnnotation(CompilerSupport.class);
    if(annotation == null) {
      ourCache.put(contentFolderTypeProvider, null);
      return null;
    }
    else {
      ContentFolderTypeProvider extension = ContentFolderTypeProvider.EP_NAME.findExtension(annotation.ruleProvider());
    }
  }
}
