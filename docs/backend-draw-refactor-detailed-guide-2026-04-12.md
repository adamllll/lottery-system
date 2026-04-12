# 后端抽奖重构超详细傻瓜文档

> 文档目标：把这次“后端抽奖改造”讲到连第一次接触这段代码的人也能顺着看懂。
>
> 适合谁看：
> - 想知道本次到底改了什么
> - 想知道为什么要这么改
> - 想自己复盘一遍代码的人
> - 后面要继续维护抽奖功能的人

---

## 1. 先用一句人话讲清楚这次改造

这次改造的核心目标只有一句话：

**把“谁中奖”这件事从前端决定，改成后端决定。**

以前的模式更像是：

1. 前端先自己随机闪名字
2. 前端把中奖名单一起发给后端
3. 后端负责把前端给的名单保存下来

现在的新模式是：

1. 前端只负责发“请开始抽这个奖”的命令
2. 后端自己锁住奖项、自己挑中奖人、自己落库、自己改状态
3. 前端只展示后端返回的最终结果

这一条变化，是整次重构的根。

---

## 2. 为什么一定要改

如果不改，旧方案会有几个天然问题。

### 2.1 中奖名单由前端决定，不可靠

旧方案里，前端会把 `winnerList` 直接传给后端。

这意味着：

- 前端可以伪造中奖人
- 页面刷新、重复点击、并发请求时，前后端很容易不同步
- 真正的“抽奖权威结果”不在后端

从系统设计角度看，这是最危险的问题。

因为“谁中奖”属于核心业务结果，必须由后端说了算。

### 2.2 并发时容易重复抽同一个奖项

如果两个管理员几乎同时点了“开始抽奖”，或者同一个页面抖了两次请求：

- 两次请求可能都进来
- 两次都可能试图处理同一个 `activityId + prizeId`
- 就可能出现重复开奖、状态互相覆盖、缓存不一致

所以这次必须补“处理中”机制。

### 2.3 前端刷新后会丢失部分显示状态

旧页面里有一部分展示逻辑是依赖前端内存数据的：

- 页面一刷新，前端内存里的 `names`、`data.list` 就没了
- 某些奖项的中奖名单就会显示异常

这次顺手把这个坑也补了。

### 2.4 MQ 旧链路不能直接删

仓库里原本已经有 RabbitMQ 的抽奖链路和回归测试。

如果这次硬删：

- 现有测试会断
- 兼容历史逻辑的能力会丢
- 风险太大

所以这次采用的是：

**保留 MQ 入口，但让 MQ 和同步接口共用同一个开奖内核。**

这叫“共享执行内核”。

---

## 3. 改造后的整体设计长什么样

### 3.1 新设计的角色分工

#### 前端负责

- 发起 `POST /draw-prize`
- 做抽奖动画
- 收到 `200` 就展示结果
- 收到 `202` 就轮询 `/winning-records/show`

#### 后端负责

- 校验管理员权限
- 防并发
- 从候选人里选中奖者
- 改活动/奖项/用户状态
- 保存中奖记录
- 更新缓存
- 异步发邮件

### 3.2 新链路流程图

```text
管理员点击开始抽奖
        |
        v
前端 POST /draw-prize
请求体只带 activityId + prizeId
        |
        v
Controller 校验管理员身份
        |
        v
Service.executeDraw()
        |
        +--> Redis 短锁，防止同一奖项重复处理
        |
        +--> 事务里锁定 activity_prize
        |
        +--> 事务里锁定 activity_user(INIT)
        |
        +--> 后端随机选中奖人
        |
        +--> finalizeDrawInTransaction()
                |
                +--> 状态流转
                +--> 落中奖记录
                +--> 更新缓存
        |
        +--> 事务提交
        |
        +--> 异步发邮件
        |
        v
返回 winnerList 给前端
```

### 3.3 如果有并发请求会怎样

如果同一个 `activityId + prizeId` 已经在处理中：

