# 大麦 Android 抢票助手基础版 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个可旁加载安装的 Android APK，支持单账号、单任务的预约售票辅助流程：读取可见票档、选择预设观演人、按开售时间运行，遇到人机校验转人工，并在付款页面停止。

**Architecture:** 使用 Kotlin + Android Views/XML 构建本地单体应用。领域层负责任务校验、票档匹配和状态机；数据层使用应用私有 `SharedPreferences`；无障碍服务负责把大麦可见控件快照转换为不可变页面状态，并只执行经过状态机批准的点击；前台服务与精确定时器负责开售前唤醒，通知和悬浮窗负责人工接管。

**Tech Stack:** Android Gradle Plugin 8.7.3、Gradle 8.9、Kotlin 2.0.21、Java 17、compileSdk 35、targetSdk 35、minSdk 29、AndroidX Core/AppCompat/Material、JUnit 4、Robolectric、AndroidX Test、Espresso。

---

## 文件结构

首次创建以下文件和目录：

- Create: `settings.gradle.kts` - Gradle 插件和模块声明。
- Create: `build.gradle.kts` - 根项目插件版本。
- Create: `gradle.properties` - AndroidX、JVM 和构建设置。
- Create: `app/build.gradle.kts` - APK 模块、SDK、依赖和测试配置。
- Create: `app/proguard-rules.pro` - 首版最小压缩规则。
- Create: `app/src/main/AndroidManifest.xml` - 权限、Activity、前台服务和无障碍服务注册。
- Create: `app/src/main/res/values/strings.xml` - 所有用户可见文案。
- Create: `app/src/main/res/values/colors.xml` - 基础颜色。
- Create: `app/src/main/res/values/themes.xml` - Material 主题。
- Create: `app/src/main/res/xml/accessibility_service_config.xml` - 无障碍服务能力配置。
- Create: `app/src/main/res/layout/activity_main.xml` - 主界面。
- Create: `app/src/main/res/layout/activity_task_editor.xml` - 任务编辑界面。
- Create: `app/src/main/res/layout/item_permission.xml` - 权限状态项。
- Create: `app/src/main/res/layout/item_ticket_tier.xml` - 票档配置项。
- Create: `app/src/main/res/layout/item_viewer.xml` - 观演人配置项。
- Create: `app/src/main/res/drawable/ic_stat_assistant.xml` - 前台服务通知图标。
- Create: `app/src/main/java/com/example/damaiassistant/MainActivity.kt` - 权限状态和任务入口。
- Create: `app/src/main/java/com/example/damaiassistant/TaskEditorActivity.kt` - 单任务编辑和保存。
- Create: `app/src/main/java/com/example/damaiassistant/model/TaskConfig.kt` - 任务、票档和观演人数据模型。
- Create: `app/src/main/java/com/example/damaiassistant/model/PageModels.kt` - 不可变页面状态和可见控件模型。
- Create: `app/src/main/java/com/example/damaiassistant/domain/TaskValidator.kt` - 创建/启动任务校验。
- Create: `app/src/main/java/com/example/damaiassistant/domain/TicketMatcher.kt` - 票档标准化和优先级匹配。
- Create: `app/src/main/java/com/example/damaiassistant/domain/ViewerSelector.kt` - 固定观演人集合校验和页面匹配。
- Create: `app/src/main/java/com/example/damaiassistant/domain/PageStateClassifier.kt` - 页面状态和人工接管原因识别。
- Create: `app/src/main/java/com/example/damaiassistant/domain/PurchaseStateMachine.kt` - 购票状态转换。
- Create: `app/src/main/java/com/example/damaiassistant/data/TaskRepository.kt` - 本地任务序列化和存储。
- Create: `app/src/main/java/com/example/damaiassistant/data/LocalEventLog.kt` - 不含敏感数据的本地日志。
- Create: `app/src/main/java/com/example/damaiassistant/permission/PermissionCoordinator.kt` - 权限检测和系统设置跳转。
- Create: `app/src/main/java/com/example/damaiassistant/schedule/TaskScheduler.kt` - 开售前唤醒和精确定时。
- Create: `app/src/main/java/com/example/damaiassistant/service/TaskRunnerService.kt` - 前台服务和任务倒计时。
- Create: `app/src/main/java/com/example/damaiassistant/service/DamaiAccessibilityService.kt` - 大麦窗口监听和受控执行。
- Create: `app/src/main/java/com/example/damaiassistant/accessibility/NodeSnapshotter.kt` - 无障碍节点树快照。
- Create: `app/src/main/java/com/example/damaiassistant/accessibility/DamaiPageReader.kt` - 可见页面解析为标准页面状态。
- Create: `app/src/main/java/com/example/damaiassistant/accessibility/UiActionExecutor.kt` - 受状态机授权的点击执行。
- Create: `app/src/main/java/com/example/damaiassistant/notify/HumanHandoffNotifier.kt` - 验证码/异常/付款页通知。
- Create: `app/src/main/java/com/example/damaiassistant/overlay/TaskOverlayController.kt` - 悬浮暂停、继续和停止控件。
- Create: `app/src/test/java/com/example/damaiassistant/domain/TaskValidatorTest.kt` - 任务校验测试。
- Create: `app/src/test/java/com/example/damaiassistant/domain/TicketMatcherTest.kt` - 票档匹配测试。
- Create: `app/src/test/java/com/example/damaiassistant/domain/ViewerSelectorTest.kt` - 观演人选择测试。
- Create: `app/src/test/java/com/example/damaiassistant/domain/PageStateClassifierTest.kt` - 页面状态识别测试。
- Create: `app/src/test/java/com/example/damaiassistant/domain/PurchaseStateMachineTest.kt` - 状态机测试。
- Create: `app/src/test/java/com/example/damaiassistant/accessibility/DamaiPageReaderTest.kt` - 合成无障碍树解析测试。
- Create: `app/src/test/java/com/example/damaiassistant/data/TaskRepositoryTest.kt` - 本地存储往返测试。
- Create: `app/src/androidTest/java/com/example/damaiassistant/PermissionGateInstrumentedTest.kt` - 权限界面基本测试。
- Create: `app/src/androidTest/java/com/example/damaiassistant/MainActivityInstrumentedTest.kt` - 任务编辑界面测试。
- Create: `README.md` - APK 安装、权限和测试说明。

