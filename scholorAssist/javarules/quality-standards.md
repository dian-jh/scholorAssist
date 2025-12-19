---
description: 文档质量要求和审查标准
globs: 
alwaysApply: true
---

# 文档质量要求和审查标准

## 概述

本文档定义了文献辅助阅读系统开发过程中的文档质量要求和代码审查标准，确保项目的可维护性、可扩展性和代码质量。

## 文档质量要求

### 强制要求 🔴

#### 1. 文档完整性
- **API文档**: 所有接口必须有完整的Swagger/OpenAPI文档
- **数据库文档**: 所有表结构变更必须更新docs/tables.md
- **业务文档**: 重要业务逻辑必须有详细说明文档
- **部署文档**: 必须提供完整的部署和配置说明

#### 2. 文档准确性
- 文档内容必须与实际代码保持一致
- 示例代码必须能够正常运行
- 配置参数必须准确无误
- 版本信息必须及时更新

#### 3. 全局响应处理规范
- **严格使用** sc-common 中的全局响应处理器
- **禁止** 在文档中展示手动封装 Result 的示例
- **确保** 所有代码示例符合全局处理器规范

#### 4. 文档行数控制
- **每个规则文档** 必须严格控制在350行以内
- **超出限制** 的文档必须拆分为多个子文档
- **删除冗余** 注释和重复示例代码
- **精简非核心** 的业务逻辑描述

### 建议要求 🟡

#### 1. 文档易读性
- 使用清晰的标题和章节结构
- 提供目录和导航链接
- 避免过于技术化的表述
- 优先级：规范说明 > 代码示例

#### 2. 文档实用性
- 提供常见问题解决方案
- 包含最佳实践和注意事项
- 每个文档只包含1个完整接口示例
- 核心逻辑不超过50行实现

## 代码审查标准

### 审查流程

#### 1. 提交前自检
```bash
# 代码格式检查
mvn spotless:check

# 单元测试
mvn test

# 代码质量检查
mvn sonar:sonar
```

#### 2. 同行评审
- 每个Pull Request必须至少有一人审查
- 审查者必须是相关模块的负责人或资深开发者
- 审查内容包括代码逻辑、性能、安全性
- 必须通过所有自动化检查才能合并

#### 3. 全局响应处理器检查
- 检查Controller层是否直接返回业务对象
- 确认没有手动封装Result返回
- 验证异常处理是否使用BusinessException
- 确保全局处理器能正确捕获所有异常

### 代码质量标准

#### 1. 代码规范
```java
// ✅ 正确：符合全局响应处理器规范
@PostMapping("/register")
public UserInfoResponse register(@Valid @RequestBody UserRegisterRequest request) {
    return userService.register(request);
}

// ❌ 错误：手动封装Result
@PostMapping("/register")
public Result<UserInfoResponse> register(@Valid @RequestBody UserRegisterRequest request) {
    return Result.ok(userService.register(request));
}
```

#### 2. 异常处理规范
```java
// ✅ 正确：抛出BusinessException
public UserInfoResponse register(UserRegisterRequest request) {
    if (userManager.existsByUsername(request.getUsername())) {
        throw new BusinessException(400, "用户名已存在");
    }
    return createUser(request);
}

// ❌ 错误：手动处理异常
public Result<UserInfoResponse> register(UserRegisterRequest request) {
    try {
        return Result.ok(createUser(request));
    } catch (Exception e) {
        return Result.error(500, "注册失败");
    }
}
```

#### 3. 性能标准
- 接口响应时间 < 500ms
- 数据库查询优化，避免N+1问题
- 合理使用缓存机制
- 批量操作优于循环单条操作

#### 4. 安全标准
- 所有输入参数必须进行校验
- 敏感数据必须加密存储
- 防止SQL注入攻击
- 实现接口访问控制

### 测试质量标准

