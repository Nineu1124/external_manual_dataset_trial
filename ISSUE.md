# gson-type-adapter-helpers

## 1. 模拟的用户真实请求

> **[用户]** 我在使用 Gson 的 TypeAdapter，发现只有 `nullSafe()` 这一个包装方法。我现在有个需求：有些自定义 TypeAdapter 只需要支持反序列化（read），序列化方向应该明确报错而不是默默写出错误数据或者抛不清晰的异常。Gson 的 TypeAdapter 里有没有类似 `readOnly()` 这样的辅助方法？
>
> 我翻了一下 `TypeAdapter.java`，目前只有 `nullSafe()` 一个包装方法，没有提供限制适配器方向的机制。如果要实现一个只读适配器，我得自己写一个包装类，这跟 `nullSafe()` 的使用体验差距很大——`nullSafe()` 一行调用就搞定了，只读却要我手写一整个内部类。
>
> 能不能给 TypeAdapter 加一个 `readOnly()` 方法，风格跟 `nullSafe()` 一致？就是调用 `.readOnly()` 之后，`read()` 正常委托，`write()` 直接抛 `UnsupportedOperationException`。这样注册到 GsonBuilder 的时候直接链式调用 `.readOnly()` 就行了。

## 2. 详细问题描述

- **影响文件**：`gson/src/main/java/com/google/gson/TypeAdapter.java`，`TypeAdapter<T>` 类（约第 291 行 `nullSafe()` 方法附近）
- **现象**：`TypeAdapter` 只提供了 `nullSafe()` 一个装饰器方法，缺少限制适配器方向的辅助方法。当用户只需要反序列化时，没有官方方式让 `write()` 方向明确报错
- **对用户的实际影响**：
  - 用户需要自己手写只读包装类，代码冗余且风格与 `nullSafe()` 不一致
  - 容易在只读适配器中忘记处理 `write()` 方向，导致运行时出现难以定位的错误
  - 与 `nullSafe()` 的链式调用体验（如 `.nullSafe()`）不统一

## 3. 根本原因分析

`TypeAdapter.java` 中 `nullSafe()` 方法（第 291-321 行）建立了装饰器模式的标准范式：

```java
public final TypeAdapter<T> nullSafe() {
    if (!(this instanceof TypeAdapter.NullSafeTypeAdapter)) {
        return new NullSafeTypeAdapter();
    }
    return this;
}

private final class NullSafeTypeAdapter extends TypeAdapter<T> {
    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) { out.nullValue(); }
        else { TypeAdapter.this.write(out, value); }
    }
    @Override
    public T read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) { reader.nextNull(); return null; }
        return TypeAdapter.this.read(reader);
    }
    @Override
    public String toString() {
        return "NullSafeTypeAdapter[" + TypeAdapter.this + "]";
    }
}
```

但缺少与之对称的 `readOnly()` 装饰器方法，导致用户无法便捷地创建只读适配器。

## 4. 期望结果

| 验收编号 | 辅助 verifier | 评测动作（运行什么） | 检查位置（检查哪里） | 通过标准（应得到什么） | 常见失败表现 |
|----------|---------------|----------------------|----------------------|------------------------|--------------|
| A1 | F1 | 在 workspace/gson 中运行 `mvn test -Dtest=TypeAdapterTest` | 终端输出 | 测试全部通过（0 Failures, 0 Errors） | 测试失败或有编译错误 |
| A2 | F2 | 在 TypeAdapter.java 中搜索 `readOnly` 方法 | 源码 | 存在 `public final TypeAdapter<T> readOnly()` 方法声明 | 方法不存在 |
| A3 | F3 | 在 TypeAdapterTest.java 中搜索 `testReadOnly` | 测试源码 | 存在 `testReadOnly` 相关测试方法 | 测试方法不存在 |
| A4 | F1 | 构造一个 TypeAdapter 并调用 `.readOnly()`，然后调用 `write()` 方向方法 | 程序行为 | `write()` 方向抛出 `UnsupportedOperationException` | 未抛异常或抛其他类型异常 |
| A5 | F1 | 构造一个 TypeAdapter 并调用 `.readOnly()`，然后调用 `read()` 方向方法 | 程序行为 | `read()` 方向正常委托，返回正确结果 | read 方向也抛异常或返回错误结果 |

## 5. 改动方案

### TypeAdapter.java（+31 行）