- 第二个请求拿不到 Redis 锁
- 后端直接返回 `code = 202`
- 前端收到后不再重新抽，而是轮询最终结果

这样就避免了双写和重复开奖。

---

## 4. 本次到底新增了哪些类

这次新增了 4 个文件，都是为了把同步抽奖接口的“输入”和“输出”分干净。

### 4.1 `ExecuteDrawPrizeParam`

文件：

- `src/main/java/org/adam/lotterysystem/controller/param/ExecuteDrawPrizeParam.java`

它的职责只有一个：

**定义新版 `/draw-prize` 的最小入参。**

只保留两个字段：

- `activityId`
- `prizeId`

也就是说，前端不再提交：

- `winnerList`
- `winningTime`

这两个字段现在都应该由后端自己生成。

### 4.2 `DrawPrizeDTO`

文件：

- `src/main/java/org/adam/lotterysystem/service/dto/DrawPrizeDTO.java`

它是服务层内部的返回对象。

为什么要单独做一个 DTO：

- Service 层不应该直接依赖前端返回对象
- Controller 层和 Service 层的职责要分开

它里面包含：

- `activityId`
- `prizeId`
- `prizeName`
- `prizeTier`
- `winningTime`
- `winnerList`

### 4.3 `DrawPrizeResult`

文件：

- `src/main/java/org/adam/lotterysystem/controller/result/DrawPrizeResult.java`

它是最终返回给前端的结果对象。

和 `DrawPrizeDTO` 的差别主要在于：

- DTO 里 `prizeTier` 是枚举
- Result 里 `prizeTier` 是最终给前端看的字符串

这一步的意思就是：

**Service 层保留业务语义，Controller 层负责转成接口输出。**

### 4.4 `DrawPrizeControllerTest`

文件：

- `src/test/java/org/adam/lotterysystem/DrawPrizeControllerTest.java`

这个测试专门锁住控制层契约，避免以后再改歪。

它验证了 3 件事：

1. 非管理员不能抽奖
2. 正在处理时要返回 `202`
3. 成功时返回体结构必须正确

---

## 5. Controller 层怎么改的

文件：

- `src/main/java/org/adam/lotterysystem/controller/DrawPrizeController.java`

这层改动虽然不多，但非常关键。

### 5.1 `/draw-prize` 改成接收新参数

原来控制器接收的是旧的 `DrawPrizeParam`。

现在改成：

- 接收 `ExecuteDrawPrizeParam`

这一步的意义是：

**从接口入口处就掐断前端提交中奖名单的可能。**

### 5.2 增加管理员校验

控制器里新增了 `isAdmin(HttpServletRequest request)`。

逻辑很简单：

1. 从请求头里取 `user_token`
2. 用 `JWTUtil.parseJWT()` 解析 token
3. 从 claims 中取 `identity`
4. 判断是不是 `ADMIN`

如果不是管理员，就抛：

- `ControllerErrorCodeConstants.DRAW_PRIZE_FORBIDDEN`

对应提示语：

- `仅管理员可以执行抽奖`

这一步很重要，因为以前前端就算隐藏按钮，理论上普通用户还是可能自己伪造请求。

现在后端也兜住了。

### 5.3 增加 DTO -> Result 转换

Controller 不直接把 `DrawPrizeDTO` 原样吐给前端，而是做一次 `convertToDrawPrizeResult()`。

这样做的好处有两个：

1. 控制器负责接口格式
2. 服务层不必关心“前端最终长什么样”

这是典型的分层职责拆分。

---

## 6. Service 接口怎么改的

文件：

- `src/main/java/org/adam/lotterysystem/service/DrawPrizeService.java`

这次接口层新增了两个核心方法。

### 6.1 `executeDraw(ExecuteDrawPrizeParam param)`

这是新的同步抽奖入口。

专门给 `/draw-prize` 用。

它做的事情包括：

- 拿锁
- 校验
- 选人
- 调共享开奖内核
- 构造同步返回结果

