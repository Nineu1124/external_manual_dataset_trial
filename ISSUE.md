# Issue: 为 Gson 添加 JSON Diff 结构化差异比较工具

## 1. 模拟的用户真实请求

> **[用户]** Gson 目前只能用 `equals()` 判断两个 JsonElement 是否相等，但没有办法知道它们到底哪里不一样。我现在需要对比两个 JSON 的差异，希望有个工具能返回结构化的结果，告诉我每个差异的路径、操作类型（新增/删除/变更）以及变化前后的值。
>
> 比如：
> ```json
> {"name": "Alice", "age": 30}
> ```
> 和
> ```json
> {"name": "Bob", "age": 30, "email": "bob@example.com"}
> ```
> 我希望能得到类似这样的结果：
> - `$.name` 发生了变更（old="Alice", new="Bob"）
> - `$.email` 是新增的（new="bob@example.com"）
>
> 要求：
> 1. 对 Object 按成员名比较（忽略插入顺序），对 Array 按索引比较（顺序敏感）
> 2. 递归比较，不要只报一个笼统的"不一样"，要给出每条差异的精确 JSON Path
> 3. DiffEntry 里保存的值必须是深拷贝，不能因为后续修改原始对象而影响 diff 结果
> 4. diff 结果的列表应该是不可修改的
> 5. diff 语义要和 `JsonElement.equals()` 一致——equals 返回 true 的两个元素，diff 结果应该为空

## 2. 详细问题描述

**影响文件**：Gson 库中缺少 JSON Diff 相关功能，需要新增以下类：
- `gson/src/main/java/com/google/gson/JsonDiff.java`（核心 diff 工具类）
- `gson/src/main/java/com/google/gson/DiffEntry.java`（差异条目）
- `gson/src/main/java/com/google/gson/DiffOperation.java`（差异操作枚举：ADD / REMOVE / CHANGE）
- `gson/src/main/java/com/google/gson/JsonDiffResult.java`（diff 结果容器）

**现象**：
- Gson 当前无法对两个 JsonElement 进行结构化差异比较
- `JsonElement.equals()` 只能判断是否相等，无法得知具体差异
- 用户需要自行编写递归比较逻辑，容易出错且缺乏标准化

**对用户的实际影响**：在需要对比 JSON 配置、API 响应差异等场景下，缺少开箱即用的 diff 工具。

## 3. 根本原因分析

Gson 库从未提供结构化 diff 能力。需要新增一组类来实现此功能：

1. `DiffOperation` 枚举定义三种操作类型：ADD（右侧独有）、REMOVE（左侧独有）、CHANGE（两侧都有但值不同）
2. `DiffEntry` 记录单条差异的路径、操作类型、旧值（深拷贝）和新值（深拷贝）
3. `JsonDiffResult` 作为不可变结果容器，提供 `isEmpty()`、`size()`、`getDifferences()` 方法
4. `JsonDiff` 作为纯静态工具类，提供 `diff(JsonElement, JsonElement)` 方法，递归比较并生成差异列表

## 4. 期望结果

| 验收编号 | 辅助 verifier | 评测动作（运行什么） | 检查位置（检查哪里） | 通过标准（应得到什么） | 常见失败表现 |
|----------|---------------|----------------------|----------------------|------------------------|--------------|
| A1 | P1 | 检查 init 中 `gson/src/main/java/com/google/gson/` 目录 | 源码目录 | 不存在 `JsonDiff.java`、`DiffEntry.java`、`DiffOperation.java`、`JsonDiffResult.java` 任何一个文件 | init 中已包含这些文件（功能已存在） |
| A2 | F1 | 在 final 中运行 `mvn test-compile surefire:test -Dtest=JsonDiffTest -pl gson` | 终端输出 | 35 个测试全部通过，无 Failures、无 Errors | 编译失败或测试不通过 |
| A3 | F2 | 在 final 中运行 `mvn test-compile surefire:test -Dtest=JsonDiffTest#testDiffEntryValuesAreDeepCopies -pl gson` | 终端输出 | 测试通过，确认 DiffEntry 中的值是深拷贝 | 测试失败，说明值是可变引用 |
| A4 | G1 | 在 final 中运行 `mvn test-compile surefire:test -Dtest=JsonObjectTest,JsonArrayTest -pl gson` | 终端输出 | 所有原有测试仍通过，无回归 | 原有测试失败，说明改动破坏了现有功能 |

**补充说明：**

- A2 覆盖的功能验证包括：
  - 相等元素 diff 结果为空
  - Object 成员变更/新增/删除检测
  - Array 元素按索引比较
  - 嵌套结构递归比较
  - 原始值变更检测
  - diff 结果列表不可修改
  - DiffEntry 值为深拷贝
  - null 参数校验
