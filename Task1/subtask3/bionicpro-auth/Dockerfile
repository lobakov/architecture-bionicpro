FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y wget unzip && rm -rf /var/lib/apt/lists/*

ENV GRADLE_VERSION=8.10
ENV GRADLE_HOME=/opt/gradle
ENV PATH=$GRADLE_HOME/bin:$PATH

RUN wget --no-verbose "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip gradle-${GRADLE_VERSION}-bin.zip \
    && mv "gradle-${GRADLE_VERSION}" "${GRADLE_HOME}" \
    && rm gradle-${GRADLE_VERSION}-bin.zip

WORKDIR /build
COPY . /build

RUN gradle build

RUN mkdir -p /app
RUN cp /build/build/libs/*.jar /app/app.jar

WORKDIR /app

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
