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
package com.android.tools.idea.editors.gfxtrace.service;

import com.android.tools.rpclib.binary.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class Capture implements BinaryObject {
  //<<<Start:Java.ClassBody:1>>>
  private String myName;
  private BinaryID myAtoms;
  private BinaryID[] myApis;

  // Constructs a default-initialized {@link Capture}.
  public Capture() {}


  public String getName() {
    return myName;
  }

  public Capture setName(String v) {
    myName = v;
    return this;
  }

  public BinaryID getAtoms() {
    return myAtoms;
  }

  public Capture setAtoms(BinaryID v) {
    myAtoms = v;
    return this;
  }

  public BinaryID[] getApis() {
    return myApis;
  }

  public Capture setApis(BinaryID[] v) {
    myApis = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }

  private static final byte[] IDBytes = {83, -125, -36, 55, 30, 38, -97, -71, -55, -8, 111, 75, 66, 59, -51, -68, 1, -125, 118, -30, };
  public static final BinaryID ID = new BinaryID(IDBytes);

  static {
    Namespace.register(ID, Klass.INSTANCE);
  }
  public static void register() {}
  //<<<End:Java.ClassBody:1>>>
  public enum Klass implements BinaryClass {
    //<<<Start:Java.KlassBody:2>>>
    INSTANCE;

    @Override @NotNull
    public BinaryID id() { return ID; }

    @Override @NotNull
    public BinaryObject create() { return new Capture(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      Capture o = (Capture)obj;
      e.string(o.myName);
      e.id(o.myAtoms);
      e.uint32(o.myApis.length);
      for (int i = 0; i < o.myApis.length; i++) {
        e.id(o.myApis[i]);
      }
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      Capture o = (Capture)obj;
      o.myName = d.string();
      o.myAtoms = d.id();
      o.myApis = new BinaryID[d.uint32()];
      for (int i = 0; i <o.myApis.length; i++) {
        o.myApis[i] = d.id();
      }
    }
    //<<<End:Java.KlassBody:2>>>
  }
}