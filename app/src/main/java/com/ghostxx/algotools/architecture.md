# AlgoTools 清洁架构设计

## 架构层次

```
com.ghostxx.algotools/
├── domain/              # 领域层 - 包含业务逻辑和实体
│   ├── entity/          # 业务实体
│   ├── usecase/         # 用例 - 业务逻辑
│   └── repository/      # 仓库接口 - 定义数据操作契约
│
├── data/                # 数据层 - 实现数据获取和存储
│   ├── repository/      # 仓库实现
│   ├── source/          # 数据源 (本地、远程等)
│   ├── mapper/          # 数据转换器
│   └── model/           # 数据模型 (DTO)
│
├── presentation/        # 表示层 - UI和视图逻辑
│   ├── view/            # UI组件
│   │   ├── activity/    # Activity
│   │   ├── fragment/    # Fragment
│   │   ├── adapter/     # 适配器
│   │   └── dialog/      # 对话框
│   └── viewmodel/       # ViewModel - 处理UI状态和事件
│
└── common/              # 通用组件
    ├── di/              # 依赖注入
    ├── util/            # 工具类
    ├── extension/       # 扩展函数
    └── constant/        # 常量
```

## 各层职责

### 领域层 (Domain)

- 包含核心业务逻辑
- 定义业务实体
- 定义仓库接口
- 不依赖于其他层或框架

### 数据层 (Data)

- 实现领域层定义的仓库接口
- 管理数据源和数据获取逻辑
- 将数据源的数据模型映射到领域实体
- 只依赖于领域层

### 表示层 (Presentation)

- 包含UI组件和表示逻辑
- 使用ViewModel管理UI状态
- 通过依赖注入获取用例
- 只依赖于领域层

### 通用层 (Common)

- 提供应用程序各层都可能使用的功能
- 包含工具类、常量、扩展函数等
- 不包含业务逻辑

## 数据流

1. UI事件 → ViewModel
2. ViewModel → 用例
3. 用例 → 仓库接口
4. 仓库实现 → 数据源
5. 数据源 → 仓库实现 → 用例 → ViewModel → UI

## 依赖规则

- 内层不应该知道外层的存在
- 外层对内层的依赖通过依赖倒置原则实现
- 所有依赖都应指向领域层

## 示例用例流程：哈希分析

1. UI (Fragment) 捕获用户输入并调用 ViewModel 方法
2. ViewModel 调用相应的用例
3. 用例通过仓库接口获取数据
4. 仓库实现选择合适的数据源并获取数据
5. 数据返回给用例进行处理
6. 处理结果返回给 ViewModel
7. ViewModel 更新 UI 状态
8. UI 观察 ViewModel 状态变化并更新界面 