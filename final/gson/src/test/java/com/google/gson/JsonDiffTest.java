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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.junit.Test;

/** Tests for {@link JsonDiff}, {@link JsonDiffResult}, {@link DiffEntry}, and {@link DiffOperation}. */
public class JsonDiffTest {

  // ---- Null argument handling ----

  @Test
  public void testDiffNullLeft() {
    NullPointerException e =
        assertThrows(
            NullPointerException.class, () -> JsonDiff.diff(null, JsonNull.INSTANCE));
    assertThat(e).hasMessageThat().isEqualTo("left must not be null");
  }

  @Test
  public void testDiffNullRight() {
    NullPointerException e =
        assertThrows(
            NullPointerException.class, () -> JsonDiff.diff(JsonNull.INSTANCE, null));
    assertThat(e).hasMessageThat().isEqualTo("right must not be null");
  }

  // ---- Identical elements -> no differences ----

  @Test
  public void testDiffIdenticalPrimitives() {
    JsonDiffResult result = JsonDiff.diff(new JsonPrimitive(42), new JsonPrimitive(42));
    assertThat(result.isEmpty()).isTrue();
    assertThat(result.size()).isEqualTo(0);
  }

  @Test
  public void testDiffIdenticalStrings() {
    JsonDiffResult result =
        JsonDiff.diff(new JsonPrimitive("hello"), new JsonPrimitive("hello"));
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  public void testDiffIdenticalBooleans() {
    JsonDiffResult result =
        JsonDiff.diff(new JsonPrimitive(true), new JsonPrimitive(true));
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  public void testDiffIdenticalNulls() {
    JsonDiffResult result = JsonDiff.diff(JsonNull.INSTANCE, JsonNull.INSTANCE);
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  public void testDiffIdenticalObjects() {
    JsonObject a = new JsonObject();
    a.addProperty("name", "Alice");
    a.addProperty("age", 30);
    JsonObject b = new JsonObject();
    b.addProperty("name", "Alice");
    b.addProperty("age", 30);
    assertThat(JsonDiff.diff(a, b).isEmpty()).isTrue();
  }

  @Test
  public void testDiffIdenticalArrays() {
    JsonArray a = new JsonArray();
    a.add(1);
    a.add("two");
    a.add(false);
    JsonArray b = new JsonArray();
    b.add(1);
    b.add("two");
    b.add(false);
    assertThat(JsonDiff.diff(a, b).isEmpty()).isTrue();
  }

  // ---- Empty objects and arrays ----

  @Test
  public void testDiffEmptyObjects() {
    assertThat(JsonDiff.diff(new JsonObject(), new JsonObject()).isEmpty()).isTrue();
  }

  @Test
  public void testDiffEmptyArrays() {
    assertThat(JsonDiff.diff(new JsonArray(), new JsonArray()).isEmpty()).isTrue();
  }

  // ---- JsonObject: member addition, removal, change ----

  @Test
  public void testDiffObjectMemberAdded() {
    JsonObject left = new JsonObject();
    left.addProperty("a", 1);
    JsonObject right = new JsonObject();
    right.addProperty("a", 1);
    right.addProperty("b", 2);

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.ADD);
    assertThat(entry.getPath()).isEqualTo("$.b");
    assertThat(entry.getOldValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo(new JsonPrimitive(2));
  }

  @Test
  public void testDiffObjectMemberRemoved() {
    JsonObject left = new JsonObject();
    left.addProperty("a", 1);
    left.addProperty("b", 2);
    JsonObject right = new JsonObject();
    right.addProperty("a", 1);

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.REMOVE);
    assertThat(entry.getPath()).isEqualTo("$.b");
    assertThat(entry.getOldValue()).isEqualTo(new JsonPrimitive(2));
    assertThat(entry.getNewValue()).isNull();
  }

  @Test
  public void testDiffObjectMemberChanged() {
    JsonObject left = new JsonObject();
    left.addProperty("name", "Alice");
    JsonObject right = new JsonObject();
    right.addProperty("name", "Bob");

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.CHANGE);
    assertThat(entry.getPath()).isEqualTo("$.name");
    assertThat(entry.getOldValue()).isEqualTo(new JsonPrimitive("Alice"));
    assertThat(entry.getNewValue()).isEqualTo(new JsonPrimitive("Bob"));
  }

  @Test
  public void testDiffObjectMultipleChanges() {
    JsonObject left = new JsonObject();
    left.addProperty("a", 1);
    left.addProperty("b", 2);
    JsonObject right = new JsonObject();
    right.addProperty("a", 10);
    right.addProperty("c", 3);

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(3);

    List<DiffEntry> diffs = result.getDifferences();
    assertThat(diffs.stream().filter(e -> e.getOperation() == DiffOperation.CHANGE)).hasSize(1);
    assertThat(diffs.stream().filter(e -> e.getOperation() == DiffOperation.REMOVE)).hasSize(1);
    assertThat(diffs.stream().filter(e -> e.getOperation() == DiffOperation.ADD)).hasSize(1);
  }

  // ---- JsonObject: order ignored ----

  @Test
  public void testDiffObjectOrderIgnored() {
    JsonObject a = new JsonObject();
    a.addProperty("x", 1);
    a.addProperty("y", 2);
    JsonObject b = new JsonObject();
    b.addProperty("y", 2);
    b.addProperty("x", 1);

    assertThat(JsonDiff.diff(a, b).isEmpty()).isTrue();
  }

  // ---- JsonArray: index-sensitive ----

  @Test
  public void testDiffArrayOrderSensitive() {
    JsonArray a = new JsonArray();
    a.add(1);
    a.add(2);
    JsonArray b = new JsonArray();
    b.add(2);
    b.add(1);

    JsonDiffResult result = JsonDiff.diff(a, b);
    assertThat(result.isEmpty()).isFalse();
    assertThat(result.size()).isEqualTo(2);
    // Both indices have changed values
    assertThat(
            result.getDifferences().stream()
                .allMatch(e -> e.getOperation() == DiffOperation.CHANGE))
        .isTrue();
  }

  @Test
  public void testDiffArrayElementAdded() {
    JsonArray left = new JsonArray();
    left.add(1);
    JsonArray right = new JsonArray();
    right.add(1);
    right.add(2);

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.ADD);
    assertThat(entry.getPath()).isEqualTo("$[1]");
    assertThat(entry.getNewValue()).isEqualTo(new JsonPrimitive(2));
  }

