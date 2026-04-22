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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The result of comparing two {@link JsonElement} values using {@link JsonDiff#diff}.
 *
 * <p>The result contains a list of {@link DiffEntry} instances, each describing a single
 * difference. If the list is empty, the two elements are considered equal according to the same
 * rules as {@link JsonElement#equals(Object)}.
 *
 * @see JsonDiff
 * @see DiffEntry
 * @since 2.12.2
 */
public final class JsonDiffResult {
  private final List<DiffEntry> differences;

  JsonDiffResult(List<DiffEntry> differences) {
    this.differences = Objects.requireNonNull(differences);
  }

  /**
   * Returns {@code true} if no differences were found, meaning the two {@link JsonElement} values
   * are equal.
   */
  public boolean isEmpty() {
    return differences.isEmpty();
  }

  /** Returns the number of differences found. */
  public int size() {
    return differences.size();
  }

  /**
   * Returns an unmodifiable list of {@link DiffEntry} instances describing each difference.
   *
   * <p>The entries are ordered by discovery order during the depth-first traversal of the JSON
   * tree.
   */
  public List<DiffEntry> getDifferences() {
    return Collections.unmodifiableList(differences);
  }
}
