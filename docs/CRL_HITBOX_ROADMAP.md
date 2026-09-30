# CRL Hitbox 功能与碰撞性能交接路线图

更新日期：2026-09-30，北京时间。面向接手开发、测试和验收本项目的人员。

本项目的交付目标是在 Minecraft 1.12.2、Cleanroom 和 Java 25 上独立实现 HitboxAPI 描述的碰撞功能，提供便于附属 Mod 和其他 Mod 调用的开放 API、可变碰撞体和事件驱动形态更新，并在本机使六项碰撞吞吐量分别不低于 README 原表数值。没有死线。当前已有几何库和 Entity 缓存基础，但不能据此宣布功能、性能或真实游戏验收完成。第 13 节将新增 API、行为、测试和实现顺序细化到方法。

这份文件是可随源码交接的工作路线图，不依赖原开发团队的会话、工具或人员分工。它汇总已确认目标、当前事实、后续工作和验收办法。本次只完成调查和文档整理；未恢复生产代码开发，未运行构建、基准或游戏实例，也未创建 commit 或发布版本。新接手人员开始实现前，应取得相应开发范围和冻结代码例外的明确授权。

## 1 用户已确认的交付目标

> 目标就是达到参考仓库内描述的碰撞性能

> 在你的目标机器测 CHB，六项均达到 README 原表 ops/s

> 接口对外开放，便于只做附属和让其他mod使用，碰撞箱可变，可由事件驱动形态。我需要你将路线图精确到方法

据此，本轮完整目标包含功能、开放接入、可变/事件驱动形态和性能；只达到其中一部分仍是未完成。中等、最高增强档尚未另行定义，不能自行添加，更不能把已要求的性能移到可选增强。使用平台及本机验收已明确；本文给出具体 API 设计提案，基准输入和统计口径仍须在第一阶段固定，见第 8、13 节。

本项目采用自己的 `dev.crlhitbox` API。功能对齐不自动承诺 HitboxAPI 包名、方法签名、二进制兼容、网络格式或内部对象布局兼容。若接手时发现实际消费者需要这些兼容能力，应作为范围变更确认。

## 2 参考项目与参考范围