  @Test
  public void testDiffArrayElementRemoved() {
    JsonArray left = new JsonArray();
    left.add(1);
    left.add(2);
    JsonArray right = new JsonArray();
    right.add(1);

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.REMOVE);
    assertThat(entry.getPath()).isEqualTo("$[1]");
    assertThat(entry.getOldValue()).isEqualTo(new JsonPrimitive(2));
  }

  // ---- Nested structures ----

  @Test
  public void testDiffNestedObject() {
    JsonObject left = new JsonObject();
    JsonObject leftInner = new JsonObject();
    leftInner.addProperty("x", 1);
    left.add("inner", leftInner);

    JsonObject right = new JsonObject();
    JsonObject rightInner = new JsonObject();
    rightInner.addProperty("x", 2);
    right.add("inner", rightInner);

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.CHANGE);
    assertThat(entry.getPath()).isEqualTo("$.inner.x");
  }

  @Test
  public void testDiffNestedArray() {
    JsonArray left = new JsonArray();
    JsonArray leftInner = new JsonArray();
    leftInner.add(1);
    left.add(leftInner);

    JsonArray right = new JsonArray();
    JsonArray rightInner = new JsonArray();
    rightInner.add(2);
    right.add(rightInner);

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.CHANGE);
    assertThat(entry.getPath()).isEqualTo("$[0][0]");
  }

  @Test
  public void testDiffMixedNestedStructure() {
    // left: {"items": [{"name": "A", "price": 10}]}
    JsonObject left = new JsonObject();
    JsonArray leftItems = new JsonArray();
    JsonObject leftItem = new JsonObject();
    leftItem.addProperty("name", "A");
    leftItem.addProperty("price", 10);
    leftItems.add(leftItem);
    left.add("items", leftItems);

    // right: {"items": [{"name": "B", "price": 10, "discount": true}]}
    JsonObject right = new JsonObject();
    JsonArray rightItems = new JsonArray();
    JsonObject rightItem = new JsonObject();
    rightItem.addProperty("name", "B");
    rightItem.addProperty("price", 10);
    rightItem.addProperty("discount", true);
    rightItems.add(rightItem);
    right.add("items", rightItems);

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(2);
    // $.items[0].name changed
    // $.items[0].discount added
    assertThat(
            result.getDifferences().stream()
                .anyMatch(
                    e ->
                        e.getPath().equals("$.items[0].name")
                            && e.getOperation() == DiffOperation.CHANGE))
        .isTrue();
    assertThat(
            result.getDifferences().stream()
                .anyMatch(
                    e ->
                        e.getPath().equals("$.items[0].discount")
                            && e.getOperation() == DiffOperation.ADD))
        .isTrue();
  }

  // ---- JsonNull ----

  @Test
  public void testDiffNullVsJsonNull() {
    // JsonNull.INSTANCE equals JsonNull.INSTANCE
    assertThat(JsonDiff.diff(JsonNull.INSTANCE, JsonNull.INSTANCE).isEmpty()).isTrue();
  }

  @Test
  public void testDiffObjectWithJsonNullValue() {
    JsonObject left = new JsonObject();
    left.add("a", JsonNull.INSTANCE);
    JsonObject right = new JsonObject();
    right.addProperty("a", "value");

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.CHANGE);
    assertThat(entry.getPath()).isEqualTo("$.a");
    assertThat(entry.getOldValue()).isEqualTo(JsonNull.INSTANCE);
    assertThat(entry.getNewValue()).isEqualTo(new JsonPrimitive("value"));
  }

  @Test
  public void testDiffObjectMemberAddedAsJsonNull() {
    JsonObject left = new JsonObject();
    JsonObject right = new JsonObject();
    right.add("a", JsonNull.INSTANCE);

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.ADD);
    assertThat(entry.getPath()).isEqualTo("$.a");
    assertThat(entry.getNewValue()).isEqualTo(JsonNull.INSTANCE);
  }

  // ---- Type changes ----

  @Test
  public void testDiffTypeChangeStringVsNumber() {
    // "1" vs 1 -> CHANGE (not equal per JsonPrimitive.equals)
    JsonDiffResult result =
        JsonDiff.diff(new JsonPrimitive("1"), new JsonPrimitive(1));
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.CHANGE);
  }

  @Test
  public void testDiffTypeChangeObjectVsArray() {
    JsonObject left = new JsonObject();
    left.addProperty("key", "value");
    JsonArray right = new JsonArray();
    right.add("value");

    JsonDiffResult result = JsonDiff.diff(left, right);
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.CHANGE);
    assertThat(entry.getOldValue()).isEqualTo(left.deepCopy());
    assertThat(entry.getNewValue()).isEqualTo(right.deepCopy());
  }

  @Test
  public void testDiffTypeChangeNullVsPrimitive() {
    JsonDiffResult result =
        JsonDiff.diff(JsonNull.INSTANCE, new JsonPrimitive("value"));
    assertThat(result.size()).isEqualTo(1);
    DiffEntry entry = result.getDifferences().get(0);
    assertThat(entry.getOperation()).isEqualTo(DiffOperation.CHANGE);
  }

  // ---- DiffEntry oldValue/newValue are deep copies ----

  @Test
  public void testDiffEntryValuesAreDeepCopies() {
    JsonObject left = new JsonObject();
    JsonArray leftArray = new JsonArray();
    leftArray.add(1);
    leftArray.add(new JsonObject());
    left.add("arr", leftArray);

    JsonObject right = new JsonObject();
    JsonArray rightArray = new JsonArray();
    rightArray.add(2);
    rightArray.add(new JsonObject());
    right.add("arr", rightArray);

    // Also test with a CHANGE entry where type mismatch occurs
    JsonArray leftArray2 = new JsonArray();
    JsonObject leftObj = new JsonObject();
    leftArray2.add(leftObj);
    left.add("arr2", leftArray2);

    JsonArray rightArray2 = new JsonArray();
    rightArray2.add(new JsonPrimitive("str"));
    right.add("arr2", rightArray2);

    JsonDiffResult result = JsonDiff.diff(left, right);
    // Find the entry for $.arr[0] (primitive change)
    DiffEntry primitiveEntry = null;
    DiffEntry objectEntry = null;
    for (DiffEntry e : result.getDifferences()) {
      if (e.getPath().equals("$.arr[0]")) {
        primitiveEntry = e;
      } else if (e.getPath().equals("$.arr2[0]")) {
        objectEntry = e;
      }
    }
    assertThat(primitiveEntry).isNotNull();
    // The old/new values are primitives (deep copies of 1 and 2), not affected by mutations
    assertThat(primitiveEntry.getOldValue()).isEqualTo(new JsonPrimitive(1));
    assertThat(primitiveEntry.getNewValue()).isEqualTo(new JsonPrimitive(2));

    assertThat(objectEntry).isNotNull();
    // Mutate the original object after diff
    leftObj.addProperty("x", 1);
    // The diff entry values should not be affected (they are deep copies)
    assertThat(objectEntry.getOldValue().getAsJsonObject().size()).isEqualTo(0);
  }

  // ---- DiffResult.getDifferences() is unmodifiable ----

  @Test
  public void testDiffResultDifferencesIsUnmodifiable() {
    JsonDiffResult result =
        JsonDiff.diff(new JsonPrimitive(1), new JsonPrimitive(2));
    assertThrows(
        UnsupportedOperationException.class,
        () -> result.getDifferences().add(null));
  }

  // ---- Root level changes ----

  @Test
  public void testDiffRootPrimitiveChange() {
    JsonDiffResult result =
        JsonDiff.diff(new JsonPrimitive(1), new JsonPrimitive(2));
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.getDifferences().get(0).getPath()).isEqualTo("$");
  }

  @Test
  public void testDiffRootTypeChange() {
    JsonDiffResult result =
        JsonDiff.diff(new JsonPrimitive("hello"), JsonNull.INSTANCE);
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.getDifferences().get(0).getPath()).isEqualTo("$");
    assertThat(result.getDifferences().get(0).getOperation()).isEqualTo(DiffOperation.CHANGE);
  }

  // ---- Equals consistency: equals=true implies diff is empty ----

  @Test
  public void testDiffConsistentWithEqualsPrimitives() {
    JsonPrimitive a = new JsonPrimitive(10);
    JsonPrimitive b = new JsonPrimitive(10);
    assertThat(a.equals(b)).isTrue();
    assertThat(JsonDiff.diff(a, b).isEmpty()).isTrue();
  }

  @Test
  public void testDiffConsistentWithEqualsObjects() {
    JsonObject a = new JsonObject();
    a.addProperty("1", true);
    a.addProperty("2", false);
    JsonObject b = new JsonObject();
    b.addProperty("2", false);
    b.addProperty("1", true);
    assertThat(a.equals(b)).isTrue();
    assertThat(JsonDiff.diff(a, b).isEmpty()).isTrue();
  }

  @Test
  public void testDiffConsistentWithEqualsArrays() {
    JsonArray a = new JsonArray();
    a.add(1);
    a.add("x");
    JsonArray b = new JsonArray();
    b.add(1);
    b.add("x");
    assertThat(a.equals(b)).isTrue();
    assertThat(JsonDiff.diff(a, b).isEmpty()).isTrue();
  }

  // ---- DiffEntry.toString() ----

  @Test
  public void testDiffEntryToString() {
    DiffEntry entry =
        new DiffEntry("$.a", DiffOperation.CHANGE, new JsonPrimitive(1), new JsonPrimitive(2));
    String str = entry.toString();
    assertThat(str).contains("$.a");
    assertThat(str).contains("CHANGE");
  }
}
