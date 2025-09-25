FROM openjdk:17-jdk-slim
ENV PORT 8084
EXPOSE 8084
#COPY target/*.jar /opt/springboot-docker-demo.jar
ADD target/*.jar app.jar
RUN apt install && \
    apt update && \
    apt install -y ffmpeg &&\
    apt-get clean
ENTRYPOINT ["java", "-jar", "app.jar"]