# 来源与项目适配

- 上游：https://github.com/alchaincyf/zhangxuefeng-skill
- 固定提交：3501b1e679cf595a6af800619af757aba46d7574
- 许可证：MIT，保留在 LICENSE。
- UPSTREAM-SKILL.md、references/research/ 和 examples/demo-conversation.md 为该版本原始文件。
- SKILL.md 是本项目的适配入口：默认绑定分析框架，去掉关键词激活与退出角色机制，明确身份、证据和现有工具边界。
- resource-index.json 是 Java Agent 允许模型按需读取的资料目录。Java 不加载原始入口；历史 Python Agent 通过 UPSTREAM-SKILL.md 保持原有行为。
- 未引入展示动画 assets/hero.gif、微信二维码和上游宣传 README；它们不参与咨询运行。

研究资料保留上游原文，不表示本项目已独立核实其中的人物、统计或政策陈述。资料与示例不可替代业务查询和当前官方信息。

Maven 将本目录的 Markdown、JSON 和许可证打包进 classpath。修改后需重新构建、重启；运行时不会访问 GitHub，不依赖启动工作目录，也不自动更新上游。