#### 1. 单元测试
```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserManager userManager;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    void testRegister_Success() {
        // Given
        UserRegisterRequest request = createRegisterRequest();
        when(userManager.existsByUsername(anyString())).thenReturn(false);
        
        // When
        UserInfoResponse response = userService.register(request);
        
        // Then
        assertThat(response.getUsername()).isEqualTo(request.getUsername());
    }
}
```

#### 2. 测试覆盖率要求
- 单元测试覆盖率 > 80%
- 关键业务逻辑覆盖率 > 90%
- 异常场景必须有对应测试用例
- 边界条件测试覆盖

#### 3. 集成测试
- 使用TestContainers进行数据库测试
- Mock外部服务依赖
- 测试完整的业务流程
- 验证全局响应处理器工作正常

## SonarQube质量门禁

### 质量门禁标准
- **代码覆盖率**: > 80%
- **重复代码率**: < 3%
- **代码复杂度**: 单个方法 < 10
- **代码异味**: 0个阻断级别问题
- **安全漏洞**: 0个高危漏洞
- **可靠性**: A级评分

### 代码质量指标
```yaml
# sonar-project.properties
sonar.projectKey=scholar-assist
sonar.projectName=Scholar Assist
sonar.projectVersion=1.0

# 覆盖率要求
sonar.coverage.exclusions=**/*Application.java,**/*Config.java
sonar.test.exclusions=src/test/**

# 质量门禁
sonar.qualitygate.wait=true
```

## 文档审查清单

### 技术文档审查
- [ ] 文档标题和描述清晰
- [ ] 代码示例完整可运行
- [ ] 符合全局响应处理器规范
- [ ] 文档行数控制在350行以内
- [ ] 包含必要的错误处理说明
- [ ] 配置参数准确无误

### API文档审查
- [ ] 接口路径和方法正确
- [ ] 请求参数完整定义
- [ ] 响应格式符合全局规范
- [ ] 错误码定义清晰
- [ ] 包含请求示例
- [ ] 安全认证说明完整

### 数据库文档审查
- [ ] 表结构定义完整
- [ ] 字段类型和约束正确
- [ ] 索引设计合理
- [ ] 外键关系清晰
- [ ] 数据迁移脚本可执行
- [ ] 性能优化建议

## 持续改进

### 质量度量
- 每周统计代码质量指标
- 定期分析常见问题和改进点
- 收集开发团队反馈
- 更新质量标准和最佳实践

### 培训和分享
- 定期组织代码审查培训
- 分享最佳实践和经验教训
- 建立知识库和FAQ
- 鼓励团队成员技术分享

### 工具和自动化
- 集成代码质量检查工具
- 自动化测试和部署流程
- 建立质量监控仪表板
- 实现质量问题自动告警

## 质量标准执行

### 1. 开发阶段
- 编码前必须阅读相关规范文档
- 开发过程中遵循代码质量标准
- 提交前执行完整的质量检查
- 确保所有测试用例通过

### 2. 审查阶段
- 严格按照审查清单执行
- 重点检查全局响应处理器使用
- 验证文档质量和准确性
- 确保符合性能和安全要求

### 3. 发布阶段
- 通过所有质量门禁检查
- 完成用户验收测试
- 更新相关文档和版本信息
- 记录发布质量指标

## 注意事项

### 1. 强制性要求
- **严格遵循** 全局响应处理器使用规范
- **每个文档** 必须控制在350行以内
- **核心逻辑** 不超过50行实现
- **优先级**: 规范说明 > 代码示例

### 2. 质量保证
- 必须通过SonarQube代码质量检测
- 保持与现有架构100%兼容
- 确保新成员可在一小时内理解文档内容
- 所有示例代码必须可直接运行

### 3. 持续改进
- 定期审查和更新质量标准
- 收集团队反馈并持续优化
- 关注行业最佳实践和新技术
- 建立质量文化和团队共识

---

**重要提醒**：
1. 质量标准是项目成功的基础保障
2. 每个团队成员都有责任维护代码质量
3. 持续改进是质量管理的核心理念
4. 工具和流程是质量保证的重要手段