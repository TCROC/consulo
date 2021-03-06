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

/*
 * @author max
 */
package com.intellij.lang;

import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayFactory;
import javax.annotation.Nonnull;

public interface LighterASTNode {
  public static final LighterASTNode[] EMPTY_ARRAY = new LighterASTNode[0];

  public static ArrayFactory<LighterASTNode> ARRAY_FACTORY = new ArrayFactory<LighterASTNode>() {
    @Nonnull
    @Override
    public LighterASTNode[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new LighterASTNode[count];
    }
  };

  IElementType getTokenType();

  int getStartOffset();

  int getEndOffset();
}