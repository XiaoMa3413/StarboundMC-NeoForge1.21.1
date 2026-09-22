# UI 同步、服务器动作与托管数据

仅当 UI 要改变服务端状态、采用 LDLib2 服务端模板，或任务明确涉及 syncdata/RPC 时读取。本项目
普通 Screen 由客户端构树，优先沿用项目已有 Menu/Packet，而不是为了一个按钮迁移整套架构。

## 先选通信路径

| 场景 | 建议 |
| --- | --- |
| 普通 `StarboundModularScreen`，服务端已有 Menu/Packet | 继续使用项目包；Root 回调只发送请求 |
| LDLib2 `BlockUIMenuType` 等双方共享 UIElement 树 | 可用 `addServerEventListener`、SyncValue、RPCEvent |
| BE 已实现 LDLib2 managed storage | 用 `@Persisted/@DescSynced` 管理字段，并在 UI 观察结果 |
| 独立跨页面命令且项目决定采用 LDLib2 RPC | `@RPCPacket` / `RPCPacketDistributor`，先核对类型 accessor |

不要混搭两套网络链路完成同一动作。选择前先查开屏方式和服务端是否真的拥有对应 UIElement。

## 项目普通页面的安全链路

```text
客户端 Button -> 请求 Packet/Menu action -> 服务端校验 -> 修改权威状态
              <- 服务端同步结果/失败原因 <-
```

服务端至少复核：

- 玩家仍打开正确 Menu，目标方块/实体仍存在；
- 玩家位置、维度、权限和冷却；
- 目标 id/索引属于当前服务器数据，而非客户端伪造；
- 名称长度、字符、数值范围和资源消耗；
- 动作当前是否可执行。

`button.setActive(false)` 只是客户端可用性提示，不是安全验证。失败时向客户端返回可理解的状态，
不要只让按钮“没有反应”。

## LDLib2 服务端 UI 事件

在 `BlockUIMenuType`/`HeldItemUIMenuType`/`PlayerUIMenuType` 等服务器托管 UI 中：

- `addServerEventListener(type, handler)` 把对应事件经 UISyncManager 发送到服务端；
- `Button.setOnServerClick(handler)` 是 MOUSE_DOWN 服务端监听的便捷入口；
- `addSyncValue`/`addRPCEvent` 由同一个 UISyncManager 分配 id 并经 ModularUI 包同步；
- Menu 必须提供正确的 `IUISyncManagerHolder` 上下文。

客户端单独 `new Button()` 并不意味着服务端也存在相同元素。若页面不是托管 UI，
`setOnServerClick` 不是项目 Packet 的替代品。

## managed BE 字段

BE 已选择 LDLib2 managed storage 时，最小形式为：

```java
public class MyBlockEntity extends BlockEntity
        implements ISyncPersistRPCBlockEntity {
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    private int progress;
}
```

- `@Persisted`：随 BE 数据保存；
- `@DescSynced`：服务端变化同步客户端；
- `@LazyManaged`：由代码显式 markDirty，避免不必要轮询；
- `@UpdateListener`：远端收到字段更新后的回调；
- `@RequireRerender`：同步后请求渲染更新；
- `@RPCMethod`：托管对象方法 RPC。

字段类型必须存在 direct accessor。自定义类型先查 `syncdata/AccessorRegistries.java` 和同版本测试，
不要看到 Codec 就假定能自动同步。

## 通用 LDLib2 RPC

`@RPCPacket("id")` 标注静态处理方法；参数都需要 direct accessor。发送入口位于
`RPCPacketDistributor`。使用前确认：

- id 在整个运行环境唯一；
- 方法运行侧判断正确；
- 服务端处理器不信任客户端参数；
- 不重复实现项目已有 payload；
- API 与 2.2.36.a 源码一致。

## UI 刷新

网络数据到达客户端后，更新已有元素的 text/class/active/display。列表使用稳定 key 做差异更新，
不要每个包都清空并重建所有行。若包可能乱序或页面已关闭，先验证 Menu/session 和 Root 生命周期。

## 源码入口

- `gui/sync/`：SyncValue、UISyncManager、UI RPC；
- `syncdata/`：managed storage、注解与 accessor；
- `networking/`：RPC packet 与 distributor；
- `test/TestBlockEntity.java`：托管 BE 示例；
- `gui/ui/elements/Button.java`：`setOnServerClick` 实际事件类型。
