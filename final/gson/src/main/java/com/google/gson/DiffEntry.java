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

import java.util.Objects;

/**
 * Represents a single difference between two {@link JsonElement} values at a specific path.
 *
 * <p>Each entry captures:
 *
 * <ul>
 *   <li>The {@linkplain #getPath() path} where the difference was found (e.g., {@code "$.a.b[0]"})
 *   <li>The {@linkplain #getOperation() type} of difference ({@link DiffOperation#ADD}, {@link
 *       DiffOperation#REMOVE}, or {@link DiffOperation#CHANGE})
 *   <li>The {@linkplain #getOldValue() old value} from the left element (may be {@code null} for
 *       {@code ADD})
 *   <li>The {@linkplain #getNewValue() new value} from the right element (may be {@code null} for
 *       {@code REMOVE})
 * </ul>
 *
 * <p>Note: {@code oldValue} and {@code newValue} are deep copies of the original {@code
 * JsonElement} values, ensuring that the diff result is a stable snapshot independent of subsequent
 * mutations to the compared elements.
 *
 * @see JsonDiff
 * @see JsonDiffResult
 * @since 2.12.2
 */
public final class DiffEntry {
  private final String path;
  private final DiffOperation operation;
  private final JsonElement oldValue;
  private final JsonElement newValue;

  DiffEntry(String path, DiffOperation operation, JsonElement oldValue, JsonElement newValue) {
    this.path = Objects.requireNonNull(path);
    this.operation = Objects.requireNonNull(operation);
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /**
   * Returns the JSON path where the difference was found. The root element has path {@code "$"}.
   * Object members use dot notation (e.g., {@code "$.a.b"}) and array elements use bracket notation
   * (e.g., {@code "$.a[0]"}).
   */
  public String getPath() {
    return path;
  }

  /** Returns the type of difference. */
  public DiffOperation getOperation() {
    return operation;
  }

  /**
   * Returns the value from the left {@link JsonElement} at this path.
   *
   * <p>Returns {@code null} when {@link #getOperation()} is {@link DiffOperation#ADD} (the path
   * does not exist in the left element). For {@link DiffOperation#REMOVE} and {@link
   * DiffOperation#CHANGE}, this returns a deep copy of the value from the left element.
   *
   * <p>Note: A {@code null} return value means the path does not exist on that side. This is
   * distinct from {@link JsonNull#INSTANCE}, which means the path exists but the value is JSON
   * {@code null}.
   */
  public JsonElement getOldValue() {
    return oldValue;
  }

  /**
   * Returns the value from the right {@link JsonElement} at this path.
   *
   * <p>Returns {@code null} when {@link #getOperation()} is {@link DiffOperation#REMOVE} (the path
   * does not exist in the right element). For {@link DiffOperation#ADD} and {@link
   * DiffOperation#CHANGE}, this returns a deep copy of the value from the right element.
   *
   * <p>Note: A {@code null} return value means the path does not exist on that side. This is
   * distinct from {@link JsonNull#INSTANCE}, which means the path exists but the value is JSON
   * {@code null}.
   */
  public JsonElement getNewValue() {
    return newValue;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(path).append(": ").append(operation);
    if (oldValue != null) {
      sb.append(", old=").append(oldValue);
    }
    if (newValue != null) {
      sb.append(", new=").append(newValue);
    }
    return sb.toString();
  }
}
