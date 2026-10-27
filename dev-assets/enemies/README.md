# 敌人设计与原生制作源

本目录保存首批四种机械敌人的设计、概念、可编辑 Blockbench 工程及导出回执。只加入四枚刷怪蛋，不添加自然刷新。玩法合同见 [enemies reference](../../docs/reference/enemies.md)，具体造型与攻击设计见 [design.md](design.md)。

## 资产来源

- 概念及正/左侧/背视图由内置 imagegen 生成，完整提示词为 [concept-prompts.json](concept-prompts.json)，修订提示词见 [concept-edit-prompts.json](concept-edit-prompts.json)。[概念清单](concepts/manifest.json) 记录来源、尺寸和SHA256。
- 监工使用 `quarry_warden-v2.png`：修正左侧视图近侧工具为冲击锤，保留旧图供回溯。
- `models/*.bbmodel` 是真正的 Blockbench 5.2.2 free mesh 工程。脚本通过本机 Blockbench API 创建网格、骨骼与纹理，原生保存后重开；不是用概念图或离线渲染冒充建模界面。
- `geometry/*.json` 来自重开工程后的实际渲染几何，附原生源哈希。`export-validation.json` 记录生产资源、纹理和源文件哈希。
- 纹理采用概念三视图投影和机械断面像素材质，保留概念中的绘制光照。当前是首轮可测试造型，不宣称自动达到95%美术匹配率；最终由用户审查。

## 可复现流程

1. 启动 `../工具/Blockbench/Blockbench_5.2.2_portable.exe` 对应工作区工具（实际路径相对仓库为 `../工具/Blockbench`），启用本机9228调试端口。
2. 修改 `scripts/enemy_model_specs.py`，执行 `python scripts/enemy_models.py [实体ID…]`。它会覆盖指定制作源；旧工程、截图与回执先归档至概念图目录的 `iterations`。
3. `python scripts/validate_enemy_models.py` 检查封闭边、绕序、有限顶点与UV范围。
4. `python scripts/export_enemy_models.py` 检查源/缓存哈希、骨骼父子结构，转换为游戏坐标并导出资源；禁止手改缓存绕过源文件检查。
5. `python scripts/enemy_model_review.py` 生成正/侧/背概念与实际模型对照，不拉伸比例，不生成虚构评分。
6. 运行 reference 中的服务端和客户端测试，再进行默认正式构建。

动画由游戏代码驱动骨骼；bbmodel 保存机械层级与枢轴，不包含预烘焙动画时间轴。巡逻机后部风扇叶片拥有独立 `rear_rotor`，护罩不随叶片转动。

## 审查文件

工作区交付目录为 `../概念图/2026-10-27_敌人设计与建模/`，`index.html` 汇总概念、三视图、Blockbench完整界面截图、对照板、原生工程链接及游戏截图。`delivery.json` 保存最终构建、安装与截图哈希。

概念、制作源和回执进入Git；完整截图及自动迭代历史放在工作区审查目录，避免让生产资源包携带开发证据。数值匹配率和最终美术通过状态不由脚本自行确认。
