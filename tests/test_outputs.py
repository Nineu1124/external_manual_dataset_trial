#!/usr/bin/env python3
"""
Verifier for gson-json-diff-utility issue package.

Tests:
  P1: JsonDiff class does not exist in init (problem reproduction)
  F1: JsonDiff.diff() produces correct structured diff results (functional verification)
  F2: DiffEntry values are deep copies, not mutable references (functional verification)
  G1: Existing Gson core classes still work correctly (non-regression)
"""

import subprocess
import sys
import os

ISSUE_DIR = os.path.join(os.path.dirname(__file__), "..")
WORKSPACE = os.environ.get("ISSUE_WORKSPACE", os.path.join(ISSUE_DIR, "workspace"))
MAVEN_CMD = os.environ.get("MAVEN_CMD", "mvn")


_parent_installed = set()


def _install_parent_pom(workspace):
    """Install the parent pom to local Maven repo (idempotent per workspace)."""
    ws_abs = os.path.abspath(workspace)
    if ws_abs in _parent_installed:
        return True, "already installed"
    cmd = [MAVEN_CMD, "install", "-N", "-pl", ""]
    result = subprocess.run(cmd, cwd=ws_abs, capture_output=True, text=True, timeout=120)
    if result.returncode == 0:
        _parent_installed.add(ws_abs)
    return result.returncode == 0, result.stdout + result.stderr


def run_maven_test(test_class, test_method=None, workspace=None):
    """Run a specific Maven test and return (success, output).

    workspace should point to the multi-module project root (containing parent pom.xml).
    Tests are run on the 'gson' module via -pl gson.
    """
    ws = workspace or WORKSPACE
    # Ensure parent pom is installed so the module can resolve it
    _install_parent_pom(ws)
    if test_method:
        test_spec = f"{test_class}#{test_method}"
    else:
        test_spec = test_class
    # surefire:test does not auto-compile; must compile first
    cmd = [MAVEN_CMD, "test-compile", "surefire:test", f"-Dtest={test_spec}", "-pl", "gson"]
    result = subprocess.run(cmd, cwd=ws, capture_output=True, text=True, timeout=300)
    return result.returncode == 0, result.stdout + result.stderr


def run_maven_compile(workspace=None):
    """Run mvn compile test-compile and return (success, output)."""
    ws = workspace or WORKSPACE
    _install_parent_pom(ws)
    cmd = [MAVEN_CMD, "compile", "test-compile", "-pl", "gson"]
    result = subprocess.run(cmd, cwd=ws, capture_output=True, text=True, timeout=300)
    return result.returncode == 0, result.stdout + result.stderr


def check_class_not_exists(class_name, module_dir):
    """Check that a Java source file does NOT exist in the module directory."""
    path = os.path.join(module_dir, "src", "main", "java", "com", "google", "gson", f"{class_name}.java")
    return not os.path.exists(path)


def check_class_exists(class_name, module_dir):
    """Check that a Java source file EXISTS in the module directory."""
    path = os.path.join(module_dir, "src", "main", "java", "com", "google", "gson", f"{class_name}.java")
    return os.path.exists(path)


# ── P1: Problem reproduction ──

def test_P1_jsondiff_not_in_init():
    """P1: JsonDiff and related classes do NOT exist in init (feature not implemented)."""
    init_gson = os.path.join(ISSUE_DIR, "init", "gson")
    assert check_class_not_exists("JsonDiff", init_gson), "JsonDiff.java should NOT exist in init"
    assert check_class_not_exists("DiffEntry", init_gson), "DiffEntry.java should NOT exist in init"
    assert check_class_not_exists("DiffOperation", init_gson), "DiffOperation.java should NOT exist in init"
    assert check_class_not_exists("JsonDiffResult", init_gson), "JsonDiffResult.java should NOT exist in init"
    print("PASS: P1 - JsonDiff classes do not exist in init")


# ── F1: Functional verification ──

def test_F1_jsondiff_functionality():
    """F1: JsonDiff.diff() produces correct structured diff results."""
    final_gson = os.path.join(ISSUE_DIR, "final", "gson")
    final_root = os.path.join(ISSUE_DIR, "final")
    assert check_class_exists("JsonDiff", final_gson), "JsonDiff.java must exist in final"
    assert check_class_exists("DiffEntry", final_gson), "DiffEntry.java must exist in final"
    assert check_class_exists("DiffOperation", final_gson), "DiffOperation.java must exist in final"
    assert check_class_exists("JsonDiffResult", final_gson), "JsonDiffResult.java must exist in final"

    # Run the full JsonDiffTest suite
    success, output = run_maven_test("JsonDiffTest", workspace=final_root)
    assert success, f"JsonDiffTest should pass in final. Output:\n{output[-2000:]}"
    print("PASS: F1 - JsonDiff functionality works correctly")


# ── F2: Deep copy verification ──

def test_F2_deep_copy():
    """F2: DiffEntry values are deep copies, not mutable references."""
    final_root = os.path.join(ISSUE_DIR, "final")
    success, output = run_maven_test("JsonDiffTest", "testDiffEntryValuesAreDeepCopies", workspace=final_root)
    assert success, f"testDiffEntryValuesAreDeepCopies should pass. Output:\n{output[-2000:]}"
    print("PASS: F2 - DiffEntry values are deep copies")


# ── G1: Non-regression ──

def test_G1_existing_classes_work():
    """G1: Existing Gson core classes (JsonObject, JsonArray) still work correctly."""
    final_root = os.path.join(ISSUE_DIR, "final")
    success, output = run_maven_test("JsonObjectTest,JsonArrayTest", workspace=final_root)
    assert success, f"JsonObjectTest and JsonArrayTest should still pass. Output:\n{output[-2000:]}"
    print("PASS: G1 - Existing Gson classes work correctly (non-regression)")


if __name__ == "__main__":
    tests = [
        ("P1", test_P1_jsondiff_not_in_init),
        ("F1", test_F1_jsondiff_functionality),
        ("F2", test_F2_deep_copy),
        ("G1", test_G1_existing_classes_work),
    ]

    passed = 0
    failed = 0
    for name, test_fn in tests:
        try:
            test_fn()
            passed += 1
        except Exception as e:
            print(f"FAIL: {name} - {e}")
            failed += 1

    print(f"\n{'='*40}")
    print(f"Results: {passed} passed, {failed} failed out of {len(tests)}")
    if failed > 0:
        sys.exit(1)
