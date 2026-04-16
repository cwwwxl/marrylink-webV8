# MarryLink App (H5) Dockerfile
# 直接部署本地构建好的 dist 产物

# 使用国内镜像加速的 nginx
FROM nginx:1.27-alpine

LABEL maintainer="MarryLink Team"
LABEL description="MarryLink Mobile App (H5 Version)"

# 删除默认配置
RUN rm -rf /etc/nginx/conf.d/default.conf

# 复制自定义 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/marrylink-app.conf

# 复制 H5 构建产物（构建前请先将 h5 目录复制到此目录）
# cp -r /root/h5 /path/to/marrylink-app/
COPY h5 /usr/share/nginx/html

# 设置文件权限
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

# 启动 nginx
CMD ["nginx", "-g", "daemon off;"]