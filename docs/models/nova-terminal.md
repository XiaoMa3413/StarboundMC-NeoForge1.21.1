# N.O.V.A. 壁挂通讯终端

在 Blockbench 5.2.1 中通过本地 Blockbench MCP 制作。使用 Generic Model 网格、独立材质与逐面 UV；运行时使用 NeoForge 内置 OBJ 加载器。

## 造型与动态

- 切角钛灰护框、石墨外壳、墙面安装座、侧面散热槽与向前伸出的通讯下颚。
- 屏幕为第一视觉主体，沿用球体、斜轨道和独立眼睛的 N.O.V.A. 身份。
- 下方铭牌、两个通讯键、麦克风孔及顶部小型传感器，发光集中在屏幕和状态标记。
- 49 个网格、1489 个三角形/四边形面、6 个分组、7 个运行时材质。
- 屏幕每帧 128×112，48 帧、每帧 2 tick，循环 4.8 秒：1 像素悬浮、微小视线移动、短促眨眼和低亮投影环。
- 这是环境待机循环，不反映剧情状态或实时说话状态。打开后的 GUI 保留原有状态动画和剧情交互。

## 资产

- `nova-terminal-textured-v1.bbmodel`：可编辑源模型，包含完整动画条带与独立 `nova_body`、`nova_eyes` 源图；Blockbench 纹理播放速度为 10 fps。
- `nova-terminal-export.obj`：通过 MCP `export_model` 得到的实际 OBJ 导出。
- `../../tools/blockbench_nova_terminal.js`：Blockbench 内执行的建模与绘制过程，不是普通 Node.js 几何生成器。
- `../../tools/export_nova_terminal.py`：从上述源文件包装游戏资源、材质与原版 `.mcmeta`，验证坐标、UV、索引、非退化面、不透明材质及动画帧。
- `previews/nova-terminal.png`：Blockbench 外观预览；`previews/nova-screen.gif`：屏幕动画预览。
- `previews/nova-terminal-ingame.gif`：游戏连续截图组成的动态预览；`nova-terminal-facings.png` 与 `nova-terminal-blink.png` 记录四向和闭眼检查。
- 游戏资源：`models/block/nova_terminal.obj/.mtl`、`models/block/ship_ai_terminal.json`、`textures/block/nova_*.png`。

保留 `ship_ai_terminal` 方块 ID、物品、战利品、原有舰船位置、四向放置、右键菜单与剧情快照。更新选择/碰撞轮廓以匹配安装座、上部护框和下部通讯键，所有几何位于单方块内。

## 编辑与导出

优先直接打开 `.bbmodel` 编辑。重新制作时创建独立 Generic Model 项目，导入现有 `textures/gui/ship_ai/nova_body.png` 与 `nova_eyes.png`，创建 `mount`、`housing`、`bezel`、`display`、`controls`、`ventilation` 分组，然后通过 MCP `risky_eval` 执行建模脚本（该旧版 MCP 要求移除文件开头的注释）。先保存 checkpoint；不要在已有模型上重复执行生成流程。

导出前将纹理动画停在第 0 帧。通过 MCP `export_model` 分别保存 `project` 和 `obj`，使用 `options: {}` 避免交互导出窗口。然后运行：

```powershell
python tools/export_nova_terminal.py
.\gradlew.bat test build
.\gradlew.bat runClient -PnovaTerminalSmoke
```

旧 `generate-command-deck-models.py` 遇到已有 OBJ 模型时保留其文件，避免覆盖已制作的终端。

## 验证

资源测试检查动画有实际变化、眨眼确实闭合、源图保持独立且与 GUI 原图一致、材质引用完整及 UV 不越界。客户端夹具在全新 `run-nova-terminal-smoke` 世界中验证四向模型、前方留空、安装座、动画帧加载、交互、物品模型及资源重载，并保存连续动画截图供视觉检查。

2026-09-24：673 项测试通过，独立客户端夹具全部通过。55 张连续截图中的屏幕区域均有变化，闭眼帧的眼部亮像素降至睁眼帧的约 18%；已视觉检查四向外观、铭牌方向、游戏内闭眼以及原有对话菜单。