| 来源 | 在本项目中的用途 | 使用边界 |
| --- | --- | --- |
| [AnECanSaiTin/HitboxAPI](https://github.com/AnECanSaiTin/HitboxAPI) | 用户指定的功能与六项吞吐量目标 | 依据 README 定义可观察结果；不直接移植源代码、算法、网络协议或许可证 |
| [CleanroomModTemplate](https://github.com/CleanroomMC/CleanroomModTemplate) | 已有构建脚手架的来源 | 本地只读模板位于 `D:/WI - Dev Workspace/CleanroomModTemplate`；当前项目构建文件和技术约束决定实际基线，不跟随上游自动升级 |
| [OpenJDK JMH](https://github.com/openjdk/jmh) | 性能测量方法与工具 | 测试依赖与生产几何隔离；不是碰撞算法参考或性能保证 |

功能及数值来源固定为 [HitboxAPI README 快照](https://github.com/AnECanSaiTin/HitboxAPI/blob/820a0a31fe279a26e317fa88f585a497686db085/README.md)。2026-09-30 读取并确认其文本与当时 `master/README.md` 相同。该 README 最近变更提交为 `820a0a31fe279a26e317fa88f585a497686db085`；读取文本的 UTF-8 SHA-256 为 `06DE5049A5EA1FFF6F6106C498DE34753B211D65FD4FB451CEE489CA84ED8193`。这不是参考 Mod 的产物哈希。

同日查询公开仓库树，返回提交 `56317f77d75945a9deaac6f7053dd228f26603a3`，结果未截断。没有找到专门的 benchmark/JMH 测试源目录或对应基准类文件名；[该提交的 build.gradle](https://github.com/AnECanSaiTin/HitboxAPI/blob/56317f77d75945a9deaac6f7053dd228f26603a3/build.gradle) 声明 JMH 1.37 依赖，但 JMH Gradle 插件处于注释状态。不能仅凭依赖存在声称原基准已经可复现。此次未逐个读取其生产 Java 文件，也未运行参考 Mod。

在用户补充开放 API、可变形态需求后，又只读检查了同一提交的公开 `IRay`、`IComposite`、`IOBB`、`ICapsule`、`ICollider` 接口声明：[接口目录](https://github.com/AnECanSaiTin/HitboxAPI/tree/56317f77d75945a9deaac6f7053dd228f26603a3/src/main/java/cn/anecansaitin/hitboxapi/api/common/collider)。Ray 暴露长度、起点、终点、方向；组合提供增删替换；OBB/Capsule 提供形态 setter。这里只用接口说明功能，不复制实现，也不把其中的碰撞回调或战斗语义自动带入 CHB。

UG capsule 是另外一个消费者驱动的实验分支，不是上述六项性能目标的基线。W108 是本项目自己的精确旋转重建契约，也不是 HitboxAPI 功能或性能的替代证据。

## 3 当前仓库可接手的基础

### 3.1 源码身份与工作树

本次实际核对的主目录为 `D:/WI - Dev Workspace/CrlHitbox-src`，分支 `main`，HEAD：

```text
0f167a36011e9cd0b02edf202c251d7a79569d92
feat(platform): add local holder contents replacement
```

前置提交包括 `1fc4e87` 的精确旋转重建、`4ba9a49` 的 Entity capability、`8f1754b` 的刚体放置查询。它们说明源码演进，不代表本次重新完成了测试。

整理前已有修改：`AGENTS.md`；已有未跟踪内容：`.codex/`、`docs/UG_CHB_CAPSULE_SCOPE.md`、`exhibit/`。须保留这些内容，不以清理工作树作为接手步骤。本文是本次新增文件。`docs/input.md` 是本地排除文件，不会随普通 clone 到达接手人，也不得强制加入版本库；本路线图不以它作为必需阅读入口。

### 3.2 已有能力与仍缺少的证据

| 能力 | 当前可核对事实 | 尚不能声称的结果 |
| --- | --- | --- |
| 基本几何 | 存在 `Aabb`、`Sphere`、`Obb`、`Capsule`、`Composite`、`Segment3d` 及查询实现和测试 | 未在本轮复跑测试；没有六项 JMH 达标记录 |
| 固体相交 | 已有四类基本固体的 typed 查询及 `Solid3d` 通用分派 | API 存在不等于目标性能达标 |
| 嵌套组合 | `Composite` 构造器接收嵌套组合，按顺序展开为固体叶子 | 不保留可编辑层级树，不接纳 Ray 或 Segment 作为固体叶子 |
| 刚体放置 | `RigidTransform3d` 和 `PlacedSolid3d` 已实现，支持调用者提供的坐标变换 | 没有选定 Minecraft Entity 原点、朝向和渲染插值规则 |
| Entity 缓存 | 已有非持久化 capability、ordered holder、不可变 snapshot 和原子 `replaceContents` | 没有联网镜像、自动同步或 F3+B 显示；真实容器生命周期仍需验收 |
| 精确重建 | W108 实现及专项验证文档存在 | 不代表网络解码、包资源限制或联网验收完成 |
| Ray | 现有 `Segment3d` 是有限闭线段，端点按字典序规范化 | 不能把它标为具有原点与方向语义的 Ray |
| 可变外部 API / 事件形态更新 | 当前几何值不可变，外部只能自行构造新值并替换 holder 内容 | 尚无本文拟定的可变类型、统一 Collider API 或显式更新事件 |
| 渲染 | 主目录生产源码没有客户端渲染模块 | 没有自定义碰撞体 F3+B 功能和真实 GPU 通过证据 |
| 网络 | 主目录生产源码没有完整同步模块 | 历史 Phase 2B 仍未完成 |

历史记录报告主线曾完成 35 suites / 320 tests 且失败、错误、跳过均为零；本次没有重跑，不能当作新候选的验收。现存历史验证文档和源码应作为接手后的复验入口，而非免测理由。

### 3.3 已有方法的复用清单

| 现有实现 | 后续具体复用 | 不重复建设的内容 |
| --- | --- | --- |
| `Aabb`、`Sphere`、`Obb`、`Capsule` 构造器 | 可变 facade 在有效更新时产生新值；复用输入域与边界语义 | 不重写一套不同精度或不同接触语义的基本形状 |
| `GeometryIntersections.intersects(...)` | solid-solid、segment-solid、placed-solid 的实际窄相 | 不把已有矩阵重新排成“尚未实现”任务 |
| `GeometryDistances`、内部 `BoxSat` 等 | 在现有公开或批准的内部边界复用算法 | 不为新增 facade 复制整套数学内核 |
| `Composite(List<? extends Solid3d>)` | 固体叶子的既有平铺组合与查询 | 新 mutable 组合负责编辑与 Ray，不重写原 Composite 合同 |
| `RigidTransform3d.andThen/transformPoint/transformVector` | Entity、组合和 renderer 的共同变换次序 | 不新增另一套 quaternion/矩阵约定 |
| `PlacedSolid3d` 与 placed query | 非 identity 放置后的固体查询 | 不把所有形状降为 AABB 或另写粗略窄相 |
| `EntityHitboxHolder` 的 `put/remove/clear/replaceContents/snapshot` | 现有 solid 缓存继续可用，事件更新 solid 时可直接调用这些入口 | 不重新实现已提交的 `replaceContents`，不推翻旧 API |
| `EntityHitboxes.find/require`、provider/bootstrap/attachment | 复用 capability 生命周期和平台接入点 | 不建立另一套全局 Entity/World 注册框架 |
| W108 `Rotation3d.reconstructExact` | 只有精确重建/未来 codec 需要时使用 | 不重新设计已完成的 W108，不把它插入普通每帧查询 |
| wrapper、Java 25、JUnit、`compileGeometryIsolation`、现有回归 | 扩展相应测试与隔离源范围 | 不重新初始化项目、不复制参考仓库构建配置 |

第 13 节新增快照/可变外壳是对现有几何的封装，不是替换内核。新通用 Entity 缓存仅为旧 `PlacedSolid3d` 类型不能容纳的 Ray、mixed Compound 和 enabled 状态提供兼容的增量入口；实现前应核对是否能进一步缩小这层，不能把旧 solid 缓存搬家并宣称原功能需要从零重做。

### 3.4 技术不变量

- Minecraft `1.12.2`；MCP `stable / 39-1.12`；最低编译与 API 基线 Cleanroom `0.6.8-alpha`；Java 25；Mod ID `crlhitbox`；版本 `0.1.0-SNAPSHOT`。保留现有 wrapper、插件、仓库、manifest 和 loader 配置；相关变更另行明确授权。
- 几何生产代码只依赖 JDK。`compileGeometryIsolation` 使用空外部 classpath，并继续是 `check` 的依赖。JMH、Minecraft、Netty、渲染和平台类型不得进入纯几何生产边界。
- 几何采用有限 `double`、闭集接触算相交、明确退化形状、精确值相等。不得为性能加入全局 epsilon、静默降低精度或伪造结果；double 查询不提供连续不穿透或认证几何保证。
- `Solid3d` 保持 sealed：`Aabb`、`Sphere`、`Obb`、`Capsule`、`Composite`。`Segment3d` 和 `PlacedSolid3d` 不是 solid。Ray 方案必须保持这一类型边界，除非另有批准。
- 21 个历史几何生产文件仍处于冻结边界；W108 与 UG 的例外是分别限定的。新增 Ray、性能优化涉及冻结文件时，应先提出具体文件与语义变更清单，不把“性能目标”视为自动解除冻结。
- holder 只保存实体局部的值，不持有 Entity/World；可变访问属于所属逻辑游戏线程；持久化保持关闭。几何查询不触发伤害、回调、网络、世界修改或普通路径日志。
- 客户端类隔离，专服不得因渲染代码发生类链接失败。实现优先采用最低 loader 的实际标准扩展点；Mixin、AT、coremod 不是预先授权的捷径。
- 项目许可证仍未选定；既有第三方声明保留。生成可交接本地产物不等于获得公开发布、改许可证、打 tag 或 push 的授权。

## 4 功能目标与验收矩阵

以下为从参考 README 转成 CHB 可观察行为的候选验收细化。已确认的功能集合不变；README 未说明的语义应在阶段 R0 确认。

| ID | 必需功能 | CHB 状态与开发内容 | 通过条件 |
| --- | --- | --- | --- |
| F01 | AABB、OBB、Sphere、Capsule 的碰撞查询 | 复用现有基本几何、旋转和分派 | 常见相交、不相交、接触、包含、有效退化及旋转用例结果正确；通用入口与 typed 入口一致 |
| F02 | 有长度的有向 Ray 查询 | 新增保留 origin/direction/length 的独立值与可变外壳；查询可复用有限 Segment 数学 | 对各基本固体、Ray、组合和放置形状可查询；方向与长度裁剪正确；不得排序后丢失公开 Ray 的方向信息 |
| F03 | 多形状组合及组合嵌套 | 保留原 solid Composite；新增可编辑 Collider 组合层 | 支持增删替换、Ray 子项和嵌套快照；多层变换正确；几何结果与对应平铺等价，不能只用包围盒重叠判真 |
| F04 | 统一易用的查询入口 | 保留 `GeometryIntersections` 和 typed 公共 API，补足 Ray 使用路径 | 外部小型消费者只依赖公开 API 即可完成构造、组合、查询；不要求照搬 `ColliderUtil` 名称 |
| F05 | Entity 上的非持久化碰撞体缓存 | 已有 holder/capability；补足支持类型、坐标和使用示例 | 实体附加、增改删、快照和销毁正确；新实体不继承旧缓存；重载不从存档恢复；Ray 能否附加须明确 |
| F06 | F3+B 显示缓存碰撞体 | 新增客户端调试渲染与 Entity 坐标适配 | F3+B 开启可见、关闭不可见；实际几何边界而非仅外包围盒；随实体移动/旋转正确；组合全部显示；不破坏原版调试显示和渲染状态 |
| F07 | Cleanroom 可实际使用 | 专服及真实 GPU 客户端独立实例 | 实际加载、capability 生命周期、示例查询和 F3+B 场景通过；安装产物与测试源码关联 |
| F08 | 六项碰撞吞吐量目标 | 尚未测量 | 第 5 节六项逐项达标，正确性与基准有效性同时通过 |
| F09 | 可由附属与其他 Mod 使用的公开 API | 新增稳定的 Collider 接口、形状类型和 Entity 入口 | 独立消费者只 import `dev.crlhitbox.api.*`；无需访问 internal、反射或复制实现；自定义提供者可输出受支持快照 |
| F10 | 可变碰撞体与事件驱动形态 | 新增明确 setter、复合编辑与显式事件入口 | 事件触发后查询与渲染看到新形态；无效更新不破坏旧状态；旧快照稳定；查询不隐式发事件 |

第 13 节将所有基本形状、有限 Ray 和组合定义为可查询对象，并明确新增部分是 CHB 的 API 提案，不是对参考实现算法的断言。现有固体矩阵不得退化。查询结果仍为碰撞与否，不自动扩展最近命中点、法线、接触流形、穿透深度或刚体物理响应。

## 5 六项性能合同与测量办法

### 5.1 必须保留的数值目标

来源为第 2 节固定 README。原环境为 AMD Ryzen 5 5600G、32GB、Microsoft OpenJDK 21.0，JMH 预热 5 轮、测量 5 轮；测试为同类碰撞体之间的碰撞判断。Rotated 项包含旋转后相关向量的重新计算。

| ID | 项目 | CHB 最低吞吐量 ops/s | 参考原表 Error | 当前 CHB 实测 |
| --- | --- | ---: | ---: | --- |
| P01 | AABB | 3,561,837,865.930 | 157,982,649.213 | NOT EXECUTED |
| P02 | Capsule | 22,350,681.198 | 238,993.658 | NOT EXECUTED |
| P03 | Rotated Capsule | 16,842,309.523 | 168,423.918 | NOT EXECUTED |
| P04 | OBB | 5,528,493.224 | 64,548.996 | NOT EXECUTED |
| P05 | Rotated OBB | 4,648,963.750 | 111,369.535 | NOT EXECUTED |
| P06 | Sphere | 119,556,589.705 | 171,114.656 | NOT EXECUTED |

六项是分别满足的门槛，不用平均值、总分、最佳一项或多线程总吞吐量抵消慢项。不把参考表 Error 从 Score 中扣除来降低要求，不改成只达到参考实现的某个百分比。Compound、Ray、混合类型与游戏负载应有补充测量，但参考 README 没有给它们相应绝对分数，不能虚构已有用户阈值。

### 5.2 目标机器

采用用户确认的“在本机测 CHB，六项都达到目标以上”口径，将“达到目标以上”落为逐项 `Score >= 原表 Score`，不采用误差放宽。本机于 2026-09-30 实测为：AMD Ryzen 9 5950X，16 核 / 32 逻辑处理器，系统可见内存 63.9 GiB，Windows Server 2025 Datacenter，版本 `10.0.26100`，64 位。

此主机是已确认的性能验收机；接手人如果无法使用这台机器，应先确认替代机器，不默默变更对标环境。本次已直接执行指定路径的 java/javac 版本查询，确认 Temurin `25.0.3+9-LTS`，见第 14 节。CPU 调度、功耗模式、频率/温度、后台任务、虚拟化情况、正式使用的 GC/JVM 参数和 JMH 版本仍须随每次报告记录；未采集的配置为 UNKNOWN，不推断默认值。

原表硬件和 JDK 与 CHB 不同：因此可以证明“CHB 在指定机器达到该绝对数值”，不能据此证明同硬件、同工作负载下优于参考算法。JDK 21 只描述参考环境，不改变 CHB Java 25 基线。

### 5.3 在实现优化前固定测试口径

接手人先查找可获得的原始 benchmark 工程和原始结果。若仍不可得，编写独立 CHB 基准，完整公开工作负载，并由需求方确认比较口径；六个目标值保持不变。不能声称新 harness 精确复现了未公开的原 harness。

建议第一版基准协议如下；线程数、时长、输入比例和统计规则属于待确认细化，必须在正式测量前冻结，不能看完结果再挑有利参数。

1. JMH Throughput / ops/s，单线程测量单次真实碰撞操作；预热 5 轮、测量 5 轮。建议至少 3 个独立 JVM fork；每轮时长、heap、GC 和全部参数写入可复现配置。
2. 每项固定形状规模、位置与朝向分布、重叠率、接触/分离数据。输入不能全部为可编译期折叠的常量，也不能全选远距离必不相交以只测早退。建议将相交、分离、接触分别报告并提供事先冻结的混合数据集。
3. 未旋转项测已经构造好的值之间的查询。Rotated 项每个操作必须包含与契约一致的旋转更新、相关向量重算及查询；不得把旋转放入 Trial setup 后仍标成 Rotated。CHB 不可变 API 引起的必要对象构造/分配要如实计入相应项目。
4. 返回或消费查询结果，检查死代码消除与常量折叠；审查内循环、批次操作数和 `OperationsPerInvocation`，防止把循环吞吐量或空操作算成一次碰撞。空基准只作诊断，不从结果任意扣除来制造达标。
5. 实测生产公开调用路径；typed 核、generic 分派、放置查询可分别报告，不用专为 benchmark 编写的捷径替代真实 API。生成随机数据与正确性 oracle 通常在计时段外，旋转成本按第 3 条处理。
6. 用独立正确性用例先验证所有基准输入和查询结果；基准快但结果错误为 NG。JMH 依赖与基准源码单独隔离，不进入发布 Mod 的生产运行依赖。
7. 保存每个 fork 的原始结果、汇总 Score/Error、实际操作定义、命令、退出状态和分配/GC 辅助证据。不得只保留最快 fork；剔除受干扰运行须说明可验证原因并完整重测。
8. 建议以预先冻结协议的最终汇总 Score 逐项与第 5.1 节比较，Error/置信区间同时展示。若需要保守置信区间下界也越过目标，须提前确认，不能临时新增或放松统计门槛。接近门槛且不稳定时补充复测，不挑一次峰值宣布通过。

[JMH 常量折叠示例](https://github.com/openjdk/jmh/blob/master/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_10_ConstantFold.java) 和 [循环测量示例](https://github.com/openjdk/jmh/blob/master/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_11_Loops.java) 可用于审查基准有效性。

P01 的数值特别高，必须优先核对一次 operation 的定义、JIT 生成路径和测量有效性。分数高本身不证明参考结果有误，也不允许直接删除该门槛。如果无法取得可信、可比的测量，应记录“可比性受阻”，向需求方提出证据和选项，不假称达标。

### 5.4 优化边界与失败处置

先测现有实现，按 profile 确认热点，再选择计算复用、分派、临时对象、边界处理等实际责任点。改动涉及冻结内核、数值运算顺序、公共 API 或构建约定时，先确定具体例外及回归要求。

保持有效输入域、接触语义、退化支持、不可变性、线程边界及纯几何隔离。不能以换成 float、缩小输入域、跳过检查、强制缓存陈旧结果或使用 mutable 全局 scratch 换取漂亮分数。可提出有依据的设计变更，但必须批准后实施并重新建立正确性证据。

每个失败项记录候选身份、目标、实测、偏差、热点证据、修改、正确性重测和性能重测。某项未达标时保留上个候选，不宣布整体性能 OK；必要时提出冻结例外或架构调整，由需求方决定，目标值不自动下调。

## 6 主干工作与明确延期项

本轮主干为：方法级契约与例外确认 → 可复现的六项性能基线 → 开放可变 Collider / Ray / 可编辑组合 → Entity 缓存与事件形态更新 → F3+B → 必要优化 → 独立附属接入及完整功能、性能、真实环境验收 → 保存交付物。性能基线尽早建立，避免全部功能完成后才发现数值目标不可达。

以下不因历史计划或参考仓库内存在相关类就自动进入本轮：

- 自动联网复制、delta/resync、周期广播和持久化。
- 命中/受击角色、伤害、战斗规则、骨骼动画、CrlOzzAPI 绑定。
- 连续碰撞、TOI、接触流形、穿透修复、角色移动与地形防穿透。
- UG capsule 新一轮调优、停放的 OBB 顾问方案、多平台迁移、公开发布。

联网范围需作一次明确选择：如果验收要求服务端创建/更新的缓存自动出现在远端客户端的 F3+B 中，那么全量同步成为该真实路径的必要依赖，应从延期项提升为主干；若各逻辑侧由调用者设置缓存，则客户端侧缓存的实际显示可以先独立完成。不能用客户端假数据冒充服务端数据已同步，也不能把没有联网说成不支持客户端查询。

## 7 分阶段推进路线

阶段编号仅属于本交接计划，不表示历史 Phase 2B 已获恢复。每一阶段均执行实现、测试、失败修复重测、保存候选；最终整体验收才允许标为 OK。

### R0 固定功能与性能验收契约

前置：获得接手开发范围授权；核对当前源码和受保护修改。

工作：确认第 8 节决策项；按第 3.3 节复用已有实现；完成第 14 节所需基础建设；列出形状/查询支持矩阵；冻结六项性能 harness 协议；明确 F3+B 的数据来源与坐标系。根据预期热点和 Ray 设计列出需要的冻结文件例外，保留不变内容。核对已有测试能否在 Java 25 上发现并执行，建立新的候选基线记录。

产出：在本文件补充已确认决策、源码身份、测试环境和下一片修改范围。退出条件：接手人能明确判断每个功能和每个性能项是否通过。原基准缺失只阻塞准确比较合同，不阻止已授权的源码调查和独立功能复验。

### R1 建立并运行六项生产路径基准

前置：R0 的测量口径已固定；新增 benchmark 依赖/构建路径已获批准。

工作：在与纯几何生产分离的测试工程或 source set 中建立六项基准及结果检查；测现有实现，保存全部原始输出。分开报告 non-rotated 与 rotated 的实际工作量，并验证计时结果可重复。

产出：六行当前成绩、达标差距、CPU/GC/分配热点、基准有效性检查。退出条件是可信基线，不要求此时所有分数已经达标。若某项没有实际执行或 harness 不可信，不能填零或 PASS。

### R2 完成几何功能缺口和独立消费者入口

前置：Ray 语义、Compound 范围及相关冻结例外确认。

工作：按第 13.1–13.5 节实现开放接口、各类 setter、不可变快照、有限 Ray、可编辑嵌套组合与统一查询；复验现有 solid 矩阵，保留 Segment 的既有语义。准备只使用公开 API 的外部小型消费者示例，证明无需接触内部实现。

验证：方向、起点位于形状内部、切触、零方向策略、平行、明显远距、普通尺度及有机制依据的大坐标边界；组合与平铺等价；旋转/刚体变换后的查询一致；几何隔离与 API 结构检查。最终具体用例随 R0 合同确定。

退出条件：F01–F04 对批准操作全部通过；Ray 未完成时不得借 Segment 宣布完成。此阶段不得顺手重做网络或战斗系统。

### R3 完成 Entity 缓存的实际使用路径

前置：支持附加的类型、entity-local 原点和局部轴定义清楚。

工作：按第 13.6–13.7 节补充所有 Collider 类型的 Entity 缓存和显式更新事件；复验 capability 注册、附加和访问。实现独立的 Entity→世界坐标适配边界，不让 holder 开始读取 Entity/World。实体逻辑 tick 的查询姿态与客户端 partial-tick 绘制姿态区分清楚，Ray 不进入 `Solid3d`。

验证：实际 Entity 的缓存新增、修改、删除、销毁、新建、非持久化；不同 Entity 隔离；局部→世界变换和 `a.andThen(b) = b ∘ a` 的次序；服务端类加载安全。

退出条件：F05 有真实专服生命周期证据和可运行消费者示例。若启用远端同步目标，接入第 10 节全量同步路线并完成其相关 gate。

### R4 实现 F3+B 调试显示

前置：R3 坐标契约稳定；真实 GPU 客户端实例可用。

工作：使用实际 Cleanroom 0.6.8-alpha 客户端扩展点；按原版 F3+B 开关显示 holder 中的几何。显示 OBB 方向、球面/胶囊轮廓和组合叶子，不能全部画成 AABB。Ray 绘制长度只是可视化裁剪，不得改变查询域。

验证：开关、普通移动、旋转、相机偏移、插值、零尺寸/半径、远离世界原点、实体移除、世界切换；绘制前后 GPU 状态正确；原版调试框保持可用；关闭后不继续进行无用复杂几何计算。用固定坐标与预置场景复现，保存截图/录像与日志。

退出条件：F06、客户端相关 F07 真机通过；软件渲染、源码检查或 headless 测试不能替代。

### R5 逐项消除性能差距

前置：R1 基线有效；功能与几何语义稳定。性能调查可以提前进行，最终分数必须绑定最终候选。

工作：按差距和热点逐项优化 P01–P06；每次保持相同冻结工作负载。先保护正确性，再核对吞吐量与分配变化。受影响的其他几何查询执行回归，不能把优化一个项目的错误转移到其他项目。

退出条件：六项全部达到第 5.1 节要求，基准方法和结果经独立复核。未达标就保存 NG/候选记录并继续修复；遇到真实授权或技术决策边界才标明 BLOCKED。

### R6 最终验收与交付保存

前置：必需功能、性能和缺陷修复收敛，冻结本次候选源码及配置。

执行第 9 节最终矩阵：构建与隔离、行为回归、六项性能、专服、真实 GPU 与外部消费者使用。若最终修改影响性能，重测相应项目；最终记录必须能证明六项结果对应同一可交付版本。

产出：第 11 节交付包。退出条件：功能与性能全部满足、必要人工确认完成、阻断缺陷为零、产物可取用。没有死线不等于无限扩张；完成本档后保留 OK 基线，新增能力另立范围。

## 8 接手前后必须明确的决策

| 决策 | 当前状态 | 未确认时的处理 |
| --- | --- | --- |
| 功能与六项原表绝对吞吐量 | 用户已确认 | 不降低、不替换为相对百分比 |
| 死线 | 用户明确无死线 | 不虚构完成日期；按阶段退出条件推进 |
| 目标机器 | 用户已明确本机；5950X 主机参数已采集 | 换机先确认；正式运行记录完整环境 |
| benchmark 输入/线程/fork/时长/统计 | 原 README 信息不全；第 5.3 节为建议 | 第一阶段冻结，不用有利数据集暗中替代 |
| Ray 域和结果 | 参考公开 API 有长度；本路线设计有限有向 Ray，布尔查询 | 第 13.3 节作为具体提案收口；不增加无限射线 |
| Compound 语义 | 现有固体 Composite 保持不变；新增可编辑 Collider 组合 | 第 13.4 节固定嵌套、Ray 子项、快照式所有权及变换 |
| 对外开放、可变形态、事件驱动 | 用户明确要求，属于主干 | 第 13 节逐方法落实；不能只开放静态读取或仅做内部示例 |
| Entity frame | 当前为调用者定义的抽象局部系 | 明确原点、轴、yaw/pitch、逻辑/渲染采样及 supported cache types |
| 服务端缓存远端可见 | README 未承诺同步细节 | 明确是否将全量同步提升为主干 |
| 冻结代码与构建例外 | 仍有限定保护 | 逐片批准准确例外；不以整个路线图解除所有冻结 |
| 最终发布/许可证 | 未确定 | 可保存本地候选；外部分发方式及许可另行确认 |

确认后应直接更新本表和对应验收项，保留确认日期/来源，不额外创建重复状态账本。

## 9 验证门槛和环境准备

| 门槛 | 必须证明 | 不能替代它的证据 |
| --- | --- | --- |
| Source / Static | 类型/API 边界、冻结例外、classfile 链接、依赖隔离 | 文件名或源码字符串匹配不证明运行行为 |
| Build / Unit / Integration | 真实逻辑正确、测试确实发现并执行、消费者集成可用、主产物成功生成 | 编译成功不能证明性能或游戏效果 |
| Performance | 同一候选的六项 JMH 分数达到原表；方法有效且可重跑 | 无来源估算、单次峰值、单元测试耗时、TPS/FPS |
| Dedicated Server | 最低 loader 下加载、capability 生命周期、需要时的网络和身份处理 | 单元测试或 IDE 中类可见 |
| Real GPU Client | 真实 GPU 路径、F3+B、坐标和渲染状态、需要时的远端同步 | 软件渲染、图片生成、源码/字节码检查 |

正式实例必须与开发 checkout、其他项目和有价值世界隔离。当前没有在本次交接中确认 CHB 专用的服务端目录、客户端目录、端口、启动/优雅停止方法或 EULA-ready 状态；这些保持 UNKNOWN，不猜测路径和端口，不擅自接受 EULA 或清世界。

服务端和客户端测试 mod 列表均须确认存在兼容的 Fugue 与 scalar；按实际文件/元数据核对，不能因大小写或版本文件名变化误判。记录 loader、Java、所有测试 mod、配置、世界和安装 CHB JAR 哈希。实际测试实例可在同一物理主机，但进程、目录、数据和证据独立。

构建使用仓库 wrapper 和 Java 25，Gradle 缓存默认 `C:/GradleCaches`。获得执行范围后，典型完整回归为：

```powershell
$env:GRADLE_USER_HOME = 'C:/GradleCaches'
java -version
.\gradlew.bat --version --console=plain
.\gradlew.bat clean compileGeometryIsolation test --stacktrace --console=plain
.\gradlew.bat build --stacktrace --console=plain
```

执行前确认实际 Java toolchain 与临时构建目录不承载待保留证据。历史 `jdk.net.unixdomain.tmpdir` 设置只是特定主机错误的进程级修复，不应写进生产客户端配置或无条件复制到新机器。JMH 任务尚未创建，因此本文不提供虚构可执行的 benchmark 命令。

所有必要测试的未执行、未发现、跳过、超时和环境失败均不可记 PASS。本次文档整理的 Dedicated Server、Performance、Real GPU Client 均为 NOT EXECUTED。

## 10 历史后续路线与风险隔离

### 10.1 全量联网同步

历史 Phase 2B 目标是 server-authoritative full snapshot；它不是当前已有能力。若真实使用合同需要远端自动显示，应单独收口协议，再实现以下切片：

1. 验证 `ResourceLocation` 无损编码。历史只读发现 `(ab, cd:ef)` 与 `(ab:cd, ef)` 可能得到同一字符串，但属于不同 ID；一字符 namespace 也有构造问题。须针对实际编译依赖复现，再决定编码/版本契约，不能通过偷偷禁止合法 ID 掩盖问题。
2. 版本化直接二进制 codec，顺序保存 entry，接入已审阅 W108 重建；禁止部分解码后发布和未检查长度分配。
3. 以 dimension、runtime entity ID、UUID、provider generation 和 server revision 区分目标及新旧状态；客户端本地 holder revision 与服务端 wire revision 分开。
4. StartTracking、显式发送及所需玩家生命周期触发；网络线程只传递已解码不可变消息，client-main-thread 执行查找与原子安装。
5. 有界 pending store、过期、身份拒绝、世界/断连清理；异常保留首因，不把失败包装为成功。
6. codec 独立 oracle、replica 独立参考模型、恶意输入资源限制、专服与真实客户端相关联验收。

历史候选协议为 channel `crlhitbox`、S2C discriminator `0`、version `1`；消息上限 1,048,576 bytes，最多 4,096 entries，单 ID 1,024 bytes，单 Composite 4,096 leaves，全消息 16,384 primitive leaves；pending 最多 256 messages / 16 MiB / 200 client ticks。这些是旧方案的约束，不是已实现功能或此次批准的新 wire 规范。修订 ResourceLocation 或扩展 Ray/缓存类型会影响协议，须在实现前统一确认，不静默复用版本号。

本地 `replaceContents` 已进入当前 HEAD，不应按旧任务材料重复实现。W108 也不应因开始联网而重新设计。delta、C2S resync、自动变更观察、持久化和动画绑定均另行立项。

### 10.2 UG capsule 和停放的 OBB 研究

独立目录 `D:/WI - Dev Workspace/CrlHitbox-ug-capsule-worktree` 的 HEAD 为 `b8a29fd`。本次只读核对发现它仍含未提交的 cast 生产源码、测试与文档。普通 checkout 或只交付该 commit 会丢失这些内容，不能视为主线已拥有 capsule cast。

其验证记录显示：334-test 历史构建与隔离记录存在，但总体验收未收口；thin-wall 分配量 35,536–36,752 B/cast 高于该专项 16 KiB 目标；若干退出分支与实际 refinement cap 仍为 NOT_DEMONSTRATED。它是固定方向 Capsule 平移对单静态 AABB 的专项候选，不等于 P02/P03 的静态 Capsule 同类碰撞性能。

此路线已关窗，OBB 顾问结论也处于停放状态。它们不阻塞 README 功能/性能主干，也不因本次交接而自动重新调优或合并。如以后接手这一专项，须另行取得完整 dirty 源码、合同、实际消费者需求和验收记录。

## 11 完成定义和最终交付物

只有同时满足以下条件才称本项目本档 OK：

1. R0 批准的完整功能矩阵全部实现；Ray、可编辑组合、可变碰撞体、事件驱动形态、公开附属接入、Entity 缓存和 F3+B 无未披露的缺口。
2. P01–P06 全部达到原表绝对吞吐量；生产路径正确、统计口径固定、原始结果可重跑。任何一项失败都不能用整体均值抵消。
3. 对应源码的必要构建、行为、隔离、专服和真实 GPU 验收通过；需要的人工确认已取得。不存在已知阻断缺陷。
4. 提供可从说明直接使用的公开 API 示例与安装方法；能力描述不夸大为物理系统、伤害系统或连续防穿透。
5. 交付包包含主 remapped Mod JAR、必要开发产物/源码、源码 commit 与必要 dirty 快照、依赖与配置清单、构建环境、测试及 JMH 原始证据、游戏验收记录、已知限制。主 JAR 记录大小和 SHA-256；dev JAR 与 runtime JAR 不混用。
6. 保存当前可交付版本；后续增强不能覆盖这个已通过版本。未全部满足时标记 WIP / CHECKPOINT / CANDIDATE，并逐项指出 NG 或 BLOCKED。

建议最终包内采用一份 manifest 关联所有产物与证据，不靠散落日志或聊天结论识别版本。不打包无关秘密、私人收件箱或其他项目内容。commit、tag、push、公开发布、部署和生产数据修改分别遵循用户授权；路线图不代替这些授权。

## 12 接手阅读顺序与第一步

先读本文件的目标、性能表和决策表，再按具体工作读取：

| 路径 | 需要了解的内容 |
| --- | --- |
| [README.md](../README.md) | 当前公开能力描述及非目标 |
| [GEOMETRY_SEMANTICS.md](GEOMETRY_SEMANTICS.md) | 几何、数值、组合、放置和查询契约 |
| [ENTITY_HOLDER_SEMANTICS.md](ENTITY_HOLDER_SEMANTICS.md) | 帧、revision、线程、生命周期和非持久化 |
| [EXACT_ROTATION_RECONSTRUCTION_DESIGN.md](EXACT_ROTATION_RECONSTRUCTION_DESIGN.md) | 涉及重建或 codec 时必读的 W108 契约 |
| [W108_RECONSTRUCTION_VERIFICATION.md](W108_RECONSTRUCTION_VERIFICATION.md) | W108 历史验证和证据边界 |
| [PHASE2B_REPRESENTATION_BLOCKER.md](PHASE2B_REPRESENTATION_BLOCKER.md) | 已经处置的重建历史问题，勿误认为当前全部阻塞 |
| [BASELINE.md](BASELINE.md) 和 [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) | 初始化来源、平台约定和第三方声明 |
| `src/main/java/dev/crlhitbox/api/geometry/`、`api/entity/`、`internal/entity/` 与对应测试 | 当前真实实现；用源码与实测核对文档 |

第一步交接动作是核对 HEAD、dirty 内容和本文件第 8 节，然后确定下一片的准确修改范围与性能测量口径。不要先移植参考仓库，也不要先启动历史网络、连续碰撞或战斗路线。接手报告只需说明本档进度、真实验证结果、六项性能差距、交付位置与下一项待处理事项。

## 13 方法级接口与实现清单

本节是根据用户新增需求形成的具体设计提案，供接手人逐方法实现和验收。标为“已有”的 API 已核对源码；其余签名均为“拟新增”，目前不可调用。签名块是接口清单，省略 import、实现体和普通 `equals/hashCode/toString`，不是已经编译通过的源文件。R0 应一次收口接口提案及必要的冻结例外，避免边写实现边改外部合同。

### 13.1 模块和状态模型

采用四个明确边界：

1. `api.geometry`：已有不可变几何内核，保持纯 JDK；冻结规则继续有效。
2. `api.collider`：拟新增的开放提供者接口、可变形状对象、不可变快照和统一查询，仍只依赖 JDK 与现有几何。新增隔离编译范围必须覆盖这一层。
3. `api.entity`、`api.event`：Entity 缓存、坐标适配入口和显式 Forge 更新事件；可引用 Minecraft/Forge，不进入纯几何 source set。
4. `internal.client`：F3+B 和渲染，禁止反向进入 common/server 的 classfile 引用。

可变对象服务于开发者更新形态；每次有效修改先完整校验，再发布新的不可变状态。查询读取快照，不在查询中调用用户事件、更新形态或读取世界。快照可跨线程传递；可变对象与 Entity 缓存只能在各自所属逻辑线程修改。

对外开放的意义是：其他 Mod 可以直接构造/更新对象、调用查询、接入 Entity、订阅事件，也可以实现 `Collider` 返回受支持快照。不会为了开放 API 解除 `Solid3d` 的 sealed 限制。任意新基本形状或自定义碰撞算法注册 SPI 不在本次范围；附属可以组合现有形状表达自身模型。

### 13.2 公共接口与不可变快照

拟新增到 `dev.crlhitbox.api.collider`：

```java
public interface Collider {
    long revision();
    boolean enabled();
    RigidTransform3d localToParent();
    ColliderSnapshot snapshot();
}

public interface MutableCollider extends Collider {
    boolean setEnabled(boolean enabled);
    boolean setLocalToParent(RigidTransform3d transform);
}

public sealed interface ColliderSnapshot
        permits SolidColliderSnapshot, RayColliderSnapshot, CompoundColliderSnapshot {
    boolean enabled();
    RigidTransform3d localToParent();
    Optional<Aabb> bounds();
}

public final class SolidColliderSnapshot implements ColliderSnapshot {
    public SolidColliderSnapshot(Solid3d solid,
            RigidTransform3d localToParent, boolean enabled);
    public Solid3d solid();
}

public final class RayColliderSnapshot implements ColliderSnapshot {
    public RayColliderSnapshot(Ray3d ray,
            RigidTransform3d localToParent, boolean enabled);
    public Ray3d ray();
}

public final class CompoundColliderSnapshot implements ColliderSnapshot {
    public CompoundColliderSnapshot(List<? extends ColliderSnapshot> children,
            RigidTransform3d localToParent, boolean enabled);
    public int childCount();
    public ColliderSnapshot child(int index);
}
```

三个 final 类均实现接口中的全部 accessor，构造时防御复制并预计算不可变状态；不通过 getter 暴露可变数组/列表。`bounds()` 表示在该快照 parent frame 下的保守边界；禁用或无有效叶子的组合返回 `Optional.empty()`，该含义是“空集合”，不是“无界”或“未知”。启用叶子必须具有可表示的有限 bounds，否则构造失败，不能以 empty 隐藏错误。

`localToParent` 是整个 collider 的额外放置；OBB/Capsule 自身的 orientation 是形状内部旋转。组合子项的 parent 是组合局部系，不能重复叠加 entity/world transform。快照值相等包括形态、enable、变换与有序子项，不含可变对象的 revision。

所有 `MutableCollider`：默认 enabled=true、localToParent=identity、revision=0；有效 setter 返回 true 并 checked-increment 一次，无效输入先抛出且原状态不变；值相同返回 false、不增加 revision；溢出拒绝更新。不按对象 identity 判断是否变化，不做模糊相等。`snapshot()` 在没有更新时复用已验证的不可变快照，不能每次查询重新分配整棵树。外部自定义 `Collider` 也必须遵守其返回快照的稳定性合同；查询核心只接收快照，不执行提供者代码。

方法验收：`setEnabled` 会影响查询和渲染；`setLocalToParent` 的 bounds 与窄相正确；快照不受后续 setter 影响；null、非有限输入、revision 溢出不改变状态；同值更新无额外分配和 revision 增长。

### 13.3 各形状的可变方法

以下类均为拟新增的 public final class，实现 `MutableCollider` 并继承其公共合同。构造器参数均为局部坐标，不隐含 Entity pose。

```java
public final class MutableAabbCollider implements MutableCollider {
    public MutableAabbCollider(Vec3d min, Vec3d max);
    public Vec3d min();
    public Vec3d max();
    public boolean setBounds(Vec3d min, Vec3d max);
}

public final class MutableSphereCollider implements MutableCollider {
    public MutableSphereCollider(Vec3d center, double radius);
    public Vec3d center();
    public double radius();
    public boolean setCenter(Vec3d center);
    public boolean setRadius(double radius);
    public boolean setShape(Vec3d center, double radius);
}

public final class MutableObbCollider implements MutableCollider {
    public MutableObbCollider(Vec3d center, Vec3d halfExtents, Rotation3d orientation);
    public Vec3d center();
    public Vec3d halfExtents();
    public Rotation3d orientation();
    public boolean setCenter(Vec3d center);
    public boolean setHalfExtents(Vec3d halfExtents);
    public boolean setOrientation(Rotation3d orientation);
    public boolean setShape(Vec3d center, Vec3d halfExtents, Rotation3d orientation);
}

public final class MutableCapsuleCollider implements MutableCollider {
    public MutableCapsuleCollider(Vec3d center, double centerlineLength,
            double radius, Rotation3d orientation);
    public Vec3d center();
    public double centerlineLength();
    public double radius();
    public Rotation3d orientation();
    public boolean setCenter(Vec3d center);
    public boolean setCenterlineLength(double length);
    public boolean setRadius(double radius);
    public boolean setOrientation(Rotation3d orientation);
    public boolean setShape(Vec3d center, double centerlineLength,
            double radius, Rotation3d orientation);
}

public final class Ray3d {
    public Ray3d(Vec3d origin, Vec3d direction, double length);
    public Vec3d origin();
    public Vec3d direction();
    public double length();
    public Vec3d end();
    public Aabb bounds();
    public Segment3d asSegment();
}

public final class MutableRayCollider implements MutableCollider {
    public MutableRayCollider(Vec3d origin, Vec3d direction, double length);
    public Vec3d origin();
    public Vec3d direction();
    public double length();
    public Vec3d end();
    public boolean setOrigin(Vec3d origin);
    public boolean setDirection(Vec3d direction);
    public boolean setLength(double length);
    public boolean setShape(Vec3d origin, Vec3d direction, double length);
}
```

逐方法约束：

| 方法组 | 必须实现的语义 | 必测项 |
| --- | --- | --- |
| `setBounds` | 一次发布 min/max；不提供可暂时产生 min>max 的分别赋值 | 任意轴反序拒绝；零宽合法；失败保留 bounds、revision、snapshot |
| Sphere `setCenter/setRadius/setShape` | 复用现有 Sphere 域；`setShape` 是一次有效修改 | 半径零、负值、NaN、中心变更后查询和渲染一致 |
| OBB `setHalfExtents/setOrientation/setShape` | 不把 shape orientation 与 localToParent 混为一谈；必要轴/角点随有效变更更新 | 90 度旋转、零 extent、交错 setter、bounds 失效检查 |
| Capsule `setCenterlineLength/setOrientation` | 长度是中心线长度，不含两端半球；默认轴为局部 +Y，端点为 center ± orientation.rotate(0,length/2,0) | 长度零退化球、半径零、旋转与平移、完整外长为 length+2r |
| Ray 构造和 `setDirection` | 保存有向单位方向；输入必须有限且非零，归一化采用稳定尺度运算；无方向默认值 | 极小/大有限方向、不依赖下溢平方、反向不误命中 |
| Ray `setLength/setShape/asSegment` | length 有限且 >=0；end=origin+direction*length，终点和 delta 不可表示时拒绝；零长为起点 | 前向长度之外不命中；`asSegment` 排序不改变 Ray 自身 origin/direction |
| 所有 `setShape` | 完整校验后原子发布，单次 revision；不以连续多个 setter 实现不安全中间状态 | 后一参数无效时整个操作无变化；旧 snapshot 保持稳定 |

Ray 采用有限域 `[0,length]`，不实现无限射线，不继承 `Solid3d`。它可复用现有 Segment 的集合查询；公开 Ray 方向和原点不能被端点规范化覆盖。`Ray3d` 放在新增 collider 层，避免仅为增加名称而改动被冻结的 Segment。

### 13.4 可编辑与嵌套组合

拟新增：

```java
public final class MutableCompoundCollider implements MutableCollider {
    public MutableCompoundCollider();
    public MutableCompoundCollider(List<? extends ColliderSnapshot> children);
    public int childCount();
    public ColliderSnapshot child(int index);
    public void addChild(ColliderSnapshot child);
    public boolean setChild(int index, ColliderSnapshot child);
    public ColliderSnapshot removeChild(int index);
    public boolean replaceChildren(List<? extends ColliderSnapshot> children);
    public boolean clearChildren();
}
```

子项是已经完成的不可变快照，支持固体、Ray 和组合，保持插入顺序。`addChild`/`removeChild` 为一次有效变更；`setChild` 相等时 no-op；`replaceChildren` 全量校验后一次发布；空组合合法，表示不与任何物体相交且不绘制。

这是明确的快照式所有权：把 `childCollider.snapshot()` 加入组合后，修改原 `childCollider` 不会暗中改变父组合；必须再 `setChild` 发布。新增接口不保存任意可变子图，因此不需要跨对象隐藏监听器，也不允许活引用形成环。多层树编辑用新子快照逐层替换，renderer/query 对深层输入使用迭代遍历，避免用户控制的递归栈溢出；接收路径对不可承受的规模明确失败，容量门槛在 R0 固定。

验收方法：`replaceChildrenRejectsInvalidInputAtomically`、`nestedSnapshotDoesNotAliasMutableChild`、`setChildInvalidatesBounds`、`rayLeafParticipatesInQueries`、`disabledChildDoesNotIntersect`、`nestedPlacementComposesOnce`。这些是计划中的测试名称，不是已有测试结果。

### 13.5 统一碰撞查询及内部方法边界

已有 `GeometryIntersections.intersects(...)` 的固体、Segment 和 PlacedSolid overload 保留。拟新增公共入口：

```java
public final class ColliderQueries {
    public static boolean intersects(ColliderSnapshot first, ColliderSnapshot second);
    public static boolean intersects(Ray3d ray, Solid3d solid);
    public static boolean intersects(Ray3d ray, PlacedSolid3d solid);
    public static boolean intersects(Ray3d first, Ray3d second);
}
```

第一方法是所有附属共用的 collider 入口，双方必须属于同一 parent frame。先处理 disabled/empty，再做保守 negative pruning，最后进入真实窄相。组合相交是有效叶子对的存在性判断。它不访问 Entity、持久化、网络、事件或渲染，也不把 broad-phase positive 当作窄相 positive。

计划中的内部职责和必须覆盖的路径如下；名字可在实现中局部调整，语义不可省略：

| 内部方法 | 输入和返回 | 职责与复用点 |
| --- | --- | --- |
| `ColliderQueryDispatcher.intersects(first, second)` | 两个 `ColliderSnapshot` → boolean | 共用分派；遍历组合累计 placement；不能调用用户 provider 或重新生成形态 |
| `intersectsSolidSolid(first, firstToParent, second, secondToParent)` | 两个 Solid3d 与变换 → boolean | identity 情况保留 typed 快路径；其他情况复用 PlacedSolid/PlacedQueries 的真实语义 |
| `intersectsRaySolid(ray, rayToParent, solid, solidToParent)` | Ray、solid 及变换 → boolean | 有限方向转成正确 parent-frame Segment，与固体窄相；不排序覆盖公开 Ray 状态 |
| `intersectsRayRay(first, firstToParent, second, secondToParent)` | 两个有限 Ray 及变换 → boolean | 新的闭线段相交行为验证；不能以容易下溢的 squaredDistance==0 直接代替可靠谓词 |
| `ColliderSnapshotFactory.build...(...)` | 已验证形态、enable、placement → snapshot | 更新路径构造不可变缓存；查询不依赖懒加载副作用 |

完整查询矩阵为 AABB、Sphere、OBB、Capsule、Ray 的 15 个无序基本配对，加任意两者对 Compound、Compound 对 Compound，均覆盖正反参数顺序。已有四类 solid 的 10 对继续复用并回归；Ray 引入的 5 对必须补齐。变换后的组合也在矩阵内。返回 boolean，不偷偷调用碰撞回调或自动施加伤害。

Ray-Ray 新谓词若需要改动旧几何，先取得例外；新增实现需覆盖重合、交叉、共线分离、平行、仅延长线相交、零长与数值尺度，不把“很近”当作相交。对所有组合路径，bounds 仅能排除，不能证实。

### 13.6 Entity 缓存与坐标入口

已有公开方法保持原语义：

```java
EntityHitboxes.find(Entity entity);
EntityHitboxes.require(Entity entity);
EntityHitboxHolder.put(ResourceLocation id, PlacedSolid3d localPlacement);
EntityHitboxHolder.replaceContents(EntityHitboxSnapshot snapshot);
EntityHitboxHolder.remove(ResourceLocation id);
EntityHitboxHolder.clear();
EntityHitboxHolder.snapshot();
```

旧 holder 只能保存 Solid placement，不能为了 Ray 直接改其返回类型。提议采用新增通用缓存入口，同时保留旧入口兼容：

```java
public final class EntityColliders {
    public static Optional<EntityColliderHolder> find(Entity entity);
    public static EntityColliderHolder require(Entity entity);
    public static boolean requestUpdate(Entity entity, ResourceLocation reason);
}

public final class EntityColliderHolder {
    public EntityColliderHolder();
    public long revision();
    public int size();
    public boolean isEmpty();
    public Optional<ColliderSnapshot> find(ResourceLocation id);
    public boolean put(ResourceLocation id, ColliderSnapshot localCollider);
    public boolean remove(ResourceLocation id);
    public boolean clear();
    public boolean replaceContents(EntityColliderSnapshot snapshot);
    public EntityColliderSnapshot snapshot();
}

public final class EntityColliderSnapshot {
    public static EntityColliderSnapshot of(long revision,
            Map<ResourceLocation, ? extends ColliderSnapshot> entries);
    public long revision();
    public int size();
    public boolean isEmpty();
    public ResourceLocation id(int index);
    public ColliderSnapshot collider(int index);
    public Optional<ColliderSnapshot> find(ResourceLocation id);
}

public final class EntityColliderFrames {
    public static RigidTransform3d translationOnly(Entity entity);
    public static ColliderSnapshot placeInWorld(ColliderSnapshot entityLocal,
            RigidTransform3d entityLocalToWorld);
}
```

新 `EntityColliderHolder` 存不可变 ColliderSnapshot，按现有 holder 的有效修改/revision/原子替换规则处理；没有 Entity/World 引用、持久化、事件回调或隐式网络。`of` 捕获 Map 的迭代顺序并防御复制，要求非负 revision；需要确定顺序的消费者应传有序 Map。`replaceContents` 不导入源 revision。`placeInWorld` 返回新的放置快照但不改缓存；查询和渲染共同使用相同变换合同。

默认 Entity 原点建议明确为 `posX/posY/posZ` 所在点，局部轴与世界轴平行；`translationOnly` 只平移，不自动使用 head/body yaw/pitch。实体造型的朝向由调用者提供的 collider transform 或显式 entityLocalToWorld 表达。这是具体建议，不声称所有实体具有同一头/身体原点。更复杂的朝向适配可由附属在外部事件中计算，无需先引入骨骼系统。

内部 `EntityHitboxProvider` 拟加一个独立通用 holder 字段及相应 capability 暴露；旧 holder 和旧 capability 不变。注册、附加、访问和专服 classloading 都需要重新验证。持久化仍为关闭。

兼容绘制策略固定为：F3+B 读取旧/新 holder 的快照，旧 `PlacedSolid3d` 转成临时 solid view；相同 `ResourceLocation` ID 以新通用缓存为准，避免重复绘制。两种缓存不暗中互相复制；外部查询需要哪一种就通过对应公开入口取得。迁移示例说明“读旧值→构造新快照→put→明确移除旧项”，不偷偷改写旧数据。

方法验收包括 absence 下 `find` 返回 empty、`require` 明确失败、null 拒绝、同值 no-op、插入顺序、原子替换、溢出、Ray/Compound 存取和实体销毁释放。缓存是快照发布模型：修改 standalone MutableCollider 后，必须显式 `put(id, mutable.snapshot())` 才改变 Entity 可见形态；不承诺隐式活绑定。

### 13.7 事件驱动形态更新

新增的 CHB 事件是显式更新入口，不是碰撞发生回调。拟新增 `dev.crlhitbox.api.event.EntityColliderUpdateEvent`，继承实际 Forge `Event`，不 cancellable：

```java
public final class EntityColliderUpdateEvent extends Event {
    public EntityColliderUpdateEvent(Entity entity, ResourceLocation reason,
            EntityColliderHolder holder, EntityColliderSnapshot before);
    public Entity getEntity();
    public ResourceLocation getReason();
    public EntityColliderHolder getHolder();
    public EntityColliderSnapshot getBefore();
}
```

`EntityColliders.requestUpdate(entity, reason)` 的方法合同：

1. 验证参数和所属逻辑游戏线程；取得已经附加的通用 holder，不补建 Entity 或静默挂 capability。
2. 捕获 before snapshot，在 `MinecraftForge.EVENT_BUS` 同步发布一次更新事件；具体 bus 与线程检测实现按最低 patched loader 核对。
3. 外部监听器根据姿态、装备、自有动画/状态等修改 MutableCollider，然后 `event.getHolder().put(id, collider.snapshot())` 发布；也可一次 `replaceContents` 批量替换。
4. 正常返回时，比较 holder 前后 revision，实际有修改返回 true，否则 false。不自动发网络包，不产生默认形态，不扫描全部实体，不从查询或渲染线程调用。
5. 同一实体递归 `requestUpdate` 明确拒绝；清理重入标记不得覆盖原始异常。标记不放进几何 ThreadLocal。
6. 监听器失败保留 cause 并向负责的事件/任务边界传播，不返回成功。单次 `put/replaceContents` 原子，但整个多监听器事件不是事务：先前已成功发布的修改不会伪装成回滚。需要批量原子性的监听器先构造完整 snapshot，再调用一次 `replaceContents`。

外部 Mod 可在自己的事件中直接调用 setter + put，无需 CHB 特有事件；也可在实际 Forge tick、姿态变化或装备事件中调用 `requestUpdate`。CHB 不擅自安装“每 tick 扫描所有实体”的默认驱动。没有自动网络同步时，更新仅对调用所在逻辑侧有效；远端显示要求按第 10.1 节另行接入。

计划测试方法：`requestUpdatePostsExactlyOnce`、`requestUpdateReturnsFalseForNoChange`、`eventDrivenResizeChangesIntersection`、`failedShapeValidationKeepsPreviousEntry`、`listenerFailurePreservesCause`、`recursiveUpdateIsRejected`、`queryDoesNotPostEvents`、`wrongThreadUpdateFailsClearly`。实际 Forge 总线集成和实际 Entity 生命周期不能只用 mock 验收。

### 13.8 F3+B 和内部渲染方法

以下为客户端内部计划方法，不承诺为公共扩展 API：

| 方法 | 合同 |
| --- | --- |
| `ColliderDebugRenderHandler.onRenderWorldLast(RenderWorldLastEvent event)` | 在核实可用的客户端标准事件中读取原版 debug bounding-box 开关；未开启立即退出；只读取快照 |
| `EntityColliderRenderFrames.interpolatedTranslation(Entity entity, float partialTicks)` | 仅用于显示，将上一/current 坐标按 partialTicks 插值；不改逻辑缓存或服务端姿态 |
| `ColliderDebugRenderer.render(ColliderSnapshot snapshot, RigidTransform3d parentToWorld, Vec3d camera)` | 累积局部变换与 camera-relative 坐标；disabled/empty 不画；迭代处理组合 |
| `drawAabb(...) / drawObb(...) / drawSphere(...) / drawCapsule(...) / drawRay(...)` | 使用相应实际轮廓；Ray 按真实有限长度；sphere/capsule 细分是渲染近似，不改变碰撞判定 |
| `collectVisibleSnapshots(Entity entity)` | 捕获并合并旧/新缓存视图，同 ID 新缓存优先；不触发事件、持久化、形态重算或网络 |

`RenderWorldLastEvent` 是待按实际依赖核对的接入方案，不以本表声称当前已有该 handler。渲染状态必须成对保存/恢复，异常也要释放，不能污染原版或其他 Mod 绘制。不为便于调试向 common 加客户端类型，不先加 Mixin。

### 13.9 六项 benchmark 方法和真实性检查

拟新增隔离的 `ColliderBenchmark`，命名与六项一一对应：

```java
@Benchmark public boolean aabbPair(CollisionState state);
@Benchmark public boolean capsulePair(CollisionState state);
@Benchmark public boolean rotatedCapsulePair(CollisionState state);
@Benchmark public boolean obbPair(CollisionState state);
@Benchmark public boolean rotatedObbPair(CollisionState state);
@Benchmark public boolean spherePair(CollisionState state);
```

最终验收必须覆盖本次对外提供的调用路径。建议上述六项使用 `ColliderQueries.intersects(a.snapshot(), b.snapshot())`；未变形的 snapshot 是缓存值，不能在基准中偷偷绕开生产入口。已有 `GeometryIntersections` typed overload 的单独微基准保留作热点诊断，不单凭底层分数证明新增 facade 达标。

`rotatedCapsulePair` 和 `rotatedObbPair` 必须在计时操作中执行实际 `setOrientation`、必要状态/向量更新、取得新快照和查询，使用事先固定的非同值 orientation 序列。旋转一个还是两个输入、姿态序列与输入比率在 R0 冻结；不能把 no-op setter 当成旋转工作。shape 更新带来的分配必须记录。准确对标口径仍受第 5.3 节原 harness 缺失限制。

`CollisionState` 提前建立具有独立预期结果的固定数据集；计时方法选择下一组输入并返回布尔结果。不能用恒定碰撞结果可推导的代码制造 P01 分数。基准有效性审查与功能正确性检查是性能 gate 的一部分。

结果校验器计划方法 `verifyThresholds(Path jmhJson, Path manifest)`：核对六个 benchmark ID 全部存在、单位为 ops/s、所需 fork/轮次与源码身份正确、Score 为有限正数，然后逐项按本文件阈值比较。缺失、重复而含糊、NaN、错误单位、运行失败都报具体错误，不能填零或忽略。它只是最终结果检查，不替代 JMH 测量或人工审查 benchmark 工作量。

### 13.10 接手实现顺序和变更清单

| 切片 | 方法/文件组 | 最小完成证据 |
| --- | --- | --- |
| A | `ColliderSnapshot` 三种值、factory、`Collider/MutableCollider` 合同 | 防御复制、变换、bounds、equals 和隔离编译；无 Minecraft/JMH 生产依赖 |
| B | AABB/Sphere/OBB/Capsule 的 constructor/accessor/setter/snapshot | 无效更新原子性、no-op、revision、旧快照稳定及现有几何回归 |
| C | `Ray3d`、`MutableRayCollider`、Ray 5 个基本配对 | 有限方向长度、退化/接触、Ray-Ray 数值正确性 |
| D | `MutableCompoundCollider` 全部编辑方法、统一 `ColliderQueries` | 15 对矩阵、嵌套、Ray 子项、snapshot ownership 和深层迭代遍历 |
| E | `EntityColliders`、新 holder/snapshot、provider 增量、frames | 实际 capability 附加、存取、非持久化、旧入口兼容和专服安全 |
| F | `requestUpdate`、`EntityColliderUpdateEvent` | 外部事件改形态后真实查询改变，错误不假成功、重入/线程检查 |
| G | F3+B 内部方法组 | 独立真实 GPU 场景、旧/新数据合并、开关与状态恢复 |
| H | 六个 JMH 方法、`verifyThresholds`、必要优化 | 本机六项均不低于原表，并绑定最终生产调用路径和源码 |
| I | 独立附属示例、使用文档、交付 manifest | 附属只依赖公开 API 编译运行，事件变形、查询和显示形成完整链 |

可在 A/B 起步时先运行旧几何基线；H 的测量贯穿开发，但最终不能仅沿用旧 API 的历史成绩。任何生产优化涉及旧内核时，另列准确的冻结例外；新增 public API 也需更新 classfile API-surface 测试为明确清单，不能直接删除闭包检查。

预计变更范围是新增 `src/main/java/dev/crlhitbox/api/collider/`、`api/event/`、相关 Entity API，以及平台 provider/bootstrap、客户端模块、对应测试和独立 benchmark 配置。源码实际写入尚未开始。本计划不预先授权修改全部这些目录，接手时按切片确定具体文件。

### 13.11 外部附属使用示例和验收

以下是拟新增 API 的示意代码，供接手者实现后作为独立示例 Mod 编译验证；现在不能直接编译调用：

```java
// 在所属逻辑游戏线程，附属收到自己的姿态/装备/状态变化事件。
MutableCapsuleCollider body = new MutableCapsuleCollider(
        new Vec3d(0.0, 0.9, 0.0), 1.2, 0.3, Rotation3d.identity());
body.setShape(new Vec3d(0.0, 0.6, 0.0), 0.6, 0.3, Rotation3d.identity());
EntityColliders.require(entity).put(bodyId, body.snapshot());

// 同一 world frame 查询；不修改缓存，也不产生伤害或事件。
ColliderSnapshot local = EntityColliders.require(entity).find(bodyId).orElseThrow();
ColliderSnapshot world = EntityColliderFrames.placeInWorld(
        local, EntityColliderFrames.translationOnly(entity));
boolean hit = ColliderQueries.intersects(world, obstacleInWorld);
```

示例中的 `entity`、`bodyId` 和 `obstacleInWorld` 由消费者真实提供，不生成假 Entity。实际长期使用时保留调用者自己的可变对象或由状态构造新值，不能每次查询重复创建整个示例模型。

事件订阅示例：

```java
@SubscribeEvent
public void updateShape(EntityColliderUpdateEvent event) {
    // 先按真实实体类型/能力筛选；读取附属自有状态，计算新快照。
    // buildBodySnapshot 是附属自己的方法，不是 CHB API。
    ColliderSnapshot next = buildBodySnapshot(event.getEntity());
    event.getHolder().put(bodyId, next);
}

// 附属在真实、已验证的逻辑事件入口显式发起，不在 render/query 中调用。
EntityColliders.requestUpdate(entity, poseChangedReason);
```

独立附属必须作为另一个消费工程编译，依赖 CHB 开发 artifact，验证没有 `dev.crlhitbox.internal` import、反射、复制源码或仅 test classpath 才可用的入口。运行时安装匹配的 remapped artifact，真实事件使形态从站立变蹲伏或从收拢变展开，查询结果与 F3+B 同步变化；关闭 F3+B 不影响逻辑查询。其性能仍由六项正式基准与补充事件/显示负载共同记录，不能以演示流畅替代吞吐量 gate。

## 14 本机基础盘点与补建任务

基础缺口属于本路线图的前置工作。此处区分“已验证存在”“未建立”和“尚未确认”，不把有目录、有设备或有依赖声明当成环境已通过。本次只做只读盘点和 JDK 版本查询，没有安装依赖、创建游戏实例或改变主机配置。

### 14.1 当前基础状态

| 基础项 | 本次核对结果 | 接手后的处理 |
| --- | --- | --- |
| 项目与源码 | `main` / `0f167a36011e9cd0b02edf202c251d7a79569d92`，已有实现见第 3 节 | 保留 dirty/untracked，按已存在代码增量推进 |
| Java 25 | `C:/GradleCaches/jdks/eclipse_adoptium-25-amd64-windows.2/bin/java.exe` 实测 Temurin `25.0.3+9-LTS`；同目录 javac 为 `25.0.3` | 复用此 JDK；正式构建与 JMH 固定相同发行版和版本 |
| PATH 默认 Java | `Get-Command` 指向 Oracle `javapath` shim，未把它当作指定 JDK 证据 | 执行脚本使用明确 JAVA_HOME/可执行路径，避免 shim 或其他 JDK 抢占 |
| Gradle 基础 | wrapper 脚本、JAR、properties 存在；已有 Java 25 构建、JUnit 与 isolation task | 接手复验工具链和任务；不重新搭建项目、不随意升级插件 |
| 当前本地产物 | runtime JAR 75,364 bytes，SHA-256 `188E804123F56583DA29E841280B8B30F705070311ED28844170A5B5C3F592ED`；dev/sources JAR 也存在 | 仅作现有候选识别，不是本轮功能/性能验收通过物 |
| CHB JMH 工程 | 当前 build/gradle/src 未发现 JMH/benchmark wiring；默认 Gradle cache 下 `org.openjdk.jmh` 目录不存在 | 新建隔离 benchmark 工程/源集，解析锁定依赖并验证任务；不能直接宣布“已有 JMH 环境” |
| 原参考 benchmark | 未找到公开的对应完整基准源文件，原参数不全 | 先补齐测量合同，必要时取得原 harness；不能复制数字当实测 |
| 硬件 | 5950X / 16C32T / 63.9 GiB / Windows Server 2025 | 此机为六项正式性能验收机，控制和记录运行干扰 |
| GPU / 显示 | 系统列出 NVIDIA Quadro K620，driver `32.0.15.8241`，以及 GameViewer Virtual Display Adapter | 只是设备枚举；必须验证 Minecraft 的实际 OpenGL renderer 使用物理 GPU，不能由虚拟显示设备名推定 GPU gate 通过 |
| CHB 专用测试实例 | 候选父目录存在且有多个其他项目实例；未确认 CHB 自有 server/client 子目录 | 建立隔离的 CHB 实例；不借用其他项目或有价值世界作为默认环境 |
| Fugue / scalar | 本次未核对拟建 CHB 实例的 mods 清单 | 两端准备兼容版本，核对 mod metadata、来源与 SHA-256，启动前确认均存在 |
| 公共 API 消费示例 | 尚未找到本计划要求的独立附属示例工程 | 新增独立消费者验收 fixture，不能只放在 main/test classpath 冒充外部接入 |
| 原始证据保存 | 尚无这份合同对应的 JMH JSON、GPU/专服记录和统一 manifest | 建立按候选源码与产物身份关联的本地证据目录；具体路径在接手范围中确定 |

### 14.2 基础建设顺序和退出条件

| 基础任务 | 具体工作 | 退出条件 | 阻塞的阶段 |
| --- | --- | --- | --- |
| B01 工具链复验 | 固定 JDK 25 路径和 Gradle cache；复验 wrapper/JUnit/isolation；记录实际类路径和测试发现数 | 干净构建候选可生成，旧回归与空外部类路径几何编译通过 | 生产实现回归和最终交付 |
| B02 JMH harness | 为 benchmark 单独配置 JMH core/annotation processor；优先固定并审查 1.37 作为与参考 build 声明一致的候选；创建六项基准及阈值检查 | 六项均被发现并运行、原始 JSON 可读、fork/轮次/单位验证通过；生产 Mod 无 JMH 依赖 | R1/R5/R6 性能部分 |
| B03 本机测量配置 | 记录 OS/CPU/JDK/GC/JVM 参数；排除同时运行的重负载构建/游戏对结果的干扰；进行稳定性试跑 | 同一数据集可重复，运行差异有解释；未改变目标数值 | 正式性能达标声明 |
| B04 独立测试实例 | 在明确授权目录建立 CHB 专用 server/client；选定独立端口、启动/停止和日志路径；准备最低 loader、Fugue、scalar 及 disposable 测试世界 | 空基线可启动、连接和优雅退出，基础依赖完整；EULA 与实例操作权限已明确 | R3/R4/R6 游戏验收 |
| B05 GPU 路径 | 用客户端实际日志/系统信息核对 vendor、renderer、driver；准备固定摄像机/坐标场景 | 物理 GPU 渲染与可读截图/录像路径成立，非软件 renderer | F3+B 真实 GPU gate |
| B06 消费者工程 | 用现有 Cleanroom 构建约定建立独立附属 fixture，通过 CHB dev artifact 编译，runtime 使用匹配 remapped artifacts | 只用公开 API 的创建、变形、事件、查询、显示链可运行 | F09/F10 和最终交付 |
| B07 证据与交付保存 | 指定输出目录，记录源码/dirty、依赖配置、JAR 哈希、原始 JMH、功能与实例记录 | 每份结果可追到同一候选；必要快照含真实参与构建的未跟踪源码，排除秘密 | OK 保存 |

基础任务按依赖推进：GPU 环境尚未建立不阻止纯 JDK 几何测试；原 JMH 参数缺失不阻止公开 API 设计；专服实例未获使用权限则只阻塞相关运行链。不能把基础准备“做完”当成功能或性能已经完成。

B02/B06 如需增加构建文件、依赖或 source set，先取得准确的构建约定例外；优先与发布生产路径隔离。正式命令应由实际新任务验证后写回文档，不在计划中伪造 `jmh` 或 example 任务已存在。

### 14.3 可落地的辅助入口

接手人可在授权的测试工具目录实现以下入口；它们是待建设内容，不是已安装命令：

- `Verify-ChbEnvironment.ps1`：读取 JDK、wrapper、CPU/内存/GPU、实际路径和配置，输出环境 JSON；发现缺项明确失败，不自动安装或改全局 PATH。
- `Run-ChbBenchmarks.ps1`：使用 B02 实际创建并验证的任务启动指定六项，要求显式源码/产物身份与输出目录，保留完整退出码和 JMH 原始文件；不接管其他 Java 进程。
- `Verify-ChbThresholds`：对应第 13.9 节 `verifyThresholds(Path, Path)`，逐项校验六个目标；失败项包含目标、实际、单位、候选和证据路径。
- `Export-ChbCandidate.ps1`：显式指定输出目录，保存主 JAR、必要源码状态、依赖配置和验收 manifest；不自动 commit/push，不清理工作树，不覆盖已保存的 OK 版本。

这些脚本只为完成真实交付服务；若现有工具已经提供相同可靠能力，直接复用，不为凑文件数重新编写。
