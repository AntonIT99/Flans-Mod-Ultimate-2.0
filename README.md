# Flan's Mod Ultimate 2.0 - Minecraft 1.20.1

### Attachment Config File Encoding / 配件配置文件编码

**English:**

- Maintained GB2312 encoding support for Chinese content packs
- File reading now supports: UTF-8 → GBK → GB2312 → ISO\_8859\_1 fallback chain

**中文:**

- 保持对中文内容包的GB2312编码支持
- 文件读取现在支持：UTF-8 → GBK → GB2312 → ISO\_8859\_1 回退链

***

### Armor Rendering Fixes / 护甲渲染修复

**English:**

- Fixed Z-fighting issues between player skin and armor models
- Fixed armor flickering on certain body parts (arms, legs)
- Resolved rendering order conflicts in inventory player preview
- Added proper RenderType handling for custom armor layers

**中文:**

- 修复了玩家皮肤与护甲模型之间的Z-fighting问题
- 修复了特定身体部位（手臂、腿部）的护甲闪烁问题
- 解决了背包界面玩家预览中的渲染顺序冲突
- 为自定义护甲层添加了正确的RenderType处理

***

### Ammo Box Mechanism / 弹药箱机制

**English:**

- Fixed ammo box only giving 1 clip instead of configured amount when ammo stack size is 1
- Now correctly gives `NumClips` amount as configured in grenades section
- Works independently from virtual ammo supply system

**中文:**

- 修复了弹药堆叠为1时，弹药箱只给1个弹夹而非配置数量的问题
- 现在正确给予grenades部分配置的 `NumClips` 数量
- 独立于虚拟弹药补给系统工作

***

### Shotgun Shooting Fix / 霰弹枪射击修复

**English:**

- Fixed shotgun pellet spread calculation
- Corrected pellet count and spread pattern
- Each pellet now correctly deals individual damage
- Fixed pellet trajectory and hit detection

**中文:**

- 修复了霰弹枪弹丸扩散计算
- 修正了弹丸数量和扩散模式
- 每个弹丸现在正确造成独立伤害
- 修复了弹丸轨迹和命中检测

***

### Ammo Display System / 弹药显示系统

**English:**

- Implemented digital ammo display on HUD
- Added current ammo / max ammo indicator
- Added ammo type icon display
- Fixed ammo count not updating correctly
- Added reload indicator when ammo is low
- Supports both numeric and visual ammo bar display
- Added fire mode indicator display

**中文:**

- 在HUD上实现了数字弹药显示
- 添加了当前弹药/最大弹药指示器
- 添加了弹药类型图标显示
- 修复了弹药计数不正确更新的问题
- 添加了低弹药时的换弹提示
- 支持数字和可视化弹药条显示
- 添加了射击模式指示器显示

***

### AA Gun System / 防空炮系统

**English:**

- **Ammo Consumption**: Fixed incorrect ammo consumption logic
  - Preserved `roundsPerItem` handling for multi-round ammo items
  - Uses `ShootableItem.getRoundsRemaining()` instead of `damageValue`

**中文:**

- **弹药消耗**：修复了错误的弹药消耗逻辑
  - 保留了多轮弹药物品的 `roundsPerItem` 处理
  - 使用 `ShootableItem.getRoundsRemaining()` 而非 `damageValue`

***

### Content Manager Updates / 内容管理器更新

**English:**
- Preserved GBK/GB2312 encoding support for Chinese content packs

**中文:**
- 保留了对中文内容包的GBK/GB2312编码支持

***

### File Structure Differences / 文件结构差异

#### Current Project Exclusive Files (Digital Ammo System) / 当前项目独有文件（数字弹药系统）

| File Path / 文件路径                                    | Description / 说明                        |
| --------------------------------------------------- | --------------------------------------- |
| `client/digitalammo/LocalBulletManager.java`        | Client local ammo manager / 客户端本地弹药管理   |
| `common/digitalammo/DigitalAmmoCommand.java`        | Digital ammo command / 数字弹药命令           |
| `common/digitalammo/DigitalAmmoHelper.java`         | Digital ammo helper / 数字弹药辅助类           |
| `common/digitalammo/DigitalAmmoStorageHandler.java` | Digital ammo storage handler / 数字弹药存储处理 |
| `common/digitalammo/DigitalAmmoSupplyHandler.java`  | Digital ammo supply handler / 数字弹药补给处理  |
| `common/digitalammo/PlayerBulletStorage.java`       | Player bullet storage / 玩家弹药存储          |
| `network/client/PacketSyncDigitalAmmo.java`         | Digital ammo sync packet / 数字弹药同步包      |

