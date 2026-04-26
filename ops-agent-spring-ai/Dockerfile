FROM eclipse-temurin:21-jre

LABEL maintainer="hongwei"

ENV JAVA_OPTS="-Xms256m -Xmx512m -server -XX:+HeapDumpOnOutOfMemoryError"
ENV PARAMS=""
ENV TZ=PRC

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=skywalking-agent . /opt/skywalking/skywalking-agent
ADD ops-agent-spring-ai/target/ops-agent-spring-ai-1.0-SNAPSHOT.jar /ops-agent-spring-ai.jar

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /ops-agent-spring-ai.jar $PARAMS"]