## Task 1: 创建可编译的 Android 工程

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: 创建 Gradle 基础文件**

  使用 Kotlin DSL，根项目只声明 Android Application 和 Kotlin Android 插件，模块使用 Java 17、`minSdk = 29`、`targetSdk = 35`。应用 ID 固定为 `com.example.damaiassistant`，版本从 `1.0.0` 开始。

- [ ] **Step 2: 添加最小 AndroidX 依赖**

  在 `app/build.gradle.kts` 中加入 `core-ktx`、`appcompat`、`material`、`activity-ktx`、JUnit、AndroidX Test 和 Espresso；首版不引入网络、账号系统、OCR 或云服务依赖。

- [ ] **Step 3: 创建最小 Manifest 和主题资源**

  先注册一个空 `MainActivity`，声明应用私有存储，不请求存储权限。主题使用 Material Components，确保模拟器 API 29 至 API 35 都能启动。

- [ ] **Step 4: 运行首个构建**

  Run: `./gradlew.bat assembleDebug`

  Expected: `BUILD SUCCESSFUL`，生成 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 5: 安装并启动空壳 APK**

  Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

  Expected: APK 安装成功，主 Activity 能启动且没有崩溃。

## Task 2: 建立领域模型和任务校验

**Files:**
- Create: `app/src/main/java/com/example/damaiassistant/model/TaskConfig.kt`
- Create: `app/src/main/java/com/example/damaiassistant/model/PageModels.kt`
- Create: `app/src/main/java/com/example/damaiassistant/domain/TaskValidator.kt`
- Test: `app/src/test/java/com/example/damaiassistant/domain/TaskValidatorTest.kt`

