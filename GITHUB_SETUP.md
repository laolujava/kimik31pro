# 如何推送到 GitHub

本仓库已经初始化并提交了所有代码。要将其推送到 GitHub，请按照以下步骤操作：

## 方式 1: 使用 GitHub 网页创建仓库

1. 访问 https://github.com/new
2. 创建一个新仓库，名称建议为 `api-tutorial-examples`
3. 不要添加 README、.gitignore 或 LICENSE（我们已经有了）
4. 创建后，GitHub 会显示推送命令，类似于：

```bash
cd /g/aicode6/api-tutorial-examples
git remote add origin https://github.com/YOUR_USERNAME/api-tutorial-examples.git
git branch -M main
git push -u origin main
```

## 方式 2: 使用 GitHub CLI (需要先安装)

```bash
cd /g/aicode6/api-tutorial-examples

# 安装 GitHub CLI (如果还没安装)
# Windows: winget install --id GitHub.cli
# 或下载: https://cli.github.com/

# 登录
gh auth login

# 创建仓库并推送
gh repo create api-tutorial-examples --public --source=. --push
```

## 方式 3: 如果你已经创建了 GitHub 仓库

```bash
cd /g/aicode6/api-tutorial-examples
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
git branch -M main
git push -u origin main
```

## 验证推送成功

推送成功后，访问你的 GitHub 仓库页面，你应该能看到：

- ✅ 完整的项目结构
- ✅ Python、JavaScript 和 Java 示例代码
- ✅ 各语言的 README 文档
- ✅ 主 README.md 文件
- ✅ .gitignore 文件

## 推荐的仓库设置

创建仓库时的建议配置：

- **仓库名称**: `api-tutorial-examples`
- **描述**: `Complete API tutorial with Python, JavaScript, and Java examples - authentication, streaming, error handling, token billing`
- **可见性**: Public（公开）
- **不要勾选**: "Add a README file"（我们已经有了）
- **不要勾选**: "Add .gitignore"（我们已经有了）
- **不要勾选**: "Choose a license"（可选）

## 可选：添加 Topics

推送成功后，可以在 GitHub 仓库页面添加以下 topics 标签：

- `api`
- `tutorial`
- `python`
- `javascript`
- `java`
- `openai-api`
- `examples`
- `streaming`
- `error-handling`

这样可以让其他开发者更容易找到你的项目！
