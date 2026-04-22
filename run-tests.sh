#!/bin/bash
set -e
ISSUE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

conda activate gson-type-adapter-helpers

cd "$ISSUE_DIR/workspace/gson"

echo "=== Running TypeAdapterTest ==="
mvn test -pl gson -Dtest=TypeAdapterTest -Dsurefire.useFile=false -q 2>&1

echo ""
echo "=== Checking readOnly() method exists in TypeAdapter ==="
if grep -q "public final TypeAdapter<T> readOnly()" gson/src/main/java/com/google/gson/TypeAdapter.java; then
    echo "PASS: readOnly() method found in TypeAdapter.java"
else
    echo "FAIL: readOnly() method NOT found in TypeAdapter.java"
    exit 1
fi

echo ""
echo "=== Checking readOnly tests exist ==="
if grep -q "testReadOnly" gson/src/test/java/com/google/gson/TypeAdapterTest.java; then
    echo "PASS: testReadOnly tests found in TypeAdapterTest.java"
else
    echo "FAIL: testReadOnly tests NOT found in TypeAdapterTest.java"
    exit 1
fi