***

### GunType.java Key Differences / GunType.java关键差异

| Difference / 差异点                             | newversion                                                      | Current Project / 当前项目                                                                        |
| -------------------------------------------- | --------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| **Default Fire Mode / 默认射击模式**               | `submode = new EnumFireMode[]{EnumFireMode.SEMIAUTO}`           | `submode = new EnumFireMode[]{EnumFireMode.FULLAUTO}`                                         |
| **Fire Mode Reading / 射击模式读取**               | Uses `readFireModes(file)` method / 使用 `readFireModes(file)` 方法 | Reads directly in `read()` / 在 `read()` 中直接读取                                                 |
| **Digital Ammo System / 数字弹药系统**             | ❌ None / 无                                                      | ✅ Has `consumeBulletType` and `bulletsPerReload` / 有 `consumeBulletType` 和 `bulletsPerReload` |
| **getFireMode() Default / getFireMode()默认值** | `setFireMode(stack, mode)`                                      | `EnumFireMode defaultMode = (submode != null && submode.length > 0) ? submode[0] : mode`      |

**Current Project New Fields (Line 127-136) / 当前项目新增字段（第127-136行）：**
```java
@Getter
protected int consumeBulletType = 1;  // Which ammo type to consume (1-7) / 消耗哪种弹药类型(1-7)
@Getter
protected double bulletsPerReload = 30.0;  // Ammo per reload / 每次换弹弹药量
```

---

### Digital Ammo System Details / 数字弹药系统详解

**English:**

The Digital Ammo System is a virtual ammunition management system that allows players to store and use ammunition without physical ammo items. This system provides a more streamlined gameplay experience, especially for servers and adventure maps.

**中文：**

数字弹药系统是一个虚拟弹药管理系统，允许玩家存储和使用弹药而无需实体弹药物品。该系统提供了更流畅的游戏体验，特别适合服务器和冒险地图。

#### Core Components / 核心组件

| Class / 类 | Description / 说明 |
|-----------|-------------------|
| `PlayerBulletStorage` | Server-side player ammo storage (NBT persistence) / 服务端玩家弹药存储（NBT持久化） |
| `LocalBulletManager` | Client-side ammo cache for HUD display / 客户端弹药缓存用于HUD显示 |
| `DigitalAmmoHelper` | Helper methods for ammo operations / 弹药操作辅助方法 |
| `DigitalAmmoStorageHandler` | Save/Load ammo on player join/leave / 玩家加入/离开时保存/加载弹药 |
| `DigitalAmmoSupplyHandler` | Supply ammo from configured blocks / 从配置方块补给弹药 |
| `DigitalAmmoCommand` | Admin commands for ammo management / 管理员弹药管理命令 |
| `PacketSyncDigitalAmmo` | Network sync between server and client / 服务端与客户端网络同步 |

#### Ammo Types / 弹药类型

- Supports **7 ammo types** by default (configurable: 1-7) / 默认支持**7种弹药类型**（可配置：1-7）
- Each type can store up to **1000 rounds** (configurable) / 每种类型最多存储**1000发**（可配置）
- Default starting amount: **100 rounds** per type / 默认起始数量：每种类型**100发**

#### Gun Configuration / 枪械配置

Add these fields to your gun config files / 在枪械配置文件中添加这些字段：

```
ConsumeBulletType 1    // Which ammo type to use (1-7) / 使用哪种弹药类型(1-7)
BulletsPerReload 30.0  // Ammo consumed per reload / 每次换弹消耗的弹药量
```

#### Supply Blocks / 补给方块

Players can refill ammo by right-clicking configured supply blocks / 玩家可以通过右键点击配置的补给方块来补充弹药：

- Configured in `digitalAmmoSupplyBlocks` in config / 在配置文件的 `digitalAmmoSupplyBlocks` 中配置
- Each click adds `digitalAmmoSupplyAmount` to all types / 每次点击为所有类型添加 `digitalAmmoSupplyAmount`