在 `NullSafeTypeAdapter` 内部类之后，添加 `readOnly()` 方法和 `ReadOnlyTypeAdapter` 内部类：

```diff
+  public final TypeAdapter<T> readOnly() {
+    if (!(this instanceof TypeAdapter.ReadOnlyTypeAdapter)) {
+      return new ReadOnlyTypeAdapter();
+    }
+    return this;
+  }
+
+  private final class ReadOnlyTypeAdapter extends TypeAdapter<T> {
+    @Override
+    public void write(JsonWriter out, T value) {
+      throw new UnsupportedOperationException("Cannot write with a read-only type adapter");
+    }
+
+    @Override
+    public T read(JsonReader reader) throws IOException {
+      return TypeAdapter.this.read(reader);
+    }
+
+    @Override
+    public String toString() {
+      return "ReadOnlyTypeAdapter[" + TypeAdapter.this + "]";
+    }
+  }
```

### TypeAdapterTest.java（+29 行）

新增 3 个测试方法：
- `testReadOnly()`：验证 read 正常委托、write 抛 `UnsupportedOperationException`
- `testReadOnly_ReturningSameInstanceOnceReadOnly()`：验证幂等性
- `testReadOnly_ToString()`：验证描述字符串格式

## 6. 复现步骤

### 第一步：安装初始环境
```bash
bash $ISSUE_ROOT/reproduce.sh
```

### 第二步：激活环境，验证初始状态缺少 readOnly()
```bash
conda activate gson-type-adapter-helpers
cd $ISSUE_ROOT/workspace/gson
# 验证 TypeAdapter 中没有 readOnly 方法
grep -n "readOnly" src/main/java/com/google/gson/TypeAdapter.java && echo "FAIL: readOnly exists" || echo "PASS: readOnly does not exist in init"
# 验证 TypeAdapterTest 中没有 testReadOnly 测试
grep -n "testReadOnly" src/test/java/com/google/gson/TypeAdapterTest.java && echo "FAIL: testReadOnly exists" || echo "PASS: testReadOnly does not exist in init"
```

### 第三步：验证初始现象
- TypeAdapter.java 中不存在 `readOnly()` 方法
- TypeAdapterTest.java 中不存在 `testReadOnly` 测试
- 构建应正常通过：`mvn compile -q`

### 第四步：验证改动后效果
将 final 中的改动文件复制到 workspace：
```bash
cp $ISSUE_ROOT/final/gson/src/main/java/com/google/gson/TypeAdapter.java $ISSUE_ROOT/workspace/gson/src/main/java/com/google/gson/TypeAdapter.java
cp $ISSUE_ROOT/final/gson/src/test/java/com/google/gson/TypeAdapterTest.java $ISSUE_ROOT/workspace/gson/src/test/java/com/google/gson/TypeAdapterTest.java
```

按验收编号逐条检查：

- **A1**：运行 `mvn test -Dtest=TypeAdapterTest -Dsurefire.useFile=false` → 测试全部通过（0 Failures, 0 Errors）
- **A2**：`grep -n "readOnly" src/main/java/com/google/gson/TypeAdapter.java` → 找到 `public final TypeAdapter<T> readOnly()` 方法声明
- **A3**：`grep -n "testReadOnly" src/test/java/com/google/gson/TypeAdapterTest.java` → 找到 3 个测试方法
- **A4**：readOnly() 包装后 write 方向抛 UnsupportedOperationException（由 testReadOnly 测试覆盖）
- **A5**：readOnly() 包装后 read 方向正常委托（由 testReadOnly 测试覆盖）

## 7. 元信息

- 仓库：https://github.com/google/gson.git
- Base commit：`53d703ee76ca3e951fa4a727307c1f28dbcaf3aa`（2025-09-10）
- 题型：新功能实现
- 难度：低

## 8. 文件清单

| 文件/目录 | 用途说明 |
|-----------|----------|
| ISSUE.md | Issue 描述、复现步骤、验证方法 |
| reproduce.sh | 一键安装脚本 |
| environment.yml | conda 环境定义（openjdk=17, maven=3.9） |
| run-tests.sh | 自动化验证脚本 |
| changes.diff | init/final 的 unified diff |
| init/ | base commit 的代码库快照 |
| final/ | 实现功能后的代码库快照 |
| workspace/ | 由 reproduce.sh 自动生成，用于复现 |
