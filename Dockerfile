# syntax=docker/dockerfile:1.4.0
ARG username=worker
ARG work_dir=/home/$username/work
ARG gid=1000
ARG uid=1001
ARG gradle_cache_dir=/home/$username/.gradle/caches

# Copy across all the *.gradle.kts files in a separate stage
# This will not get any layer caching if anything in the context has changed, but when we
# subsequently copy them into a different stage that stage *will* get layer caching. So if none of
# the *.gradle.kts files have changed, a subsequent command will also get layer caching.
FROM alpine as gradle-files
RUN --mount=type=bind,target=/docker-context \
    mkdir -p /gradle-files/gradle && \
    cd /docker-context/ && \
    find . -name "*.gradle" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "*.gradle.kts" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "libs.versions.toml" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "*module-info.java" -exec cp --parents "{}" /gradle-files/ \;


FROM eclipse-temurin:19.0.2_7-jdk-focal as base_builder

ARG username
ARG work_dir
ARG gid
ARG uid
ARG gradle_cache_dir

RUN addgroup --system $username --gid $gid && \
    adduser --system $username --ingroup $username --uid $uid

USER $username
RUN mkdir -p $work_dir
WORKDIR $work_dir

# Download gradle in a separate step to benefit from layer caching
COPY --link --chown=$username gradle/wrapper gradle/wrapper
COPY --link --chown=$username gradlew gradlew
COPY --link --chown=$username gradle.properties gradle.properties
RUN ./gradlew --version

ENV GRADLE_OPTS="\
-Dorg.gradle.daemon=false \
-Dorg.gradle.logging.stacktrace=all \
-Dorg.gradle.vfs.watch=false \
-Dorg.gradle.console=plain \
"

# Do all the downloading in one step...
COPY --link --chown=$username --from=gradle-files /gradle-files ./
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    ./gradlew downloadDependencies

COPY --link --chown=$username . .


FROM base_builder as checker
ARG gradle_cache_dir
ARG gid
ARG uid

# So the actual build can run without network access. Proves no tests rely on external services.
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    --network=none \
    ./gradlew --offline check || mkdir -p build


FROM scratch as build-output
ARG work_dir

COPY --link --from=checker $work_dir/build .

# The builder step is guaranteed not to fail, so that the worker output can be tagged and its
# contents (build reports) extracted.
# You run this as:
# `docker build . --target build-output --output build-output && docker build .`
# to retrieve the build reports whether or not the previous line exited successfully.
# Workaround for https://github.com/moby/buildkit/issues/1421
FROM checker as builder
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    --network=none \
    ./gradlew --offline build


FROM scratch as jars
ARG work_dir

COPY --link --from=builder $work_dir/build/libs .
