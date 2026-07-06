# Etapa 1: build con Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: runtime liviano
FROM eclipse-temurin:21-jdk
WORKDIR /app
# El backend opera en UTC de forma explicita e independiente del host.
ENV TZ=UTC

# ─── Whisper local (actas por voz) ───────────────────────────────
# python3 + ffmpeg (decodifica m4a/aac) + faster-whisper (Whisper CPU int8).
# El modelo se descarga en el primer uso y queda cacheado en /root/.cache.
RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip ffmpeg && \
    (pip3 install --no-cache-dir faster-whisper || \
     pip3 install --no-cache-dir --break-system-packages faster-whisper) && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

COPY scripts/whisper_transcribe.py /app/scripts/whisper_transcribe.py
RUN mkdir -p /app/data/actas-audio

COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-Duser.timezone=UTC", "-jar", "app.jar", "--server.port=8080"]