- [ ] **Step 1: 写失败测试，锁定任务规则**

  测试以下规则：`ticketCount = 2` 且恰好 2 个观演人时通过；观演人数少于或多于票数时失败；活动名、场次、开售时间为空时失败；至少配置一个票档时通过；票数必须为正数。

  测试接口固定为：

  ```kotlin
  fun validate(config: TaskConfig, nowEpochMs: Long): List<ValidationError>
  ```

- [ ] **Step 2: 运行测试确认失败**

  Run: `./gradlew.bat testDebugUnitTest --tests '*TaskValidatorTest'`

  Expected: 因为 `TaskConfig` 和 `TaskValidator` 尚未实现而失败。

- [ ] **Step 3: 实现不可变模型和校验器**

  `TaskConfig` 包含 `eventName`、`performanceName`、`releaseAtEpochMs`、`ticketCount`、`viewers`、`ticketPreferences`、`enabled`。`ViewerRef` 只保存展示名称，不保存身份证号码；`TicketPreference` 保存名称、价格元值和优先级。

  ```kotlin
  data class TaskConfig(
      val eventName: String,
      val performanceName: String,
      val releaseAtEpochMs: Long,
      val ticketCount: Int,
      val viewers: List<ViewerRef>,
      val ticketPreferences: List<TicketPreference>,
      val enabled: Boolean = false
  )
  ```

- [ ] **Step 4: 运行测试确认通过**

  Run: `./gradlew.bat testDebugUnitTest --tests '*TaskValidatorTest'`

  Expected: 所有任务校验测试通过。

- [ ] **Step 5: Commit**

  当前工作区没有 Git 仓库；若实施前仍没有仓库，则保留文件变更并在进度记录中注明无法提交。

## Task 3: 实现本地任务存储和状态日志

**Files:**
- Create: `app/src/main/java/com/example/damaiassistant/data/TaskRepository.kt`
- Create: `app/src/main/java/com/example/damaiassistant/data/LocalEventLog.kt`
- Test: `app/src/test/java/com/example/damaiassistant/data/TaskRepositoryTest.kt`

- [ ] **Step 1: 写失败的存储往返测试**

  创建一份包含 2 张票、2 位观演人和 3 个票档优先级的 `TaskConfig`，保存后读取，断言所有字段一致；另测空任务和删除任务。

- [ ] **Step 2: 运行测试确认失败**

  Run: `./gradlew.bat testDebugUnitTest --tests '*TaskRepositoryTest'`

  Expected: 因为仓储尚未实现而失败。

- [ ] **Step 3: 用 SharedPreferences 实现单任务存储**

  使用应用私有 `SharedPreferences` 保存版本号和 JSON 字符串。序列化只包含任务配置，不写入账号密码、身份证号码、截图和支付内容；读取未知版本时返回明确错误而不是静默使用部分字段。仓储通过 `PreferencesStore` 接口隔离 Android API，生产实现使用 `SharedPreferences`，JVM 测试使用内存实现。

- [ ] **Step 4: 实现脱敏事件日志**

  `LocalEventLog.append(type, state, timestamp)` 只写事件类型、状态和时间。禁止把无障碍节点文本、页面截图、完整姓名或身份证内容写入日志。

- [ ] **Step 5: 运行测试确认通过**

  Run: `./gradlew.bat testDebugUnitTest --tests '*TaskRepositoryTest'`

  Expected: 存储往返、删除和敏感字段排除测试通过。

## Task 4: 实现票档标准化、匹配和观演人选择规则

**Files:**
- Modify: `app/src/main/java/com/example/damaiassistant/model/PageModels.kt`
- Create: `app/src/main/java/com/example/damaiassistant/domain/TicketMatcher.kt`
- Test: `app/src/test/java/com/example/damaiassistant/domain/TicketMatcherTest.kt`

- [ ] **Step 1: 写票档匹配失败测试**

  覆盖以下输入：主选可用时选择主选；主选售罄时选择第一备用；所有票档不可用时返回 `NoAvailableTier`；同名同价候选无法区分时返回 `AmbiguousTier`；未配置票档不能被选择。