### 6.2 `finalizeDraw(DrawPrizeParam param)`

这是共享开奖内核对外暴露的方法。

为什么保留它：

- 同步接口需要它
- MQ 兼容入口也需要它

这样同步链路和 MQ 链路就不会各自维护一套“状态流转 + 落库 + 通知”逻辑。

这就是本次设计里最核心的“共用内核”思想。

---

## 7. Redis 工具类怎么改的

文件：

- `src/main/java/org/adam/lotterysystem/common/utils/RedisUtil.java`

这里新增了两个方法。

### 7.1 `setIfAbsent(String key, String value, Long time)`

用途：

**只在 key 不存在时设置值，并带过期时间。**

它用来实现“短锁”。

这把锁的意思不是永久锁，而是：

- 某个奖项正在处理
- 短时间内别的请求不要再进来

### 7.2 `delIfMatch(String key, String value)`

用途：

**只有 value 匹配时才删除 key。**

为什么不能直接 `del(key)`：

因为锁可能已经过期，或者被别的请求重建了。

如果直接删：

- 可能会把别人刚加的新锁误删掉

所以这里用了 Lua 脚本：

```lua
if redis.call('get', KEYS[1]) == ARGV[1]
then return redis.call('del', KEYS[1])
else return 0
end
```

这一步是典型的“安全释放分布式锁”写法。

---

## 8. 数据访问层怎么改的

这里主要是给“事务内选人”提供数据库锁。

### 8.1 `ActivityPrizeMapper`

文件：

- `src/main/java/org/adam/lotterysystem/dao/mapper/ActivityPrizeMapper.java`

新增方法：

- `selectByActivityPrizeIdForUpdate(activityId, prizeId)`

SQL 末尾带：

- `for update`

作用：

在事务里锁住当前奖项行，防止并发事务同时修改同一个奖项状态。

### 8.2 `ActivityUserMapper`

文件：

- `src/main/java/org/adam/lotterysystem/dao/mapper/ActivityUserMapper.java`

新增方法：

- `selectByActivityIdAndStatusForUpdate(activityId, status)`

也是 `for update`。

作用：

在事务里锁住当前活动下状态为 `INIT` 的候选人。

这样做的目的是：

- 两个事务不会同时从同一批可抽用户里选人
- 避免重复抽中同一个人

---

## 9. 错误码怎么改的

### 9.1 控制层错误码

文件：

- `src/main/java/org/adam/lotterysystem/common/errorcode/ControllerErrorCodeConstants.java`

新增：

- `DRAW_PRIZE_FORBIDDEN = 400, "仅管理员可以执行抽奖"`

### 9.2 服务层错误码

文件：

- `src/main/java/org/adam/lotterysystem/common/errorcode/ServiceErrorCodeConstants.java`

新增：

- `DRAW_PRIZE_PROCESSING = 202, "当前奖项正在处理中"`
- `DRAW_PRIZE_CANDIDATE_NOT_ENOUGH = 404, "当前可抽取人员数量不足"`

这里要特别注意：

`202` 在这次不是 HTTP 状态码，而是项目自己的业务码。

也就是说接口仍然返回 HTTP 200，但 `CommonResult.code = 202`。

前端就是靠这个业务码判断“是否需要轮询”。

---

## 10. 这次最重要的文件：`DrawPrizeServiceImpl` 是怎么改的

文件：

- `src/main/java/org/adam/lotterysystem/service/impl/DrawPrizeServiceImpl.java`

这个文件是本次重构的核心中的核心。

你可以把它理解成：

**这次改造真正的大脑。**

下面本小姐按执行顺序一段一段讲。

---

## 11. `executeDraw()` 具体做了什么

### 11.1 先构造锁 key

代码思路：

```text
DrawPrizeExecuting_{activityId}_{prizeId}
```

用途：

- 同一个活动同一个奖项，只允许同时有一个请求在处理

### 11.2 再生成 lockValue

这里生成一个 `UUID` 作为锁值。

目的：

