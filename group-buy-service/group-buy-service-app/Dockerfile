# 基础镜像
FROM eclipse-temurin:21-jre

# 作者
MAINTAINER hongwei

# 配置
ENV JAVA_OPTS="-Xms1G -Xmx1G -server -XX:+HeapDumpOnOutOfMemoryError"
ENV PARAMS=""

# 时区
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 添加应用
ADD target/group-buy-service-app.jar /group-buy-service-app.jar

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /group-buy-service-app.jar $PARAMS"]