- [ ] **Step 2: 运行测试确认失败**

  Run: `./gradlew.bat testDebugUnitTest --tests '*TicketMatcherTest'`

  Expected: 因为匹配器尚未实现而失败。

- [ ] **Step 3: 实现标准化和优先级匹配**

  标准化规则只处理可见文本：去除首尾空白、压缩连续空白、统一货币符号和价格格式；匹配键为标准化名称与整数价格。状态为可选且唯一时才返回可执行候选。

  ```kotlin
  sealed interface TicketMatchResult {
      data class Selected(val tier: VisibleTicketTier) : TicketMatchResult
      data object NoAvailableTier : TicketMatchResult
      data object AmbiguousTier : TicketMatchResult
  }
  ```

- [ ] **Step 4: 实现观演人数验证**

  在 `ViewerSelector.kt` 中实现纯函数：先验证 `selectedViewerNames.size == ticketCount`，再按标准化展示名称匹配可见列表；缺失或同名冲突返回人工接管原因，不选择页面中的第一项。为 `ViewerSelectorTest.kt` 添加数量一致、缺失和同名冲突测试。

- [ ] **Step 5: 运行测试确认通过**

  Run: `./gradlew.bat testDebugUnitTest --tests '*TicketMatcherTest'`

  Expected: 票档优先级、售罄跳过、未配置不选和观演人数规则测试通过。

## Task 5: 建立不可变无障碍节点快照和页面解析器

**Files:**
- Create: `app/src/main/java/com/example/damaiassistant/accessibility/NodeSnapshotter.kt`
- Create: `app/src/main/java/com/example/damaiassistant/accessibility/DamaiPageReader.kt`
- Modify: `app/src/main/java/com/example/damaiassistant/model/PageModels.kt`
- Test: `app/src/test/java/com/example/damaiassistant/accessibility/DamaiPageReaderTest.kt`

- [ ] **Step 1: 定义不可变节点模型和合成树测试夹具**

  `UiNodeSnapshot` 包含 `text`、`contentDescription`、`className`、`clickable`、`enabled`、`selected`、`checked`、`left/top/right/bottom` 和 `children`。测试不直接构造 Android 框架节点，而使用纯 Kotlin 合成树。

- [ ] **Step 2: 写页面解析失败测试**

  用合成树测试：票档容器包含名称、价格和“可选”时能解析；同一票档显示“售罄”时状态正确；观演人列表能解析；包含“验证码”“滑块”“付款”“收银台”等关键词时分别生成对应状态；未知页面返回 `Unknown`。

- [ ] **Step 3: 运行测试确认失败**

  Run: `./gradlew.bat testDebugUnitTest --tests '*DamaiPageReaderTest'`

  Expected: 因为页面读取器尚未实现而失败。

- [ ] **Step 4: 实现节点快照转换**

  `NodeSnapshotter` 从当前窗口根节点递归复制可见节点，限制最大节点数量为 2,000、最大深度为 40，并在复制后立即释放框架节点引用；遇到循环或空节点时跳过。

- [ ] **Step 5: 实现页面解析**

  `DamaiPageReader.read(root: UiNodeSnapshot): VisiblePage` 先提取文本索引，再按父容器组合票档名称、价格、状态和边界；页面分类只依赖当前可见文本和控件状态，不读取网络或隐藏数据。

- [ ] **Step 6: 运行测试确认通过**

  Run: `./gradlew.bat testDebugUnitTest --tests '*DamaiPageReaderTest'`

  Expected: 票档、观演人、人机校验、付款页和未知页面解析测试通过。

## Task 6: 实现页面状态机和人工接管边界

**Files:**
- Create: `app/src/main/java/com/example/damaiassistant/domain/PageStateClassifier.kt`
- Create: `app/src/main/java/com/example/damaiassistant/domain/PurchaseStateMachine.kt`
- Test: `app/src/test/java/com/example/damaiassistant/domain/PageStateClassifierTest.kt`
- Test: `app/src/test/java/com/example/damaiassistant/domain/PurchaseStateMachineTest.kt`

