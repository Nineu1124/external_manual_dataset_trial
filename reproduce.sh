#!/bin/bash
set -e
ISSUE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLS_DIR="$HOME/.tools"
ENV_FILE="$HOME/.issue_env"

# ──────────────────────────────────────────
# 1. 安装运行环境
# ──────────────────────────────────────────

# conda 环境
if ! conda env list | grep -q "^gson-jsondiff "; then
    conda env create -f "$ISSUE_DIR/environment.yml"
fi

# Maven（conda-forge 没有标准 Maven 包，手动下载）
mkdir -p "$TOOLS_DIR"
if [ ! -d "$TOOLS_DIR/apache-maven-3.9.9" ]; then
    curl -fsSL "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz" | tar -xz -C "$TOOLS_DIR"
fi
export MAVEN_HOME="$TOOLS_DIR/apache-maven-3.9.9"
export PATH="$MAVEN_HOME/bin:$PATH"
echo "export MAVEN_HOME=$MAVEN_HOME" >> "$ENV_FILE"
echo "export PATH=$MAVEN_HOME/bin:\$PATH" >> "$ENV_FILE"

# 持久化环境变量
[ -f "$ENV_FILE" ] && grep -qF "source $ENV_FILE" "$HOME/.bashrc" || echo "source $ENV_FILE" >> "$HOME/.bashrc"

# ──────────────────────────────────────────
# 2. 将 init/ 复制为 workspace/（此步必须成功）
# ──────────────────────────────────────────
[ -d "$ISSUE_DIR/workspace" ] && rm -rf "$ISSUE_DIR/workspace"
cp -r "$ISSUE_DIR/init" "$ISSUE_DIR/workspace"
