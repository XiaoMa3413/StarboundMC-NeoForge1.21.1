# StarboundMC Forge 1.20.1 开发护栏

> 适用项目：StarboundMC `0.1-alpha`
> 技术栈：Minecraft `1.20.1`、Forge `47.4.10`、Java `17`、Mojang official mappings

本文不是 Forge API 百科，而是本项目最容易发生兼容、存档、线程和渲染问题的开发约定。
需要查询完整 API 时，以 [Forge 1.20.x 官方文档](https://docs.minecraftforge.net/en/1.20.x/) 和当前
Forge 源码为准。

## 1. 修改前先确认

| 检查项 | 本项目的真值来源 |
| --- | --- |
| Minecraft / Forge / Java | `gradle.properties`、`build.gradle` |
| modId | `gradle.properties`、`StarboundMC.MODID`、`mods.toml` |
| 注册对象 | `ModBlocks`、`ModItems`、`ModBlockEntities`、`ModMenus`、`ModEntities`、`ModSounds` |
| 网络协议与包顺序 | `network/ModNetwork.java` |
| 当前系统行为 | `docs/GAMEPLAY.md` 与测试 |
| 历史方案原因 | `docs/*-plan.md` |

当前关键值：

- `mod_id=starboundmc`
- `loader_version_range=[47,)`
- `forge_version_range=[47,)`
- `minecraft_version_range=[1.20.1,1.21)`
- `mapping_channel=official`
- `PROTOCOL_VERSION="4"`

不要从旧教程复制 1.12、1.16 或其他加载器的 API。先在本仓库或 Forge 1.20.1 源码中搜索同类
实现，再决定方法签名与事件总线。

## 2. 注册与加载生命周期

### 2.1 DeferredRegister

静态注册对象使用 `DeferredRegister` 与 `RegistryObject`，并在模组构造器把每个 register 绑定到
mod event bus：

```java
public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, StarboundMC.MODID);

public static final RegistryObject<Block> DEVICE = BLOCKS.register(
        "device", () -> new Block(BlockBehaviour.Properties.of()));

public static void register(IEventBus modBus)
{
    BLOCKS.register(modBus);
}
```

一个可手持方块通常需要两个注册：Block 和同名 BlockItem。方块实体、实体和菜单注册的是
`BlockEntityType`、`EntityType` 和 `MenuType`，不是运行时实例。

### 2.2 两条事件总线

| 总线 | 典型用途 | 本项目示例 |
| --- | --- | --- |
| Mod bus | 注册、客户端 setup、datagen、按键、维度特效注册 | `ModScreenRegistrar`、`ModKeyBindings` |
| Forge bus | 玩家、世界、tick、运行时渲染和服务器生命周期 | `SpawnHandler`、`ShipWarpEvents`、`PlanetRenderer` |

`@Mod.EventBusSubscriber` 应明确写 `modid`、`bus`，客户端订阅器还要写 `value = Dist.CLIENT`。
注解自动注册的方法必须是 `static`。

### 2.3 并行生命周期

`FMLCommonSetupEvent`、`FMLClientSetupEvent` 等可能并行分发。需要主线程或注册后状态的操作放进
`event.enqueueWork`。本项目的菜单屏幕和实体渲染器注册都集中在 `ModScreenRegistrar`：

```java
@SubscribeEvent
public static void onClientSetup(FMLClientSetupEvent event)
{
    event.enqueueWork(() ->
            MenuScreens.register(ModMenus.UPGRADE_MENU.get(), UpgradeScreen::new));
}
```

## 3. 客户端与服务端隔离

### 3.1 物理端隔离

common 包不能在类加载阶段引用 `net.minecraft.client`。客户端代码放在 `client/`，并通过以下方式
之一只在客户端加载：

- `@Mod.EventBusSubscriber(..., value = Dist.CLIENT)`
- 客户端 setup 注册
- 数据包 handler 中的 `DistExecutor`

`@OnlyIn` 不是模组端隔离工具，不应替代上述机制。

### 3.2 逻辑端隔离

方块交互、tick、物品消耗、存档和传送以服务端为准：

```java
if (level.isClientSide)
    return;

// 服务端修改状态
```

客户端可做预测、动画和输入收集，但不能决定燃料、目标是否可达、容器内容或实际掉落。

### 3.3 专用服务器

`runClient` 能运行不代表专用服务器安全。涉及以下内容时至少运行一次 `runServer`：

- common 类新增 import。
- 网络包或事件订阅器改动。
- 注册、维度、Codec 或 SavedData 改动。
- 客户端类被 common 类直接引用。

## 4. 线程与网络

### 4.1 SimpleChannel 规则

本项目在 `ModNetwork.register()` 中按自增 ID 注册消息。顺序是协议的一部分：

- 只能在列表末尾追加包，除非同时升级协议并接受不兼容。
- 改变字段编码、解码或语义时评估是否升级 `PROTOCOL_VERSION`。
- encoder 与 decoder 的字段顺序、整数编码方式和字符串长度必须完全一致。

### 4.2 Handler 模板

网络 handler 运行在线程池中，操作世界和玩家前必须排入主线程：

```java
public static void handle(MyPacket msg, Supplier<NetworkEvent.Context> supplier)
{
    NetworkEvent.Context context = supplier.get();
    context.enqueueWork(() ->
    {
        ServerPlayer player = context.getSender();
        if (player == null)
            return;
        // 在服务端重新验证 msg
    });
    context.setPacketHandled(true);
}
```

### 4.3 不信任客户端

C→S 包只能表达请求，服务端必须重新验证：

- 玩家是否仍在正确维度。
- 玩家是否打开了正确菜单。
- 目标 ID 是否存在且可达。
- 方块位置是否在合理距离且区块已经加载。
- 资源、燃料或物品是否真的足够。

不要直接对客户端提供的任意 `BlockPos` 调用会加载区块的方法。需要访问方块前先判断
`level.hasChunkAt(pos)`，再校验距离和方块类型。

### 4.4 客户端快照

`SyncFlightPacket` 使用版本 revision、服务器 tick、飞行阶段、分区坐标、局部坐标、速度和姿态。
连接到新服务器时必须重置客户端 revision，否则新的 revision 0 可能被旧会话的较大值拒绝。

## 5. 方块、方块状态与方块实体

### 5.1 方块状态

1.20.1 使用 `createBlockStateDefinition`，状态对象不可变：

```java
public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

public DeviceBlock(Properties properties)
{
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
}

@Override
protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
{
    builder.add(FACING);
}
```

调用 `state.setValue(...)` 会返回新状态，必须使用返回值。每增加一个属性都会增加启动时生成的
状态组合数；纹理或材质完全不同的对象通常更适合拆成不同方块。

### 5.2 BlockEntity 持久化

```java
@Override
protected void saveAdditional(CompoundTag tag)
{
    super.saveAdditional(tag);
    tag.putInt("Value", value);
}

@Override
public void load(CompoundTag tag)
{
    super.load(tag);
    value = tag.getInt("Value");
}
```

修改持久数据后调用 `setChanged()`。物品栏应通过 `Container`、`SimpleContainer` 或合适的
ItemHandler API 修改，不要直接替换底层数组后忘记标脏。

需要把方块实体状态同步给客户端时，按用途选择：

- 菜单 Slot / DataSlot：玩家正在打开的容器数据。
- `getUpdatePacket` / `getUpdateTag`：区块加载和方块外观状态。
- 自定义包：高频、跨维度或结构化状态。

不要把完整容器库存放进每 tick 更新包。

### 5.3 世界生成放置方块实体

`ShipStructure` 在 `ProtoChunk` 中放置船体。不能假设 `setBlockState` 会像玩家放置方块一样完成
所有 BlockEntity 生命周期；需要 tick 或立即持久化的设备应显式创建方块实体，并通过重建飞船
维度实测。

自动气闸由上下两个独立方块组成，只有下半格执行距离判断，避免重复切换和重复声音。

## 6. 菜单与屏幕

### 6.1 服务端菜单是真值源

- 物品槽与 shift-click 逻辑在 Menu。
- Screen 只绘制、发送操作请求和显示同步状态。
- `stillValid` 必须校验方块仍存在且玩家在交互距离内。
- 菜单关闭时，临时槽中的物品必须归还玩家或安全掉落。

客户端构造菜单时没有真实世界方块实体，应使用大小一致的空容器，等待 Slot 同步。

### 6.2 DataSlot 限制

`DataSlot` / `ContainerData` 的网络传输只有 16 bit 有效范围。大于 `32767` 或小于 `-32768` 的
数值需要拆成高低位或使用自定义包。飞行坐标和长时间计数不应塞入 DataSlot。

### 6.3 响应式与高密度 UI

星图把三类坐标分开：

1. 设计用基础画布坐标。
2. 响应式 viewport 投影后的逻辑 GUI 坐标。
3. `StarmapUiDensity` 计算的物理像素坐标。

交互 hitbox 必须与逻辑坐标一致；2× 绘制只提高栅格密度，不能悄悄改变点击区域。星体纹理、
选择环、悬停环和动态覆盖层必须共享同一个像素中心投影，避免低分辨率下半像素错位。

## 7. SavedData、NBT 与兼容迁移

### 7.1 SavedData

跨维度共享状态挂在主世界：

```java
server.overworld().getDataStorage()
        .computeIfAbsent(MyData::load, MyData::new, "name");
```

任何写操作后都要 `setDirty()`。StarboundMC 当前把飞船共享状态和传送器注册表放在主世界；
飞船模板放置标记则放在飞船维度，以便删除该维度时一起重置。

### 7.2 演进存档格式

新增字段时必须为旧存档提供合理默认值：

- 用 `tag.contains(key, type)` 区分“缺失”和合法的 0。
- 枚举同时保存稳定名称，读取未知值时回退到安全状态。
- 新坐标格式迁移期间保留旧字段读取，必要时也继续写旧字段。
- 对 NaN、Infinity、越界 sector 和非规范局部坐标做校验与归一化。

`ShipStateData` 就是本项目的参考实现：旧 `ShipX/Y/Z` 会迁移到 sector/local，新格式同时保留
旧绝对字段，飞行曲线不匹配时以当前确定性曲线修复位置。

### 7.3 注册名迁移

删除或重命名已经写入存档的方块/物品会损坏旧内容。优先保持注册名不变；确需替换时，在 Forge
总线监听 `MissingMappingsEvent` 并显式 `remap`。统一传送器的旧 ID 迁移可作为模板。

不要依赖 Java 字段声明顺序作为兼容策略，真正稳定的是资源命名空间和注册名。

## 8. 维度、世界生成与 Codec

### 8.1 数据与代码的边界

- 飞船维度通过 `RegistrySetBuilder` datagen 生成到 `src/generated/resources`。
- 熔岩、冰冻、荒芜维度的 JSON 位于 `src/main/resources/data/starboundmc/`。
- 自定义 `ChunkGenerator` 和 `BiomeSource` 必须提供与 JSON 字段匹配的 Codec。
- `ModWorldgen` 注册生成器和生物群系源类型，JSON 再引用对应资源位置。

改 Codec 字段、类型 ID 或 dimension JSON 后，旧世界可能无法加载。先复制测试存档，再重建对应
维度验证，不能直接拿唯一存档试错。

### 8.2 runData

```powershell
.\gradlew.bat runData
```

运行后检查 `src/generated/resources` 的真实差异。不要因为 datagen 运行成功就默认 JSON 正确；还要
启动客户端读取世界，并确认维度 type、generator、biome source 与注册 Codec 一致。

### 8.3 区块性能

世界生成循环中避免：

- 为每个方块创建不必要集合或字符串。
- 触发邻居更新和实体行为。
- 在区块加载阶段制造悬空重力方块。
- 访问当前区块之外的未加载世界状态。

荒芜生成器把无支撑水替换为空气而不是沙子，就是为了避免区块加载后产生大规模落沙更新。

## 9. 客户端渲染

### 9.1 RenderLevelStageEvent 坐标

本项目在实际版本中确认：事件提供的 PoseStack 含相机旋转，但不能假设它已经完成世界坐标到
相机原点的平移。绘制世界对象前显式使用：

```text
cameraRelative = worldPosition - cameraPosition
```

当前本地视图中 `+Z` 指向相机前方。将全屏或天空效果放到负 Z 可能落到近平面后方而不可见。

### 9.2 GL 状态必须成对恢复

天空、additive 辉光和透明几何常修改 blend、depth test、depth mask、cull 和 shader。每条渲染
路径结束前恢复调用者预期状态；提前 return 前同样要恢复。优先用 `pushPose/popPose` 包围局部变换。

### 9.3 缓存与分配

- 静态星图画布允许缓存，但窗口、viewport、密度或视觉版本变化时必须使缓存失效。
- 不要每帧上传全屏纹理。
- 高频空间解析复用数组和结果对象，避免每颗恒星每帧分配集合。
- 大坐标永远先转换为小的相对 double，再转 GPU float。
- LOD 过渡保持总亮度近似恒定，避免层级切换闪烁。

### 9.4 客户端资源预载

跃迁开始后会异步预载目标行星贴图。新增可达天体时同步检查预载、伴星贴图、资源位置和缺失贴图
回退，避免到达阶段第一次上传 4K 纹理造成明显卡顿。

## 10. 物品、掉落与工具等级

物质枪不走原版左键工具流程。激光完成时需要：

1. 服务端再次确认目标方块和距离。
2. 在破坏前保存 BlockEntity 引用。
3. 以物质枪 ItemStack 调用掉落逻辑，使时运生效。
4. 按 `requiresCorrectToolForDrops` 与当前挖掘等级决定是否掉落。

若直接使用 `Level.destroyBlock(pos, true)`，原版可能以 `ItemStack.EMPTY` 计算掉落，导致自定义时运
和工具等级失效。

升级 NBT 修改后要同步真实附魔列表；不能反复 `enchant()` 叠加同一条目。本项目通过重建附魔
列表修复旧的重复时运数据。

## 11. 国际化与资源

- 语言文件位于 `assets/starboundmc/lang/zh_cn.json` 和 `en_us.json`。
- common/服务端代码发送 `Component.translatable`，不要调用客户端 `I18n.get`。
- 翻译键只用于显示，逻辑判断使用注册名、枚举或稳定 ID。
- 资源路径全部小写，Linux 专服区分大小写。
- 修改模型前确认 blockstate、block model、item model 与纹理路径形成完整引用链。

Java 编译编码固定为 UTF-8。不要使用可能写入 BOM 或改变全文件换行的脚本批量覆盖 Java 源码；
编辑后以 `git diff` 检查是否出现无关的整文件变化。

`tools/*.ps1` 是开发辅助脚本，不属于运行时。部分脚本仍含旧机器绝对路径，执行前必须检查并改为
当前工作区；生成资产前先备份已经通过实机验收的贴图。

## 12. 验证与故障定位

### 12.1 命令层级

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
git diff --check
```

| 风险 | 验证 |
| --- | --- |
| 纯算法 | 对应 JUnit + `test` |
| 注册、资源或 Codec | `build` + 启动游戏 |
| 网络协议 | 编解码测试 + 登录/重连/服务端重启 |
| 飞行 | 各路线 + 中途退出重进 + 跨星系 |
| 船体 | 重建飞船维度 + 门和容器持久化 |
| UI | GUI Scale 1–4/Auto、宽屏和紧凑抽屉 |
| common/client 边界 | `runServer` |

### 12.2 常见症状

| 症状 | 优先检查 |
| --- | --- |
| 方块存在但 UI 打不开 | BlockEntity 是否存在、MenuProvider、客户端 screen 注册 |
| 容器关闭后物品复原 | 是否通过 Container API 修改、`setChanged()`、`broadcastChanges()` |
| 旧世界方块变成别的对象 | 注册名删除/重排、MissingMappings、是否应重建测试维度 |
| 客户端正常而专服崩溃 | common 类是否引用 `net.minecraft.client` |
| 网络包到达但状态不变 | handler 是否 `enqueueWork`、是否验证失败、是否 `setPacketHandled` |
| 天体或激光不显示 | 相机相对坐标、Z 正负、深度/混合状态、近平面 |
| 星图环与天体错位 | 基础坐标、viewport 投影和高密度像素中心是否共用同一转换 |
| 重进跃迁后停住或瞬移 | SavedData 飞行字段、服务器 init 恢复、SyncFlight revision |
| 新维度无法打开世界 | Codec 字段、JSON 类型 ID、旧维度数据兼容 |

## 13. 提交前检查清单

- [ ] `gradle.properties`、`StarboundMC.MODID` 与 `mods.toml` 一致。
- [ ] 新注册对象已经绑定 mod event bus，并有需要的 BlockItem/模型/战利品表/翻译。
- [ ] common 代码没有加载客户端类。
- [ ] 运行时事件和 mod 生命周期事件使用正确总线。
- [ ] 网络 handler 使用 `enqueueWork`、服务端重验输入并 `setPacketHandled(true)`。
- [ ] BlockEntity 调用 `super.load/saveAdditional`，数据改变后 `setChanged()`。
- [ ] 世界生成放置的设备已验证 BlockEntity 创建与 tick。
- [ ] SavedData 变更有旧字段默认值或迁移路径。
- [ ] 网络字段/顺序变化已评估协议升级。
- [ ] 世界坐标渲染已转相机相对坐标，并恢复 GL 状态。
- [ ] UI 绘制坐标与点击坐标使用同一投影。
- [ ] `test` 或 `build` 按风险通过，`git diff --check` 无错误。
- [ ] `.gradle-home/`、`run/`、解包素材和临时截图未进入提交。

## 14. 参考链接

- [Forge 1.20.x Documentation](https://docs.minecraftforge.net/en/1.20.x/)
- [Forge Networking](https://docs.minecraftforge.net/en/1.20.x/networking/)
- [Forge Registries](https://docs.minecraftforge.net/en/1.20.x/concepts/registries/)
- [Forge Sides](https://docs.minecraftforge.net/en/1.20.x/concepts/sides/)
- [Forge Block Entities](https://docs.minecraftforge.net/en/1.20.x/blockentities/)
- [Forge Data Generation](https://docs.minecraftforge.net/en/1.20.x/datagen/)

若官方文档、当前 Forge 源码和本手册冲突，以本项目实际依赖的 Forge 47.4.10 源码及可复现测试
为准，并在修复后同步更新本文。