- [ ] **Step 1: 定义页面和任务状态**

  定义 `PurchaseState`：`NotConfigured`、`WaitingForRelease`、`Preparing`、`SelectingTicket`、`SelectingQuantity`、`SelectingViewers`、`HumanHandoff`、`Submitting`、`StoppedAtPayment`、`StoppedByUser`、`Failed`。

- [ ] **Step 2: 写状态机失败测试**

  测试预约页进入等待状态、开售后进入选票、验证码进入人工接管、恢复后重新读取当前页面、付款页转终止、停止后不再产生点击动作。

- [ ] **Step 3: 运行测试确认失败**

  Run: `./gradlew.bat testDebugUnitTest --tests '*PurchaseStateMachineTest'`

  Expected: 因为状态机尚未实现而失败。

- [ ] **Step 4: 实现显式状态转换表**

  状态机只接收不可变 `VisiblePage` 和 `TaskConfig`，输出 `StateDecision`，其中包含下一状态、最多一个明确页面动作和人工接管原因。未知页面、重复事件和过期节点都不得生成默认点击。

- [ ] **Step 5: 写页面分类失败测试并实现分类器**

  分类器根据可见关键词和控件状态识别付款、验证码、滑块、短信验证、登录失效、排队、预约、票档选择和未知页面。付款关键词优先级高于普通按钮，确保识别到付款页后直接停止。

- [ ] **Step 6: 运行测试确认通过**

  Run: `./gradlew.bat testDebugUnitTest --tests '*PageStateClassifierTest' '*PurchaseStateMachineTest'`

  Expected: 页面分类和状态转换测试全部通过。

## Task 7: 实现权限检测和华为系统设置引导

**Files:**
- Create: `app/src/main/java/com/example/damaiassistant/permission/PermissionCoordinator.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/androidTest/java/com/example/damaiassistant/PermissionGateInstrumentedTest.kt`

- [ ] **Step 1: 声明首版权限和服务**

  Manifest 声明 `POST_NOTIFICATIONS`、`SYSTEM_ALERT_WINDOW`、前台服务权限、精确定时权限和无障碍服务；不声明存储、通讯录、短信、电话或账号密码相关权限。

- [ ] **Step 2: 实现权限状态模型**

  `PermissionCoordinator` 输出每项权限的 `granted`、`required`、`label` 和 `settingsIntent`。首版任务启动的必需项为无障碍、通知、悬浮窗、精确定时和华为后台运行/自启动能力；前台服务由启动结果确认。

- [ ] **Step 3: 写权限门禁测试**

  测试任何必需权限缺失时 `canRunTask == false`，全部满足且服务状态可用时 `canRunTask == true`。

- [ ] **Step 4: 实现系统设置跳转和不可用门禁**

  主界面逐项显示状态，点击权限项进入对应系统设置；任务按钮在门禁未通过时禁用，并显示具体缺失项。针对华为设备使用厂商设置入口失败后的通用 Android 设置入口作为回退。

- [ ] **Step 5: 运行测试确认通过**

  Run: `./gradlew.bat connectedDebugAndroidTest --tests '*PermissionGateInstrumentedTest'`

  Expected: 模拟器中权限状态列表和任务按钮门禁测试通过。

## Task 8: 实现前台任务服务和开售计时

