// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.io.AbstractStringEnumerator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

class LazyStubList extends StubList {
  private final AtomicReferenceArray<StubBase<?>> myStubs;
  private final ObjectStubSerializer myRootSerializer;
  private int mySize;
  private final AtomicInteger myInstantiated = new AtomicInteger(1);
  private volatile LazyStubData myData;

  LazyStubList(int size, StubBase<?> root, ObjectStubSerializer rootSerializer) {
    super(size);
    myStubs = new AtomicReferenceArray<>(size);
    myRootSerializer = rootSerializer;
    myStubs.set(0, root);
    root.myStubList = this;
  }

  @Override
  void addStub(@Nonnull StubBase<?> stub, @Nullable StubBase<?> parent, @Nullable IStubElementType<?, ?> type) {
    // stub is lazily created, so we already know all structure, so do nothing
  }

  void addLazyStub(IElementType type, int childIndex, int parentIndex) {
    addStub(childIndex, parentIndex, type.getIndex());
    mySize++;
  }

  @Override
  boolean areChildrenNonAdjacent(int childId, int parentId) {
    return false;
  }

  @Override
  public int size() {
    return mySize;
  }

  @Override
  public StubBase<?> get(int index) {
    StubBase<?> stub = getCachedStub(index);
    if (stub != null) return stub;

    StubBase<?> newStub = instantiateStub(index);
    if (myStubs.compareAndSet(index, null, newStub)) {
      if (myInstantiated.incrementAndGet() == myStubs.length()) {
        myData = null; // free some memory
      }
    }
    else {
      newStub = getCachedStub(index);
    }
    return newStub;
  }

  @Nullable
  @Override
  StubBase<?> getCachedStub(int index) {
    return myStubs.get(index);
  }

  @Nonnull
  private StubBase<?> instantiateStub(int index) {
    LazyStubData data = myData;
    if (data == null) {
      StubBase<?> stub = getCachedStub(index);
      if (stub != null) return stub;

      throw new IllegalStateException("Not all (" + mySize + ") stubs are instantiated (" + myInstantiated + "), but data for them is missing");
    }

    try {
      StubBase<?> parent = get(data.getParentIndex(index));
      StubBase<?> stub = data.deserializeStub(index, parent, getStubType(index));
      stub.id = index;
      return stub;
    }
    catch (Exception | Error e) {
      throw new RuntimeException(StubSerializationHelper.brokenStubFormat(myRootSerializer), e);
    }
  }

  void setStubData(LazyStubData data) {
    if (myInstantiated.get() < mySize) {
      myData = data;
    }
  }

}

class LazyStubData {
  private final AbstractStringEnumerator myStorage;
  private final MostlyUShortIntList myParentsAndStarts;
  private final byte[] mySerializedStubs;
  private final BitSet myAllStarts;

  LazyStubData(AbstractStringEnumerator storage, MostlyUShortIntList parentsAndStarts, byte[] serializedStubs, BitSet allStarts) {
    myStorage = storage;
    myParentsAndStarts = parentsAndStarts;
    mySerializedStubs = serializedStubs;
    myAllStarts = allStarts;
  }

  int getParentIndex(int index) {
    return myParentsAndStarts.get(index * 2);
  }

  private int getDataStart(int index) {
    return myParentsAndStarts.get(index * 2 + 1);
  }

  StubBase<?> deserializeStub(int index, StubBase<?> parent, IStubElementType<?, ?> type) throws IOException {
    StubInputStream stream = new StubInputStream(stubBytes(index), myStorage);
    StubBase<?> stub = (StubBase<?>)type.deserialize(stream, parent);
    int available = stream.available();
    if (available > 0) {
      assert available == 1 : "Stub serializer/deserializer mismatch in " + type;
      stub.markDangling();
    }
    return stub;
  }

  private ByteArrayInputStream stubBytes(int index) {
    int start = getDataStart(index);
    if (start == 0) return new ByteArrayInputStream(ArrayUtilRt.EMPTY_BYTE_ARRAY);

    int end = myAllStarts.nextSetBit(start + 1);
    if (end < 0) end = mySerializedStubs.length + 1;
    return new ByteArrayInputStream(mySerializedStubs, start - 1, end - start);
  }
}