- 释放锁时做“值匹配”
- 防止误删别人的锁

### 11.3 拿 Redis 短锁

如果 `setIfAbsent()` 失败，说明别的请求已经在处理。

这时直接抛：

- `DRAW_PRIZE_PROCESSING`

前端就会看到：

- `code = 202`

### 11.4 在事务里查活动和奖项

事务内会做这些事：

1. 查活动 `activity`
2. 用 `for update` 查活动奖项 `activity_prize`
3. 校验活动和奖项是否合法

校验逻辑包括：

- 活动存在
- 奖项存在
- 奖项没结束
- 活动没结束

### 11.5 在事务里查候选人

这里不是随便查所有人，而是：

- 只查 `activity_user`
- 只查当前活动
- 只查状态是 `INIT` 的用户
- 并且 `for update`

这表示：

**只有还没中过奖的人，才是本轮候选人。**

### 11.6 候选人不足直接失败

如果候选人数 `< prizeAmount`：

- 直接抛 `DRAW_PRIZE_CANDIDATE_NOT_ENOUGH`

注意这里是“直接失败”，不是“抽一部分”。

这是计划里已经明确的边界：

**不允许部分开奖。**

### 11.7 后端随机挑人

调用的是：

- `buildServerSelectedDrawParam()`

里面做了两件事：

1. 拷贝候选人列表
2. `Collections.shuffle(..., secureRandom)`

然后取前 `prizeAmount` 个。

这里使用的是：

- `SecureRandom`

不是普通 `Random`。

原因很简单：

- 更适合抽奖这类对随机性要求更高的场景

### 11.8 组装成旧的 `DrawPrizeParam`

虽然同步接口的新入参是 `ExecuteDrawPrizeParam`，

但真正进入共享开奖内核之前，会重新组装成旧的 `DrawPrizeParam`，里面包含：

- `activityId`
- `prizeId`
- `winningTime`
- `winnerList`

为什么还要转回旧对象：

因为现有状态流转、落库、MQ 兼容逻辑，本来就是围绕 `DrawPrizeParam` 建的。

这样可以最大限度复用旧代码，而不是推翻重写。

### 11.9 调用共享事务内开奖内核

最终执行的是：

- `finalizeDrawInTransaction(drawPrizeParam)`

这里真正做的事只有两步：

1. `statusConvert(param)`
2. `saveWinnerRecords(param)`

也就是说：

**中奖者一旦后端选出来，后面的状态推进和落库就还是沿用原有业务链。**

### 11.10 事务提交后异步通知中奖人

事务成功返回后，会调用：

- `notifyWinnersAsync(winningRecordDOS)`

注意这里是事务外异步通知。

这意味着：

- 邮件发送失败不影响开奖成功
- 只记日志，不回滚中奖结果

这和计划要求完全一致。

### 11.11 构造同步返回结果

最后调用：

- `buildDrawPrizeDTO(drawPrizeParam, winningRecordDOS)`

把这次真实抽出来的结果返回给前端。

---

## 12. `finalizeDraw()` 为什么还要保留

这个方法现在的角色是：

**同步接口和 MQ 兼容入口共用的统一收尾方法。**

它会：

1. 在事务里执行 `finalizeDrawInTransaction()`
2. 成功后异步发邮件
3. 失败后做清理

这就保证：

- 同步抽奖
- MQ 抽奖

虽然入口不同，但最后处理结果的方式一致。

这种设计最大的好处是：

**以后你改“开奖收尾逻辑”时，只改一处。**

---

## 13. 失败清理为什么要单独写

这次新增了失败清理逻辑：

- `cleanupAfterDrawFailure()`
- `rollbackStatusIfNecessary()`
- `winnerNeedRollback()`
- `statusNeedRollback()`

这个设计是为了处理一种场景：

### 13.1 可能发生的中途失败

例如：

1. 状态已经推进成功
2. 保存中奖记录时抛异常
3. 或缓存、后续某一步出了问题