**Files:**
- Create: `app/src/main/java/com/example/damaiassistant/schedule/TaskScheduler.kt`
- Create: `app/src/main/java/com/example/damaiassistant/service/TaskRunnerService.kt`
- Create: `app/src/main/res/drawable/ic_stat_assistant.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 实现任务时间计算的纯函数**

  以开售时间减去 120 秒作为准备时间，返回 `PrepareAt` 和 `ReleaseAt`；过去的开售时间直接拒绝任务，避免立即执行旧任务。

- [ ] **Step 2: 配置精确定时器**

  `TaskScheduler` 使用 `AlarmManager.setExactAndAllowWhileIdle` 设置准备时间闹钟；准备闹钟启动 `TaskRunnerService`，服务进入前台并使用 `SystemClock.elapsedRealtime()` 计算到开售的剩余时间。

- [ ] **Step 3: 创建前台服务通知**

  服务创建 `assistant_task` 通知频道，通知显示任务名称、倒计时、暂停和停止入口。服务只负责计时和状态，不直接遍历大麦页面。

- [ ] **Step 4: 实现停止和重启恢复**

  用户停止、任务超时、付款页停止或权限撤销时取消闹钟并停止服务；应用重启时从 `TaskRepository` 恢复唯一任务状态，不恢复已经进入付款页或失败的任务。

- [ ] **Step 5: 增加测试和构建验证**

  为时间计算写 JVM 测试；运行 `./gradlew.bat testDebugUnitTest` 和 `./gradlew.bat assembleDebug`，Expected: 全部通过且 APK 生成成功。

## Task 9: 实现无障碍服务和受控动作执行

**Files:**
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Create: `app/src/main/java/com/example/damaiassistant/service/DamaiAccessibilityService.kt`
- Create: `app/src/main/java/com/example/damaiassistant/accessibility/UiActionExecutor.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 注册无障碍服务配置**

  配置 `canRetrieveWindowContent=true`，监听窗口状态和内容变化，设置 50ms 事件节流；服务只接受用户在任务配置中确认的目标大麦包名。

- [ ] **Step 2: 实现目标包过滤**

  如果当前窗口不是目标大麦包名，服务不读取、不点击、不改变状态；目标包名通过用户确认的已安装“大麦”应用记录到本地任务配置。

- [ ] **Step 3: 实现节点快照和事件防抖**

  每次合并后的内容事件只生成一个快照，丢弃 300ms 内相同页面签名的重复事件；节点引用只在当前事件内使用，动作完成后重新读取页面。

- [ ] **Step 4: 实现安全点击策略**

  `UiActionExecutor` 只接受状态机输出的 `ClickTarget`；优先调用 `ACTION_CLICK`，节点不可点击、边界无效、文本不匹配或页面状态已变更时返回失败并转人工接管；不使用固定屏幕坐标盲点。

- [ ] **Step 5: 增加服务日志和手动停止入口**

  记录状态转换和错误类型，不记录节点原文；收到用户停止后立即取消待执行动作并停止前台任务服务。

## Task 10: 实现任务编辑界面、票档读取和观演人配置

**Files:**
- Modify: `app/src/main/java/com/example/damaiassistant/MainActivity.kt`
- Create: `app/src/main/java/com/example/damaiassistant/TaskEditorActivity.kt`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/layout/activity_task_editor.xml`
- Create: `app/src/main/res/layout/item_permission.xml`
- Create: `app/src/main/res/layout/item_ticket_tier.xml`
- Create: `app/src/main/res/layout/item_viewer.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 写 Activity 启动和表单验证测试**

  测试任务编辑页显示活动、场次、开售时间、票数、票档、观演人和保存按钮；票数为 2 时必须选择 2 人；必需权限缺失时开始按钮不可用。

- [ ] **Step 2: 实现主界面权限卡片和任务状态**

  主界面只显示一个任务卡片、权限状态列表、创建/编辑任务入口和开始/停止按钮；所有文案从 `strings.xml` 获取。

- [ ] **Step 3: 实现任务编辑表单**

  表单保存前调用 `TaskValidator`；保存成功后写入 `TaskRepository`，失败时在对应字段旁显示具体原因。用户可从大麦当前页面点击“读取票档”，由无障碍服务返回可见候选供选择。

- [ ] **Step 4: 实现票档优先级配置**

  每个票档项显示名称、价格、可选状态和优先级；用户只能启用已读取或手动输入的票档；重复名称和价格必须提示确认，不自动合并。

- [ ] **Step 5: 实现观演人固定选择**

  用户填写或从当前大麦页面读取观演人展示名称，使用复选框选择；选择数量不等于票数时保存和开始按钮均不可用；不提供身份证号码输入框。

- [ ] **Step 6: 运行界面测试和构建**

  Run: `./gradlew.bat connectedDebugAndroidTest --tests '*MainActivityInstrumentedTest'`

  Expected: 表单校验、保存和任务门禁测试通过。

