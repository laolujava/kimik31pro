#!/bin/bash

# GitHub 仓库推送脚本
# 此脚本帮助你快速将项目推送到 GitHub

echo "=========================================="
echo "GitHub 仓库推送助手"
echo "=========================================="
echo ""

# 检查是否已配置远程仓库
if git remote | grep -q "origin"; then
    echo "✅ 检测到已配置的远程仓库:"
    git remote -v
    echo ""
    read -p "是否要推送到此远程仓库? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git push -u origin master
        echo ""
        echo "✅ 推送完成!"
    fi
else
    echo "⚠️ 未检测到远程仓库配置"
    echo ""
    echo "请先在 GitHub 上创建仓库，然后运行:"
    echo ""
    echo "  git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git"
    echo "  git branch -M main"
    echo "  git push -u origin main"
    echo ""
    echo "或者使用 GitHub CLI:"
    echo ""
    echo "  gh repo create api-tutorial-examples --public --source=. --push"
    echo ""
fi