如果这时候不清理，系统就会出现：

- 奖项状态看起来已经抽完
- 但中奖记录不完整
- 缓存和数据库不一致

所以要补回滚。

### 13.2 回滚做了什么

失败时会尝试做三件事：

1. 回滚活动/奖项/用户状态
2. 删除已经落下去的中奖记录
3. 回刷活动缓存

这样能把系统尽量恢复到开奖前的状态。

### 13.3 为什么用了“尽量”这个词

因为回滚是补救动作，不是数据库自动回滚的万能替代品。

比如：

- 外部依赖异常
- 缓存异常
- 部分步骤已经提交

这时系统最多只能“最大努力恢复”。

但有这一层，总比完全不清理强得多。

---

## 14. MQ 兼容链路是怎么处理的

文件：

- `src/main/java/org/adam/lotterysystem/service/mq/MqReceiver.java`

这次没有删除 MQ 收消息入口，而是把它“瘦身”了。

### 14.1 现在 MQ Receiver 做的事很单纯

它现在只负责：

1. 打日志
2. 反序列化消息体
3. 用 `checkDrawPrizeStatus()` 校验旧请求是否还有效
4. 调 `drawPrizeService.finalizeDraw(param)`

### 14.2 为什么要先 `checkDrawPrizeStatus()`

因为历史 MQ 消息可能有重复投递。

例如：

- 同一个奖项已经结束
- 又来一条老消息

如果不先校验：

- 就可能把已经结束的奖项又处理一遍

所以这里保留了旧参数校验逻辑，作为兼容入口的保险丝。

### 14.3 为什么说这是“兼容旧链路”

因为现在新前端不再依赖 MQ 来完成抽奖。

但仓库里依然存在：

- MQ 配置
- Receiver
- DLX 回退逻辑
- 相关回归测试

所以本次最稳妥的做法不是删，而是改成：

**老入口继续存在，但只做入口，不再独立维护完整业务逻辑。**

---

## 15. 前端 `draw.html` 到底怎么改的

文件：

- `src/main/resources/static/draw.html`

这部分改动非常关键，因为它是和新后端契约真正对上的地方。

---

## 16. 前端请求参数改了什么

以前前端发 `/draw-prize` 时会带更多内容，甚至可能带中奖名单。

现在改成只发：

```json
{
  "activityId": 123,
  "prizeId": 18
}
```

这一步的本质是：

**前端只发命令，不发结果。**

---

## 17. 前端状态机怎么改了

以前主要是：

```text
showPic -> showBlink -> showList
```

现在变成：

```text
showPic -> showBlink -> saving -> showList
```

### 17.1 为什么新增 `saving`

因为以前用户点“确认”后，前端马上切展示阶段，容易造成：

- 请求还没回来
- 页面已经以为结果确定了

现在加一个 `saving` 状态后，意义很明确：

- 请求发出去了
- 但还没拿到结果
- 先别让用户继续点

这能避免很多状态错乱。

### 17.2 `saveLuck(data)` 现在怎么处理返回值

它现在按业务码分三种情况：

#### 情况 1：`code === 200`

说明本次请求直接开奖成功。

这时会调用：

- `applyServerWinnerList(data, result.data.winnerList || [])`

也就是说最终展示名单以**后端返回**为准。

#### 情况 2：`code === 202`

说明当前奖项已经有别的请求在处理。

这时不报错、不重抽，而是调用：

- `pollPrizeResult(data, 10)`

即轮询 `/winning-records/show`

#### 情况 3：其他错误码

直接提示错误，把页面回退到：

- `state = showPic`

让用户重新看到当前奖项。

---

## 18. `pollPrizeResult()` 是怎么工作的

这个函数是本次前端改造里最重要的新增逻辑之一。

它的行为是：

1. 调 `/winning-records/show`
2. 带 `activityId + prizeId`
3. 看当前奖项的中奖记录是否已经落库

如果已经查到结果：

- 说明另一条请求已经开奖完成
- 直接展示中奖名单

