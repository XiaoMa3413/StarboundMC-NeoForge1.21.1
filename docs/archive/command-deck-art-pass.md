# NOVA 与指挥设备美化

> 状态：已完成并归档（2026-09-17）。模型、贴图、侧面 UV 修订和设备界面已按用户反馈落地；后续
> 美术调整应另建计划并重新进行视觉核验。

本次以深蓝石墨外壳、钛灰护框、青色显示和少量琥珀色标记统一设备外观。

- NOVA 保留 96×112 的球体、斜轨道与独立眼睛层，更新球体明暗和投影纹理。
  UI 新增底部流动投影环与外围数据光点；离线隐藏新增光效，重启减弱，警告转琥珀色。
  既有说话亮度、眨眼、注视、扫描与故障反馈继续使用原状态来源。
- AI 终端为 16 个长方体组成的壁挂通讯面板，小屏幕使用新 NOVA 的三帧眼睛贴图。
  方块屏幕是循环装饰动画，不表示实时服务端剧情状态。
- 星图导航台为 12 个长方体组成的独立桌式设备，带底座、内嵌轨道图、控制区与后部节点。
  桌面轨道图是装饰贴图，实际航线仍由打开后的星图界面显示。
- 两台设备保留注册 ID、方向状态、菜单与网络链路；飞船模板没有改动。
  星图台碰撞形状按四个方向缓存，终端前框轮廓同步调整。

## 资源和复现

- `src/main/resources/assets/starboundmc/textures/block/command_deck_atlas.png`：256×256 四区高清图集。
- `textures/block/command_deck_edges.png`：1024×1024 打包图，存放 34 种按面尺寸直接绘制的完整侧板/护边，四像素采样留边。
- `textures/gui/ship_ai/nova_body.png`、`nova_bust.png`：均相对于上述 assets 目录，96×112。
- `textures/block/ship_ai_terminal_screen.png`：48×96，三个 48×32 动画帧，配套 mcmeta。
- `scripts/generate-command-deck-models.py`：重建两个方块模型及专用侧面图集，保留高清主图集。
- `scripts/command_deck_edge_art.py`：按尺寸绘制完整接缝、螺钉、灯带、散热板与按键，不裁用主图集。
- `scripts/import-command-deck-art.py ATLAS_PNG NOVA_BODY_PNG`：将生成原稿按最近邻缩放导入，
  保留现有眼睛层注册位置，合成静态回退图及方块动画帧。
- `scripts/preview-command-deck.py`：输出 `output/command-deck-model-preview.png`。
  预览是带深度遮挡的正交模型检查图，不是游戏截图，不模拟游戏光照。

贴图使用内置 imagegen 工具生成；没有调用外部 API/CLI。
最终运行资源均在项目内，构建和模型重建不依赖用户目录中的生成原稿。

## 生成提示词

当前高清图集提示词：

> Create a production Minecraft sci-fi block TEXTURE ATLAS, square 1024x1024. Exactly four equal square tiles in 2x2 layout, no gaps or margins, no perspective, flat orthographic UV textures. Crisp pixel art made on a coarse 128x128 pixel grid then nearest-neighbor upscaled. Top left tile: dark blue graphite armored equipment casing, subtle bevel perimeter, inset maintenance panels, four corner bolts, slim ventilation slits, restrained steel wear. Top right tile: brushed blue gray titanium structural frame panel, strong bevel edges, inset dark center and thin cyan circuit accents, small amber caution markings. Bottom left tile: dark spaceship control keypad, orderly rows of tactile small gray keys, two cyan data bars, one amber switch; no text. Bottom right tile: luminous cyan astronomical radar star chart on dark navy screen, concentric circular orbit tracks, sparse bright star points joined by navigation route, central warm amber sun, small edge telemetry strokes. All four squares fill their exact quadrants edge to edge. Consistent tasteful Starbound pixel science fiction aesthetic, legible at low resolution. No labels, no letters, no mockup, no objects outside the four tiles.

NOVA（以原 `nova_body.png` 为编辑参考）：

