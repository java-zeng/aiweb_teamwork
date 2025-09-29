# AiWeb 后端API对接文档

## 概述
本文档描述了AiWeb后端系统的所有API接口，包括请求参数、响应格式和错误处理。

## 基础信息
- **基础URL**: `http://localhost:8890/api/auth`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

## 统一响应格式

所有API接口都使用统一的响应格式：

```json
{
  "code": 1,           // 响应码：1表示成功，0表示失败
  "msg": "success",    // 响应信息
  "data": {}           // 返回的数据，成功时包含具体数据，失败时为null
}
```

## API接口列表

### 1. 发送验证码
**接口地址**: `POST /api/auth/sendCode`

**功能描述**: 用户注册前发送邮箱验证码

**请求参数**:
```json
{
  "username": "string",    // 用户名，必填
  "email": "string"        // 邮箱地址，必填
}
```

**响应示例**:
```json
// 成功
{
  "code": 1,
  "msg": "success",
  "data": "发送验证码成功!"
}

// 失败
{
  "code": 0,
  "msg": "用户名重复或发送邮件失败！,原因如下:具体错误信息",
  "data": null
}
```

**错误情况**:
- 用户名已存在
- 邮箱格式不正确
- 邮件发送失败

---

### 2. 用户注册
**接口地址**: `POST /api/auth/register`

**功能描述**: 用户注册新账号

**请求参数**:
```json
{
  "username": "string",        // 用户名，必填
  "nickname": "string",        // 昵称，必填
  "password": "string",        // 密码，必填
  "phoneNumber": "string",     // 手机号，必填
  "inputCode": "string",       // 用户输入的验证码，必填
  "email": "string"            // 邮箱地址，必填
}
```

**响应示例**:
```json
// 成功
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "password": "加密后的密码",
    "imageUrl": null,
    "status": 1
  }
}

// 失败
{
  "code": 0,
  "msg": "具体错误信息",
  "data": null
}
```

**错误情况**:
- 验证码错误或过期
- 用户名已存在
- 邮箱已存在
- 必填字段缺失

---

### 3. 用户登录
**接口地址**: `POST /api/auth/login`

**功能描述**: 用户登录获取JWT令牌

**请求参数**:
```json
{
  "username": "string",    // 用户名，必填
  "password": "string"     // 密码，必填
}
```

**响应示例**:
```json
// 成功
{
  "code": 1,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}

// 失败
{
  "code": 0,
  "msg": "用户名或者密码错误！",
  "data": null
}
```

**错误情况**:
- 用户名不存在
- 密码错误
- 账号被禁用

**注意事项**:
- 登录成功后，前端需要将返回的token保存到localStorage或sessionStorage
- 后续请求需要在请求头中携带token: `Authorization: Bearer {token}`

---

### 4. 忘记密码
**接口地址**: `POST /api/auth/forgot_password`

**功能描述**: 发送密码重置链接到用户邮箱

**请求参数**:
```json
{
  "userEmail": "string"    // 用户邮箱，必填
}
```

**响应示例**:
```json
// 成功
{
  "code": 1,
  "msg": "success",
  "data": "重置链接已经发送到你的邮箱"
}

// 失败
{
  "code": 0,
  "msg": "具体错误信息",
  "data": null
}
```

**错误情况**:
- 邮箱不存在
- 邮箱格式不正确
- 邮件发送失败

---

### 5. 重置密码
**接口地址**: `POST /api/auth/reset_password`

**功能描述**: 通过重置链接重置用户密码

**请求参数**:
```json
{
  "token": "string",           // 重置令牌，必填
  "userEmail": "string",       // 用户邮箱，必填
  "newPassword": "string"      // 新密码，必填
}
```

**响应示例**:
```json
// 成功
{
  "code": 1,
  "msg": "success",
  "data": "重置密码成功，请重新登陆！"
}

// 失败
{
  "code": 0,
  "msg": "具体错误信息",
  "data": null
}
```

**错误情况**:
- 重置令牌无效或过期
- 邮箱与令牌不匹配
- 新密码格式不符合要求

## 数据模型说明

### UserDto (用户信息)
```json
{
  "id": "number",           // 用户ID
  "username": "string",     // 用户名
  "nickname": "string",     // 昵称
  "password": "string",     // 密码（加密后）
  "imageUrl": "string",     // 头像URL
  "status": "number"        // 状态：1-正常，0-禁用
}
```

### AuthResponse (认证响应)
```json
{
  "token": "string"         // JWT令牌
}
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 1 | 成功 |
| 0 | 失败 |

## 前端集成建议

### 1. 请求拦截器
建议在axios或其他HTTP客户端中配置请求拦截器，自动添加JWT令牌：

```javascript
// 请求拦截器示例
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### 2. 响应拦截器
建议配置响应拦截器处理统一错误：

```javascript
// 响应拦截器示例
axios.interceptors.response.use(
  response => {
    if (response.data.code === 0) {
      // 处理业务错误
      console.error(response.data.msg);
    }
    return response;
  },
  error => {
    // 处理网络错误
    console.error('网络错误:', error);
    return Promise.reject(error);
  }
);
```

### 3. 登录状态管理
建议使用状态管理工具（如Vuex、Redux等）管理用户登录状态：

```javascript
// 登录状态管理示例
const authStore = {
  state: {
    isLoggedIn: false,
    user: null,
    token: null
  },
  mutations: {
    SET_LOGIN(state, { user, token }) {
      state.isLoggedIn = true;
      state.user = user;
      state.token = token;
      localStorage.setItem('token', token);
    },
    LOGOUT(state) {
      state.isLoggedIn = false;
      state.user = null;
      state.token = null;
      localStorage.removeItem('token');
    }
  }
};
```

## 安全注意事项

1. **JWT令牌安全**：
   - 令牌应存储在安全的地方（localStorage或httpOnly cookie）
   - 定期刷新令牌
   - 退出登录时清除令牌

2. **密码安全**：
   - 前端应对密码进行基本验证（长度、复杂度）
   - 使用HTTPS传输敏感数据

3. **输入验证**：
   - 前端应进行输入验证，减少无效请求
   - 对用户输入进行适当的转义处理

## 测试建议

1. **单元测试**：为每个API接口编写单元测试
2. **集成测试**：测试完整的用户流程（注册→登录→重置密码）
3. **边界测试**：测试各种异常情况
4. **性能测试**：测试高并发情况下的API性能

## 更新日志

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0.0 | 2024-01-XX | 初始版本，包含基础认证功能 |

## 联系方式

如有问题或建议，请联系开发团队。