如果还没查到：

- 1 秒后再轮询一次

如果连续多次还是查不到：

- 提示用户稍后刷新页面查看

这一步为什么合理：

因为计划里已经明确：

**最终结果统一复用 `/winning-records/show` 查询，而不是让 `/draw-prize` 再去查历史结果。**

---

## 19. `applyServerWinnerList()` 为什么关键

这个函数做的是“以前前端自己决定结果，现在改成后端结果落地”的最后一步。

它主要做 4 件事：

1. 把后端 `winnerList` 转成页面当前奖项的 `data.list`
2. 从候选人 `names` 里移除中奖人
3. 把状态改成 `showList`
4. 直接调用 `showList(data)`

最重要的是第二步。

如果不把中奖人从 `names` 里移除：

- 后面奖项闪烁时，这些已经中过的人可能再次出现在随机候选里

虽然最终真正开奖还是以后端为准，但页面体验会很奇怪。

所以这里也把前端动画数据保持一致了。

---

## 20. 这次额外修掉的两个前端真实 bug

这两个不是计划原文里写死的内容，但联调时确实发现了，所以顺手修掉了。

### 20.1 bug 1：活动详情字段名读错了

现象：

- 前端从 `/activity-detail/find` 取字段时，还按旧名字读
- 例如还在用 `prizeId`、`prizeTierName`、`userName`

但后端实际给的是另一套字段名，比如：

- `id`
- `tiers`
- `UserName`

结果就是：

- 页面初始化后的奖项配置有些字段读不对

修法：

- 在 `reloadConf` 相关逻辑里做字段归一化处理

意思就是：

- 不管后端字段是旧名还是新名，前端先统一整理成自己内部用的格式

### 20.2 bug 2：刷新后单奖项回显会丢

现象：

- `showWinnerListWithPrize()` 以前会依赖前端内存里的 `names`
- 页面一刷新，`names` 的上下文就不完整
- 某个奖项的中奖名单可能显示不出来

修法：

- 不再靠前端自己反推
- 直接查 `/winning-records/show`

也就是说：

**页面刷新后的回显，现在彻底以后端中奖记录表为准。**

这才是正确的做法。

---

## 21. 一次完整抽奖从点击按钮到页面展示，按秒级流程怎么走

下面本小姐用“你点了一次开始抽奖”为例，按顺序讲。

### 第 1 步：管理员进入抽奖页面

前端会加载：

- 活动信息
- 奖项信息
- 候选人列表

当前奖项展示在页面上。

### 第 2 步：点击“开始抽奖”

前端进入：

- `showBlink`

这时只是视觉动画。

页面会快速随机闪候选人名字，制造抽奖效果。

注意：

**这一步不算真正开奖。**

### 第 3 步：点击“点我确定”

前端状态进入：

- `saving`

按钮文本变成：

- `抽奖中...`

同时请求：

- `POST /draw-prize`

请求体只带：

- `activityId`
- `prizeId`

### 第 4 步：Controller 校验管理员

如果 token 里的 `identity` 不是 `ADMIN`：

- 直接返回错误

普通用户到这里就被挡住了。

### 第 5 步：Service 尝试拿 Redis 锁

如果同一奖项已经有人在处理：

- 直接返回 `202`

前端就转去轮询。

### 第 6 步：事务里锁奖项、锁候选人

后端拿数据库锁，确保当前读取和修改是安全的。

### 第 7 步：后端随机选中奖人

使用 `SecureRandom` + `shuffle`。

这时真正的中奖名单才产生。

### 第 8 步：状态流转 + 保存中奖记录

这一步会：

- 标记奖项状态
- 标记中奖用户状态
- 必要时推进活动状态
- 保存 `winning_record`
- 刷新相关缓存

### 第 9 步：事务提交

到这里为止，开奖结果已经是“正式结果”。

### 第 10 步：异步发邮件

这一步失败也不影响开奖结果。

### 第 11 步：后端返回 `winnerList`