## Task 11: 接入人工接管、通知和付款页停止

**Files:**
- Create: `app/src/main/java/com/example/damaiassistant/notify/HumanHandoffNotifier.kt`
- Create: `app/src/main/java/com/example/damaiassistant/overlay/TaskOverlayController.kt`
- Modify: `app/src/main/java/com/example/damaiassistant/domain/PurchaseStateMachine.kt`
- Modify: `app/src/main/java/com/example/damaiassistant/service/DamaiAccessibilityService.kt`

- [ ] **Step 1: 写人工接管状态测试**

  测试验证码、滑块、短信验证、登录失效、未知页面、付款页分别产生对应通知原因；付款页不能产生“继续点击”动作。

- [ ] **Step 2: 实现通知频道和通知动作**

  通知至少包含“处理人机校验”“返回大麦”“继续任务”“停止任务”四种明确动作；通知正文不包含完整姓名、身份证号、账号或支付信息。

- [ ] **Step 3: 实现悬浮控件**

  悬浮控件显示当前状态和停止按钮；仅在任务运行和人工接管状态显示；用户点击停止后状态机进入 `StoppedByUser`。

- [ ] **Step 4: 实现付款页终止保护**

  一旦 `PageStateClassifier` 返回付款页，立即停止服务内的自动动作队列、取消重复事件处理、隐藏悬浮控件中的“继续自动操作”按钮，并发送付款接管通知。

- [ ] **Step 5: 运行单元测试和构建**

  Run: `./gradlew.bat testDebugUnitTest assembleDebug`

  Expected: 状态机、人工接管和付款页停止测试通过，APK 正常生成。

## Task 12: 完成模拟器验证和 APK 交付

**Files:**
- Create: `README.md`
- Modify: `progress.md`
- Modify: `task_plan.md`

- [ ] **Step 1: 创建模拟器测试清单**

  在 `README.md` 记录安装命令、必需权限、目标大麦包名确认、任务创建步骤和停止规则；明确模拟器结果不能代表 HarmonyOS 5 真机兼容。

- [ ] **Step 2: 执行自动化测试**

  Run: `./gradlew.bat testDebugUnitTest connectedDebugAndroidTest`

  Expected: JVM 和 Android instrumentation 测试通过。

- [ ] **Step 3: 在模拟器安装 APK 并验证权限门禁**

  Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

  验证：关闭任一必需权限时不能创建/启动任务；全部权限满足时可以创建单任务。

- [ ] **Step 4: 执行合成页面流程测试**

  使用测试页面或合成无障碍节点验证票档读取、主选/备用切换、4 人选 2 人、验证码暂停、人工恢复和付款页停止；不输入支付密码，不进行真实支付。

- [ ] **Step 5: 生成可分发 APK 和校验值**

  Run: `./gradlew.bat assembleRelease`

  Expected: 生成签名配置要求明确的 Release 构建；若尚未配置正式签名，只交付 Debug APK 并在 README 中标注不可用于长期升级。对最终 APK 计算 SHA-256 并写入进度记录。

- [ ] **Step 6: 记录华为真机验证缺口**

  在没有 Mate 30 Pro 真机前，明确记录 HarmonyOS 5 的无障碍、后台保活、悬浮窗和大麦页面读取尚未实测，不宣称已完成目标设备兼容。

## 验证命令汇总

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat connectedDebugAndroidTest
./gradlew.bat assembleDebug
./gradlew.bat assembleRelease
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 计划自检

- 设计文档中的单账号、单设备、单任务、票档优先级、固定观演人、人工接管和付款页停止均有对应任务。
- 未加入验证码绕过、隐藏接口、批量设备、支付自动化或账号密码收集功能。
- 所有领域规则先写 JVM 失败测试，再实现最小逻辑。
- 页面识别通过不可变节点快照测试，避免直接依赖真实设备节点。
- 华为 HarmonyOS 5 真机验证被单独列为发布前条件，没有把模拟器结果当成最终兼容性结论。
- 计划中的文件路径、测试命令和模块职责已经逐项核对。
