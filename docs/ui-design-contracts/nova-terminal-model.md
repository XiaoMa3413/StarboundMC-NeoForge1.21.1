# N.O.V.A. 实体通讯终端

SCREEN / PURPOSE
舰船壁挂 AI 终端；让玩家在世界中认出 N.O.V.A. 并进入现有通讯界面。

INTERACTION FANTASY / SCREEN MODE
与舰载智能面对面通讯。实体设备上的 Feature 视觉；现有 LDLib2 对话界面不在本次改造范围内。

PRIMARY VISUAL / INFORMATION PRIORITY
1. 大面积深蓝屏幕中的青色球体、斜轨道环和独立眼睛。
2. 下方简洁的通讯接入区域与 N.O.V.A. 铭牌。
3. 钛灰切角护框、石墨机身、散热槽和墙面安装座。

INTERACTION FLOW
接近 → 看见动态形象 → 右键 → 原有对话与剧情反馈 → 关闭返回世界。

SPATIAL ORGANIZATION
单方块壁挂设备，默认朝北、背面靠南侧墙。屏幕占据上部主体；短操作下颚稍向前伸，结构倒角与侧面散热增加厚度。四个朝向保持相同使用逻辑。

ART / CUSTOM VISUALS
通过 Blockbench MCP 建立可编辑网格、材质和 UV。沿用 nova_body / nova_eyes 的身份与注册位置，源图保留分层；实体屏幕合成为原版纹理动画。钛灰、石墨、少量青色，独立屏幕自发光。

GENERIC UI
继续使用现有对话菜单、剧情数据和交互。

MOTION / FEEDBACK / STATES
环境待机动画：约 1 像素悬浮、短促闭眼、轻微视线移动、低亮投影环。源身体与眼睛分开；动画不声称反映剧情或说话状态。现有打开后的对话状态表现沿用原实现。

DO NOT
大幅缩放、剧烈呼吸、泛滥辉光、密集粒子、装饰性仪表盘、传统机器人头部。

IMPLEMENTATION CONTRACT
保持方块 ID、壁挂位置、四向旋转、右键菜单和独立 GUI 肖像资源。实现可决定网格拓扑、原版动画帧序列、OBJ 包装和与外壳相配的碰撞近似。
