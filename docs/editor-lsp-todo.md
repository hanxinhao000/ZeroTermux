# Editor LSP Completion TODO

## Current Status

- App 入口暂时只显示 TODO，不初始化、下载或启动 LSP server。
- LSP 运行时代码保留，等待稳定性验证后再重新接入编辑器。

## Goal

为 ZeroTermux 内置 Sora CodeEditor 接入 LSP 服务器，实现 VS Code 风格补全，同时保留现有 TextMate 高亮、文件 Tab、侧栏、图片/SVG 预览等能力。

## Architecture

- LSP server 不预置完整包进 APK，由 App 按需下载并安装到 Termux 环境，再通过本地 stdio 启动。
- 首次打开编辑器时自动初始化 Shell 基础 LSP，覆盖 bash/zsh/fish 等 shell 文件的基础补全。
- 用户可在编辑器设置界面额外下载 JSON/JSONC、JS/TS、Python、YAML 等 LSP server。
- App 内实现轻量 LSP stdio JSON-RPC client。
- Sora CodeEditor 负责 UI 与文本编辑；TextMate 继续负责高亮；LSP 负责 completion。
- LSP 功能失败时自动降级，不影响普通编辑。

## MVP Scope

1. 先完成 Shell 基础 LSP 首次初始化和 stdio 补全链路。
2. 支持 `initialize`、`initialized`、`didOpen`、`didChange`、`didClose`、`completion`。
3. 将 LSP CompletionItem 转成 Sora 补全项。
4. 在编辑器设置中增加 LSP 开关、请求超时与按需下载入口。
5. server 下载/启动失败时显示提示，不阻塞文件打开。

## On-demand Server Packages

- Shell 基础（bash/zsh/fish）：`bash-language-server`，首次打开自动初始化。
- JSON/JSONC：`vscode-langservers-extracted`。
- JS/TS：`typescript` + `typescript-language-server`。
- Python：`pyright`。
- YAML：`yaml-language-server`。

## Implementation TODO

- [x] 检查 Sora 0.24.4 补全 API：`CompletionPublisher`、`CompletionItem`、`Language` 扩展点。
- [x] 新建 LSP JSON-RPC stdio client。
- [x] 实现 stdio `Content-Length` 分帧读写。
- [x] 实现 request id、response callback、timeout、error handling。
- [x] 实现 LSP `initialize` / `initialized`。
- [x] 实现 `textDocument/didOpen` / `didChange` / `didClose`。
- [x] 实现 `textDocument/completion` 请求。
- [x] 实现 UTF-16 LSP Position 与编辑器行列转换。
- [x] 新增语言到 Termux 环境中已下载 LSP server 启动命令映射。
- [x] 新增编辑器设置项：启用 LSP、按需下载 LSP server、请求超时。
- [x] 将 LSP `CompletionItem` 转为 Sora 补全候选。
- [x] 支持 `label` / `detail` / `documentation` / `insertText` / `textEdit`。
- [x] 首次打开自动初始化 Shell 基础 LSP。
- [x] 提供 JSON/JSONC、JS/TS、Python、YAML 的按需下载入口。
- [ ] 验证 JS/TS、Python、Shell、YAML 运行效果。
- [x] Activity 销毁时关闭 LSP 进程。
- [x] 资源/JSON/XML 校验。
- [ ] 有 Java 环境时运行 `./gradlew :app:compileDebugKotlin :app:compileDebugJavaWithJavac`。

## Risks

- Termux 环境下 npm、nodejs 和网络源可用性差异较大，按需下载必须失败可恢复。
- LSP completion 需要确认 Sora 0.24.4 的补全 API，避免与 `TextMateLanguage` 冲突。
- 频繁 `didChange` / `completion` 可能卡顿，需要防抖和超时。
- 大文件需要限制 LSP 同步和补全频率。
