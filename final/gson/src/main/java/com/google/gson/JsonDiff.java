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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Compares two {@link JsonElement} values and produces a structured diff result.
 *
 * <p>The comparison follows the same semantics as {@link JsonElement#equals(Object)}:
 *
 * <ul>
 *   <li>{@link JsonObject} members are compared by name, ignoring insertion order
 *   <li>{@link JsonArray} elements are compared by index, order-sensitive
 *   <li>{@link JsonPrimitive} values are compared using {@link JsonPrimitive#equals(Object)}
 *   <li>{@link JsonNull} instances are always equal
 * </ul>
 *
 * <p>When two elements are equal according to these rules, the diff result is empty. When they
 * differ, the result contains {@link DiffEntry} instances with JSON paths describing each
 * difference.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * JsonObject left = new JsonObject();
 * left.addProperty("name", "Alice");
 * left.addProperty("age", 30);
 *
 * JsonObject right = new JsonObject();
 * right.addProperty("name", "Bob");
 * right.addProperty("age", 30);
 * right.addProperty("email", "bob@example.com");
 *
 * JsonDiffResult result = JsonDiff.diff(left, right);
 * // result contains:
 * //   $.name: CHANGE (old="Alice", new="Bob")
 * //   $.email: ADD (new="bob@example.com")
 * }</pre>
 *
 * @see JsonDiffResult
 * @see DiffEntry
 * @see DiffOperation
 * @since 2.12.2
 */
public final class JsonDiff {

  private JsonDiff() {} // Pure static utility class

  /**
   * Compares two {@link JsonElement} values and returns the differences.
   *
   * @param left the base {@link JsonElement}, must not be {@code null}
   * @param right the {@link JsonElement} to compare against, must not be {@code null}
   * @return a {@link JsonDiffResult} describing all differences; empty if the elements are equal
   * @throws NullPointerException if left or right is null
   */
  public static JsonDiffResult diff(JsonElement left, JsonElement right) {
    Objects.requireNonNull(left, "left must not be null");
    Objects.requireNonNull(right, "right must not be null");
    List<DiffEntry> differences = new ArrayList<>();
    collectDifferences(left, right, "$", differences);
    return new JsonDiffResult(differences);
  }

  private static void collectDifferences(
      JsonElement left, JsonElement right, String path, List<DiffEntry> differences) {
    // Short-circuit: if equal, no differences
    if (left.equals(right)) {
      return;
    }

    // Both are JsonObject -> compare members by name
    if (left.isJsonObject() && right.isJsonObject()) {
      diffObjects(left.getAsJsonObject(), right.getAsJsonObject(), path, differences);
      return;
    }

    // Both are JsonArray -> compare elements by index
    if (left.isJsonArray() && right.isJsonArray()) {
      diffArrays(left.getAsJsonArray(), right.getAsJsonArray(), path, differences);
      return;
    }

    // All other cases (type mismatch, or primitive values that differ)
    differences.add(
        new DiffEntry(path, DiffOperation.CHANGE, left.deepCopy(), right.deepCopy()));
  }

  private static void diffObjects(
      JsonObject left, JsonObject right, String path, List<DiffEntry> differences) {
    Set<String> leftKeys = left.keySet();
    Set<String> rightKeys = right.keySet();

    // Members only in left -> REMOVE
    for (String key : leftKeys) {
      if (!rightKeys.contains(key)) {
        differences.add(
            new DiffEntry(
                memberPath(path, key),
                DiffOperation.REMOVE,
                left.get(key).deepCopy(),
                null));
      }
    }

    // Members only in right -> ADD
    for (String key : rightKeys) {
      if (!leftKeys.contains(key)) {
        differences.add(
            new DiffEntry(
                memberPath(path, key),
                DiffOperation.ADD,
                null,
                right.get(key).deepCopy()));
      }
    }

    // Members in both -> recurse
    for (String key : leftKeys) {
      if (rightKeys.contains(key)) {
        collectDifferences(
            left.get(key), right.get(key), memberPath(path, key), differences);
      }
    }
  }

  private static void diffArrays(
      JsonArray left, JsonArray right, String path, List<DiffEntry> differences) {
    int minSize = Math.min(left.size(), right.size());

    // Compare elements at common indices
    for (int i = 0; i < minSize; i++) {
      collectDifferences(left.get(i), right.get(i), elementPath(path, i), differences);
    }

    // Extra elements in right -> ADD
    for (int i = minSize; i < right.size(); i++) {
      differences.add(
          new DiffEntry(
              elementPath(path, i), DiffOperation.ADD, null, right.get(i).deepCopy()));
    }

    // Extra elements in left -> REMOVE
    for (int i = minSize; i < left.size(); i++) {
      differences.add(
          new DiffEntry(
              elementPath(path, i), DiffOperation.REMOVE, left.get(i).deepCopy(), null));
    }
  }

  private static String memberPath(String parent, String memberName) {
    return parent + "." + memberName;
  }

  private static String elementPath(String parent, int index) {
    return parent + "[" + index + "]";
  }
}
