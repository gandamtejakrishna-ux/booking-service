# 1. STAGE: BUILD WITH SBT
FROM hseeberger/scala-sbt:11.0.13_1.6.1_2.13.7 AS build

# Set working directory
WORKDIR /app

# Copy entire project
COPY . /app

# Compile & package using Play's stage task
RUN sbt clean compile stage


# 2. STAGE: RUNTIME (LIGHTWEIGHT)
FROM openjdk:11-jre-slim

# Set working directory
WORKDIR /app

# Copy the packaged distribution from build stage
COPY --from=build /app/target/universal/stage /app

# Expose Play port (change if needed)
EXPOSE 9002

# Environment variable for Play secret
ENV PLAY_HTTP_SECRET="thisisasecretfortheapplicationandwekeepittosecuretheapplication"

# Final command to start Play app
CMD ["./bin/scala-play", "-Dplay.http.secret.key=${PLAY_HTTP_SECRET}"]
