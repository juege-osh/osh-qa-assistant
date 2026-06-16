# 前端服务启动指南

## 问题诊断

### 错误信息
```
ERR_CONNECTION_REFUSED
http://localhost:9000/auth/getCode
```

### 根本原因
- ✅ 后端服务正常运行（端口 9000）
- ✅ 后端 API 可以访问（curl 测试返回 200）
- ❌ **前端开发服务器未运行**

浏览器访问的是前端页面，前端页面再去调用后端 API。
如果前端服务器没启动，就无法访问页面，自然也无法调用后端。

---

## 启动前端服务

### 方法 1：命令行启动（推荐）

```bash
# 进入前端目录
cd frontend/ai-assistant-web

# 安装依赖（如果还没安装）
npm install

# 启动开发服务器
npm run dev
```

启动成功后会看到：
```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

### 方法 2：在 IDE 中启动

#### WebStorm / IntelliJ IDEA：
1. 打开 `package.json`
2. 找到 `"dev": "vite"` 这一行
3. 点击左侧的绿色播放按钮
4. 选择 "Run 'dev'"

#### VS Code：
1. 打开终端（Ctrl + `）
2. 运行：
   ```bash
   cd frontend/ai-assistant-web && npm run dev
   ```

---

## 访问应用

启动成功后，在浏览器访问：

```
http://localhost:5173/
```

这会自动重定向到登录页。

---

## 验证服务状态

### 检查前端服务
```bash
lsof -nP -iTCP:5173 -sTCP:LISTEN
```

应该看到：
```
node    xxxxx tianyi   25u  IPv4 ...  TCP *:5173 (LISTEN)
```

### 检查后端服务
```bash
lsof -nP -iTCP:9000 -sTCP:LISTEN
```

应该看到：
```
java    xxxxx tianyi  353u  IPv6 ...  TCP *:9000 (LISTEN)
```

### 测试后端 API
```bash
curl http://localhost:9000/auth/getCode
```

应该返回验证码相关的 JSON 数据。

---

## 完整的服务架构

```
浏览器
  ↓ 访问 http://localhost:5173/
前端开发服务器 (Vite - 端口 5173)
  ↓ 加载页面、静态资源
浏览器运行的 Vue 应用
  ↓ 调用 API: http://localhost:9000/auth/getCode
后端服务 (Spring Boot - 端口 9000)
  ↓ 连接 Redis (43.242.200.25:56379)
Redis 服务器
```

---

## 常见问题

### Q1: 端口 5173 被占用
**错误信息：**
```
Port 5173 is in use, trying another one...
```

**解决方案：**
```bash
# 杀死占用端口的进程
lsof -ti:5173 | xargs kill -9

# 或者使用其他端口
npm run dev -- --port 5174
```

### Q2: npm install 失败
**解决方案：**
```bash
# 清理缓存
npm cache clean --force

# 删除 node_modules 重新安装
rm -rf node_modules package-lock.json
npm install
```

### Q3: 前端可以访问但调用后端失败
**检查项：**
1. 后端是否正常运行？
2. 后端端口是否是 9000？
3. `.env.development` 中的 `VITE_BASE_URL` 是否正确？

---

## 快速启动脚本

创建启动脚本 `start-frontend.sh`：

```bash
#!/bin/bash
cd "$(dirname "$0")/frontend/ai-assistant-web"
echo "正在启动前端开发服务器..."
npm run dev
```

使用：
```bash
chmod +x start-frontend.sh
./start-frontend.sh
```

---

## 下一步

启动前端服务后：
1. 访问 http://localhost:5173/
2. 看到登录页面
3. 验证码应该能正常显示
4. 可以正常登录

如果还有问题，检查浏览器控制台的错误信息。
