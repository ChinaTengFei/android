/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.idea.editors.gfxtrace.service.path;

import com.android.tools.rpclib.schema.*;
import com.android.tools.rpclib.binary.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class MemoryRangePath extends Path {
  @Override
  public StringBuilder stringPath(StringBuilder builder) {
    return myAfter.stringPath(builder).append(".MemoryRange<").append(myPool).append(">")
      .append("[").append(Long.toHexString(myAddress)).append(":").append(Long.toHexString(getEndAddress())).append("]");
  }

  @Override
  public Path getParent() {
    return myAfter;
  }

  public long getEndAddress() {
    return myAddress + mySize-1;
  }

  //<<<Start:Java.ClassBody:1>>>
  private AtomPath myAfter;
  private long myPool;
  private long myAddress;
  private long mySize;

  // Constructs a default-initialized {@link MemoryRangePath}.
  public MemoryRangePath() {}


  public AtomPath getAfter() {
    return myAfter;
  }

  public MemoryRangePath setAfter(AtomPath v) {
    myAfter = v;
    return this;
  }

  public long getPool() {
    return myPool;
  }

  public MemoryRangePath setPool(long v) {
    myPool = v;
    return this;
  }

  public long getAddress() {
    return myAddress;
  }

  public MemoryRangePath setAddress(long v) {
    myAddress = v;
    return this;
  }

  public long getSize() {
    return mySize;
  }

  public MemoryRangePath setSize(long v) {
    mySize = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }


  private static final Entity ENTITY = new Entity("path","MemoryRange","","");

  static {
    ENTITY.setFields(new Field[]{
      new Field("After", new Pointer(new Struct(AtomPath.Klass.INSTANCE.entity()))),
      new Field("Pool", new Primitive("uint64", Method.Uint64)),
      new Field("Address", new Primitive("uint64", Method.Uint64)),
      new Field("Size", new Primitive("uint64", Method.Uint64)),
    });
    Namespace.register(Klass.INSTANCE);
  }
  public static void register() {}
  //<<<End:Java.ClassBody:1>>>
  public enum Klass implements BinaryClass {
    //<<<Start:Java.KlassBody:2>>>
    INSTANCE;

    @Override @NotNull
    public Entity entity() { return ENTITY; }

    @Override @NotNull
    public BinaryObject create() { return new MemoryRangePath(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      MemoryRangePath o = (MemoryRangePath)obj;
      e.object(o.myAfter);
      e.uint64(o.myPool);
      e.uint64(o.myAddress);
      e.uint64(o.mySize);
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      MemoryRangePath o = (MemoryRangePath)obj;
      o.myAfter = (AtomPath)d.object();
      o.myPool = d.uint64();
      o.myAddress = d.uint64();
      o.mySize = d.uint64();
    }
    //<<<End:Java.KlassBody:2>>>
  }
}