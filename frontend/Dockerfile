# 阶段1: 构建阶段
FROM node:18-alpine AS builder

WORKDIR /app

# 复制package文件
COPY package*.json ./

# 安装依赖
RUN npm ci

# 复制源代码
COPY . .

# 构建生产版本
# 通过 --build-arg 传入 VUE_APP_AMAP_API_KEY
ARG VUE_APP_AMAP_API_KEY
ENV VUE_APP_AMAP_API_KEY=$VUE_APP_AMAP_API_KEY
RUN npm run build

# 阶段2: 运行阶段 - 使用nginx
FROM nginx:alpine

# 复制自定义nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 暴露80端口
EXPOSE 80

# 启动nginx
CMD ["nginx", "-g", "daemon off;"]

