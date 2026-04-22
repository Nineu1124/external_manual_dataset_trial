/*
 * Copyright (C) 2025 Google Inc.
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
package com.google.gson;

/**
 * Represents the type of difference between two {@link JsonElement} values.
 *
 * @see DiffEntry
 * @see JsonDiff
 * @since 2.12.2
 */
public enum DiffOperation {
  /**
   * A path exists in the right {@link JsonElement} but not in the left.
   *
   * <p>For this operation, {@link DiffEntry#getOldValue()} returns {@code null} and {@link
   * DiffEntry#getNewValue()} returns the value from the right element.
   */
  ADD,

  /**
   * A path exists in the left {@link JsonElement} but not in the right.
   *
   * <p>For this operation, {@link DiffEntry#getOldValue()} returns the value from the left element
   * and {@link DiffEntry#getNewValue()} returns {@code null}.
   */
  REMOVE,

  /**
   * A path exists in both {@link JsonElement} values but the values differ.
   *
   * <p>For this operation, {@link DiffEntry#getOldValue()} returns the value from the left element
   * and {@link DiffEntry#getNewValue()} returns the value from the right element.
   */
  CHANGE
}