前端收到 `200` 后：

- 直接用后端结果更新页面

于是页面最终显示的就是：

- 真正后端选出来的人

不是前端闪烁动画里最后一帧是谁，就显示谁。

---

## 22. 为什么前端闪烁名单和最终名单可能不同

这点你一定要理解。

闪烁动画阶段：

- 只是前端拿 `names` 随机打乱展示

最终结果阶段：

- 以后端真实返回的 `winnerList` 为准

所以动画里你看到 `FAKE_UI_WINNER`，最后结果显示 `lisi`，这是完全正常的。

这正说明：

**现在系统的最终结果权威已经回到后端了。**

这是本次重构最重要的成功标志之一。

---

## 23. 测试是怎么补的

这次主要补了两类测试。

### 23.1 控制层测试

文件：

- `src/test/java/org/adam/lotterysystem/DrawPrizeControllerTest.java`

主要验证：

1. 非管理员调用 `/draw-prize` 会被拒绝
2. 服务层抛出处理中时，接口会正确映射成 `code = 202`
3. 服务层成功返回时，接口字段映射正确

### 23.2 集成测试

文件：

- `src/test/java/org/adam/lotterysystem/DrawPrizeTest.java`

主要覆盖：

- 同步抽奖成功
- Redis 锁存在时返回处理中
- 奖项已抽过时拒绝
- 候选人不足时报错
- `finalizeDraw()` 共享内核是否正常
- MQ 兼容入口是否仍然正确工作
- 保存中奖记录异常时是否回滚
- `ServiceException` 场景下是否回滚

这组测试的意义是：

**不仅验证新功能能跑，还验证出错时不会把系统搞脏。**

---

## 24. 手工验证是怎么做的

除了自动化测试，这次还做了手工联调。

主要验证了 3 条关键事实。

### 24.1 事实 1：后端结果会覆盖前端假名单

验证方式：

1. 故意把前端闪烁名单改成假的
2. 点击开奖
3. 观察最终展示名单

结果：

- 页面最后显示的是后端真实中奖人，不是假名字

### 24.2 事实 2：刷新后还能正确回显

验证方式：

1. 先抽完一个奖项
2. 刷新页面
3. 再看该奖项展示

结果：

- 页面会通过 `/winning-records/show` 正确回显中奖记录

### 24.3 事实 3：`202` 处理中链路成立

验证方式：

1. 人工制造“同一奖项正在处理中”
2. 再次请求 `/draw-prize`
3. 观察页面和日志

结果：

- 第二次请求拿到 `code = 202`
- 页面开始轮询 `/winning-records/show`
- 等结果落库后成功展示

这说明“并发不中断，但重复请求也不重复开奖”的目标已经达成。

---

## 25. 这次改造你最应该记住的 10 个关键词

如果你只想抓核心，请记住下面这 10 句。

1. 现在前端不再提交中奖名单。
2. 现在后端才是真正决定谁中奖的人。
3. `/draw-prize` 只接收 `activityId + prizeId`。
4. 同一个奖项并发请求会用 Redis 短锁挡住。
5. 被挡住的请求不会报废，而是返回 `202`。
6. 前端收到 `202` 后轮询 `/winning-records/show`。
7. 同步接口和 MQ 接口共用同一个开奖内核。
8. 邮件通知放到事务成功之后异步执行。
9. 失败时会尽量回滚状态、记录和缓存。
10. 页面最终显示永远以后端落库结果为准。

---

## 26. 以后你再看这套代码，推荐按什么顺序看

如果你要自己复盘代码，本小姐建议按这个顺序：

1. 先看 `DrawPrizeController`
2. 再看 `DrawPrizeService`
3. 再看 `DrawPrizeServiceImpl.executeDraw()`
4. 再看 `finalizeDraw()` / `finalizeDrawInTransaction()`
5. 再看 `RedisUtil`
6. 再看两个 Mapper 的 `for update`
7. 再看 `MqReceiver`
8. 最后看 `draw.html`