#### Admin Commands / 管理员命令

```bash
# Set player's ammo / 设置玩家弹药
/digitalammo set <player> <type(1-7)> <amount>

# Add ammo to player / 给玩家添加弹药
/digitalammo add <player> <type(1-7)> <amount>

# Fill ammo (all or specific type) / 填充弹药（全部或指定类型）
/digitalammo fill <player> <type|all>

# Get player's ammo info / 获取玩家弹药信息
/digitalammo get <player>
```

#### Config Options / 配置选项

| Config Key / 配置键 | Default / 默认值 | Description / 说明 |
|--------------------|-----------------|-------------------|
| `enableDigitalAmmoSystem` | false | Enable/disable the system / 启用/禁用系统 |
| `digitalAmmoNumTypes` | 7 | Number of ammo types / 弹药类型数量 |
| `digitalAmmoDefaultAmount` | 100 | Starting ammo per type / 每种类型起始弹药 |
| `digitalAmmoMaxAmount` | 1000 | Max ammo per type / 每种类型最大弹药 |
| `digitalAmmoSupplyAmount` | 100 | Ammo added per supply / 每次补给添加的弹药 |
| `digitalAmmoSupplyBlocks` | [] | List of supply block IDs / 补给方块ID列表 |

#### How It Works / 工作原理

1. **Reload / 换弹**: When player reloads, system checks `ConsumeBulletType` and `BulletsPerReload` / 玩家换弹时，系统检查 `ConsumeBulletType` 和 `BulletsPerReload`
2. **Virtual Ammo / 虚拟弹药**: Ammo is deducted from player's digital storage / 弹药从玩家的数字存储中扣除
3. **Sync / 同步**: Changes are synced to client via `PacketSyncDigitalAmmo` / 更改通过 `PacketSyncDigitalAmmo` 同步到客户端
4. **Persistence / 持久化**: Ammo data saved to player NBT on leave, loaded on join / 弹药数据在离开时保存到玩家NBT，加入时加载

***

### ContentManager.java Key Differences / ContentManager.java关键差异

| Difference / 差异点               | newversion                                     | Current Project / 当前项目             |
| ------------------------------ | ---------------------------------------------- | ---------------------------------- |
| **Encoding Support / 编码支持**    | UTF-8 → ISO-8859-1 only / 仅 UTF-8 → ISO-8859-1 | UTF-8 → GBK → GB2312 → ISO-8859-1  |
| **Charset Import / Charset导入** | None / 无                                       | `import java.nio.charset.Charset;` |

**Current Project Encoding Logic (Line 527-550) / 当前项目编码处理逻辑：**

```java
private static List<String> readAllLinesUtf8OrLatin1(Path file) throws IOException {
    try {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (Exception e) {
        try {
            return Files.readAllLines(file, Charset.forName("GBK"));
        } catch (Exception e2) {
            try {
                return Files.readAllLines(file, Charset.forName("GB2312"));
            } catch (Exception e3) {
                return Files.readAllLines(file, StandardCharsets.ISO_8859_1);
            }
        }
    }
}
```

**newversion Encoding Logic (Simpler) / newversion编码处理（更简洁）：**

```java
private static List<String> readAllLinesUtf8OrLatin1(Path file) throws IOException {
    try {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (MalformedInputException e) {
        return Files.readAllLines(file, StandardCharsets.ISO_8859_1);
    }
}
```

***

### Summary Table / 差异总结表

| Category / 类别                    | Difference / 差异内容                                            | Impact / 影响程度                                        |
| -------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------- |
| **Digital Ammo System / 数字弹药系统** | Current project has complete system / 当前项目有完整系统              | 🔴 Major feature difference / 重大功能差异                 |
| **Encoding Support / 编码支持**      | Current project supports GBK/GB2312 / 当前项目支持GBK/GB2312       | 🟡 Medium (required for Chinese packs) / 中等（中文内容包必需） |
| **Default Fire Mode / 默认射击模式**   | newversion: SEMIAUTO, Current: FULLAUTO                      | 🟢 Minor (behavior difference) / 极小（行为差异）            |
| **Fire Mode Reading / 射击模式读取**   | newversion refactored to separate method / newversion重构为独立方法 | 🟡 Medium (code structure) / 中等（代码结构差异）              |

***
