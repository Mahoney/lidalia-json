# syntax=docker/dockerfile:1.4.0
ARG username=worker
ARG work_dir=/home/$username/work
ARG gid=1000
ARG uid=1001
ARG gradle_cache_dir=/home/$username/.gradle/caches

FROM eclipse-temurin:17.0.1_12-jdk-focal as base_builder

ARG username
ARG work_dir
ARG gid
ARG uid
ARG gradle_cache_dir

RUN addgroup --system $username --gid $gid && \
    adduser --system $username --ingroup $username --uid $uid

USER $username
RUN mkdir -p $work_dir
RUN mkdir -p /home/$username/.gradle/caches
WORKDIR $work_dir

# Download gradle in a separate step to benefit from layer caching
COPY --link --chown=$username gradle/wrapper gradle/wrapper
COPY --link --chown=$username gradlew gradlew
COPY --link --chown=$username gradle.properties gradle.properties

RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    ./gradlew --version

ENV GRADLE_OPTS="\
-Dorg.gradle.daemon=false \
-Dorg.gradle.logging.level=info \
-Dorg.gradle.logging.stacktrace=all \
-Dorg.gradle.vfs.watch=false \
-Dorg.gradle.console=plain \
"

COPY --link --chown=$username . .

# Do check with network to resolve all dependencies
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    ./gradlew check -x test

# So the tests can run without network access. Proves no tests rely on external services.
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    --network=none \
    ./gradlew --offline check || mkdir -p build


FROM scratch as build-output
ARG work_dir

COPY --link --from=base_builder $work_dir/build .

# The builder step is guaranteed not to fail, so that the worker output can be tagged and its
# contents (build reports) extracted.
# You run this as:
# `docker build . --target build-output --output build-output && docker build .`
# to retrieve the build reports whether or not the previous line exited successfully.
# Workaround for https://github.com/moby/buildkit/issues/1421
FROM base_builder as builder
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    --network=none \
    ./gradlew --offline build


FROM scratch as jars
ARG work_dir

COPY --link --from=builder $work_dir/build/libs .