为什么这个顺序最好：

因为它和实际请求执行顺序几乎一致，最容易在脑子里串成完整流程。

---

## 27. 如果以后还要继续改，最容易踩的坑是什么

### 坑 1：不要把中奖名单重新交回前端决定

一旦你又让前端提交 `winnerList`，这次重构的意义就毁了。

### 坑 2：不要绕过 `executeDraw()` 直接在 Controller 里写业务

Controller 只做：

- 参数校验
- 权限校验
- 返回映射

不要把开奖逻辑塞进去。

### 坑 3：不要让同步链路和 MQ 链路再次分叉

如果以后同步改一套、MQ 改一套：

- 两边逻辑迟早会漂移

能共用就继续共用。

### 坑 4：不要删除 `delIfMatch()`

锁释放如果改回无脑 `del(key)`，并发边界又会变脏。

### 坑 5：不要让前端再次依赖内存去反推已开奖结果

刷新后的结果，一定要以后端记录表或缓存为准。

---

## 28. 这次改造的价值，最后再总结一遍

从“系统可信度”的角度看，这次最大的提升是：

- 抽奖结果从前端搬回后端

从“并发安全”的角度看，这次最大的提升是：

- 同一奖项重复请求会被正确处理成“处理中”

从“维护成本”的角度看，这次最大的提升是：

- 同步接口和 MQ 入口复用同一个收尾内核

从“用户体验”的角度看，这次最大的提升是：

- 前端刷新后能正确回显，动画展示和最终结果不再打架

所以这次不是简单“改个接口参数”，而是把抽奖这条业务链重新扶正了。

---

## 29. 你可以怎样拿这份文档继续学习

如果你想真正吃透这次改造，建议按下面的方式练一遍。

### 练习 1：只看文档，不看代码，自己口述一遍流程

目标：

- 能自己说出从点击按钮到返回结果的 11 步

### 练习 2：打开 `DrawPrizeServiceImpl`，对照本文逐行走

目标：

- 能知道每一段代码在整条链路里负责什么

### 练习 3：自己回答这 3 个问题

1. 为什么前端不能再传 `winnerList`
2. 为什么 `202` 之后要轮询 `/winning-records/show`
3. 为什么要保留 `finalizeDraw()` 给 MQ 复用

如果这 3 个问题你都能顺着讲明白，说明你已经真正理解这次改造了。

---

## 30. 本次涉及的核心文件清单

为了方便你跳转，这里把最关键的文件集中列一遍。

- `src/main/java/org/adam/lotterysystem/controller/DrawPrizeController.java`
- `src/main/java/org/adam/lotterysystem/controller/param/ExecuteDrawPrizeParam.java`
- `src/main/java/org/adam/lotterysystem/controller/result/DrawPrizeResult.java`
- `src/main/java/org/adam/lotterysystem/service/DrawPrizeService.java`
- `src/main/java/org/adam/lotterysystem/service/dto/DrawPrizeDTO.java`
- `src/main/java/org/adam/lotterysystem/service/impl/DrawPrizeServiceImpl.java`
- `src/main/java/org/adam/lotterysystem/service/mq/MqReceiver.java`
- `src/main/java/org/adam/lotterysystem/common/utils/RedisUtil.java`
- `src/main/java/org/adam/lotterysystem/dao/mapper/ActivityPrizeMapper.java`
- `src/main/java/org/adam/lotterysystem/dao/mapper/ActivityUserMapper.java`
- `src/main/resources/static/draw.html`
- `src/test/java/org/adam/lotterysystem/DrawPrizeControllerTest.java`
- `src/test/java/org/adam/lotterysystem/DrawPrizeTest.java`
- `README.md`

---

## 31. 最后一句话

如果你读完这份文档还是只记住一句话，那就记住这一句：

**这次改造的本质，不是“把接口改成同步”，而是“把抽奖结果的决定权重新收回后端”。**

这才是整件事真正值钱的地方，笨蛋。
