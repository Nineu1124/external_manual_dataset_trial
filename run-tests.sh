#!/bin/bash
set -e
ISSUE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 激活 conda 环境
eval "$(conda shell.bash hook)"
conda activate gson-jsondiff

# 设置 Maven（如果通过 reproduce.sh 安装了）
if [ -d "$HOME/.tools/apache-maven-3.9.9" ]; then
    export MAVEN_HOME="$HOME/.tools/apache-maven-3.9.9"
    export PATH="$MAVEN_HOME/bin:$PATH"
fi

# 设置环境变量
export ISSUE_WORKSPACE="$ISSUE_DIR/workspace"
export MAVEN_CMD="mvn"

# 运行 verifier
python "$ISSUE_DIR/tests/test_outputs.py"