> Edit this game portrait base layer with identity and layout locked. Output portrait aspect 6:7 (original 96x112). This is NOVA, a friendly cyan spherical holographic AI with one diagonal orbital ring. Preserve EXACT sphere center, size, ring silhouette and angle; preserve blank eye area because eyes are a separate animated game layer and must NOT be painted here. Keep the dark navy opaque background. Refine into crisp polished pixel art: clearer glass sphere shading, delicate cyan rim highlights, very restrained internal hologram latitude arcs, tiny light pixels on existing ring. Keep center face dark and empty, NO eyes, NO face, NO text, NO added UI, NO additional rings. Coarse original pixel scale, no smooth painted or photorealistic treatment. Full image framing identical to reference.

## 像素密度与 UV 修订

最终按用户最新反馈恢复高清图集和 48×32 方块屏幕，侧面改为程序化专用绘制。
东/西侧面以及短边不超过四个模型单位的护边、薄面，使用独立完整设计的矩形材质。
每种尺寸独立绘制完整边框、端部螺钉、接缝和方向性装饰，再打包进图集；UV 指向完整小面板，
不从方形大面板截取装饰。其余大面积面与星图显示继续保留高清原稿。
结构面横纵均为每模型单位 8 纹素，星图显示区域保持横纵相同缩放比例并居中裁切，避免轨道被拉伸。
NOVA 的 GUI 头像仍保留原有 96×112 注册尺寸。低像素试稿未被采用。

未采用的低像素试稿使用内置 imagegen，提示词留档：

> Redesign this Minecraft block texture atlas to fit VANILLA Minecraft 16x16 textures. Exactly 2x2 equal square quadrants, edge to edge, no gaps. The ENTIRE image must look like exactly 32x32 logical pixels enlarged with nearest neighbor, so each quadrant has ONLY 16x16 large pixels. Hard pixel edges. Extremely simple flat colors, total palette about 12 colors. NO scratches, no grain, no micro-details, NO anti-aliasing or smooth glow or gradients. Top left: simple dark slate blue metal with a couple broad panel seams, mostly clean flat color. Top right: plain medium slate metal trim with one narrow cyan stripe near top; no framed screen, no bolts or devices, mostly flat metal for cropping onto narrow structural faces. Bottom left: simple control panel with a few large 2x2 pale gray keys and two amber keys on dark background, coarse cyan line at top. Bottom right: simple dark navy star map, ONE stepped cyan orbit circle, amber center pixel, 4 or 5 isolated stars. This is an actual flat UV texture sheet, not a mockup. Strong vanilla Minecraft pixel texture simplicity is mandatory; remove ALL high fidelity rendering from reference.

## 验证

- LDLib2 基线脚本通过：Minecraft 1.21.1 / NeoForge 21.1.248 / LDLib2 2.2.36.a。
- 首轮构建编译通过，资源测试因旧贴图命名断言失败；已更新为新图集、独立模型和动画帧尺寸检查。
- 第一版 `gradlew.bat test build` 通过：353 项测试，0 失败、0 错误、0 跳过。
- 本轮检查 UV 不拉伸、结构面纹素密度及侧面必须使用专用材质；最终 `gradlew.bat test build` 通过，354 项测试全部通过。
- `git diff --check` 通过；仅有仓库换行符转换提示。
- 已检查实际合成头像、恢复后的高清图集与侧面 UV 修订模型预览。
- 用户已通过当前版本的游戏内视觉核验；这里保留当时的检查项，便于后续改动回归时参考。

历史复核清单：

1. 两方块四向摆放，检查正反面贴图、选框与碰撞、与墙面和地板的衔接。
2. 打开 AI 终端及广播 HUD，检查 NOVA 待机、说话、扫描、重启和警告时的眼睛与光效。
3. 在常用 GUI Scale（2/3/4）、中英文下关闭重开两个界面，确认头像裁切与文字正常。
4. 在明暗环境下检查材质辨识度；观察终端小屏幕眨眼是否自然。
5. 旧存档中的现有方块应直接显示新外观，原菜单和星图缩放、拖拽、选择、导航照常可用。
