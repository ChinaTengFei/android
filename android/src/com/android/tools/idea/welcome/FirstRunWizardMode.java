/*
 * Copyright (C) 2014 The Android Open Source Project
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
 */
package com.android.tools.idea.welcome;

/**
 * There are several reasons when first run wizard is shown. Wizard behaves slightly differently, depending on the mode.
 */
public enum FirstRunWizardMode {
  /**
   * Newly installed Android Studio, first run wizard never ran on this system.
   */
  NEW_INSTALL,
  /**
   * Android Studio was installed by the Windows installer, we need to pick it up where it left.
   */
  INSTALL_HANDOFF,
  /**
   * Android Studio was completely setup but something happened to an SDK, we need to resetup it.
   */
  MISSING_SDK
}