- 运行完整 `mvn test` 时，`EnumWithObfuscatedTest` 会在 base commit 和 final 上均失败，这是环境问题（ProGuard 未正确配置），与本次改动无关

## 5. 改动方案

### 新增文件

1. **`gson/src/main/java/com/google/gson/DiffOperation.java`** — 差异操作枚举（ADD / REMOVE / CHANGE）

2. **`gson/src/main/java/com/google/gson/DiffEntry.java`** — 差异条目数据类，包含 path、operation、oldValue、newValue；oldValue/newValue 均为 deepCopy 快照

3. **`gson/src/main/java/com/google/gson/JsonDiffResult.java`** — 不可变 diff 结果容器，`getDifferences()` 返回 `Collections.unmodifiableList`

4. **`gson/src/main/java/com/google/gson/JsonDiff.java`** — 核心工具类：
   - `diff(JsonElement, JsonElement)` 入口方法
   - `collectDifferences()` 递归比较：先 `equals()` 短路，再按类型分派
   - `diffObjects()` 按 key 比较 Object 成员
   - `diffArrays()` 按索引比较 Array 元素
   - 所有创建 DiffEntry 处均调用 `deepCopy()`

5. **`gson/src/test/java/com/google/gson/JsonDiffTest.java`** — 35 个测试用例

### 修改文件

6. **`gson/src/test/java/com/google/gson/common/MoreAsserts.java`** — 新增 `assertNoDifferences(JsonElement, JsonElement)` 辅助方法

7. **`gson/src/test/java/com/google/gson/JsonArrayTest.java`** — 新增 `testDiffOnEqualArrays`、`testDiffOnDifferentArrays`

8. **`gson/src/test/java/com/google/gson/JsonObjectTest.java`** — 新增 `testDiffOnEqualObjects`、`testDiffIgnoringOrder`、`testDiffOnDifferentObjects`

## 6. 复现步骤

### 第一步：安装初始环境
```bash
bash $ISSUE_ROOT/reproduce.sh
```

### 第二步：激活环境，编译 init，观察输出
```bash
conda activate gson-jsondiff
cd $ISSUE_ROOT/workspace
mvn install -N          # 安装 parent pom 到本地仓库
mvn compile test-compile -pl gson 2>&1 | tee /tmp/init_output.txt
```

### 第三步：验证初始现象
- 检查 `gson/src/main/java/com/google/gson/` 下不存在 `JsonDiff.java`、`DiffEntry.java`、`DiffOperation.java`、`JsonDiffResult.java`（对应 A1）
- 尝试运行 `mvn surefire:test -Dtest=JsonDiffTest -pl gson`，应报错找不到测试类（功能未实现）

### 第四步：验证改动后效果
将 final 的 gson 子目录复制到 workspace 后验证：
```bash
cp -r $ISSUE_ROOT/final/gson/src $ISSUE_ROOT/workspace/gson/
cd $ISSUE_ROOT/workspace
mvn compile test-compile -pl gson

# A2: 运行 JsonDiff 全量测试
mvn surefire:test -Dtest=JsonDiffTest -pl gson
# 预期：Tests run: 35, Failures: 0, Errors: 0

# A3: 验证深拷贝
mvn surefire:test -Dtest=JsonDiffTest#testDiffEntryValuesAreDeepCopies -pl gson
# 预期：测试通过

# A4: 非回归验证
mvn surefire:test -Dtest=JsonObjectTest,JsonArrayTest -pl gson
# 预期：所有原有测试通过
```

## 7. 元信息

- 仓库：https://github.com/google/gson.git
- Base commit：`53d703ee76ca3e951fa4a727307c1f28dbcaf3aa`（2025-04-11）
- 题型：新功能实现
- 难度：中

## 8. 文件清单

| 文件/目录           | 用途说明                     |
| --------------- | ------------------------ |
| ISSUE.md        | Issue 描述、复现步骤、验证方法       |
| reproduce.sh    | 一键安装脚本（conda 环境 + Maven + workspace） |
| environment.yml | conda 环境定义（openjdk=17）    |
| run-tests.sh    | 自动化 verifier 入口          |
| changes.diff    | unified diff（init → final） |
| tests/test_outputs.py | 自动化验证脚本（P1/F1/F2/G1） |
| init/           | base commit 的代码库快照（功能未实现） |
| final/          | 实现功能后的代码库快照              |
| workspace/      | 由 reproduce.sh 自动生成，用于复现 |
