# syntax=docker/dockerfile:1.6.0
ARG username=worker
ARG work_dir=/home/$username/work

# Copy across all the *.gradle.kts files in a separate stage
# This will not get any layer caching if anything in the context has changed, but when we
# subsequently copy them into a different stage that stage *will* get layer caching. So if none of
# the *.gradle.kts files have changed, a subsequent command will also get layer caching.
FROM --platform=$BUILDPLATFORM alpine as gradle-files
RUN --mount=type=bind,target=/docker-context \
    mkdir -p /gradle-files/gradle && \
    cd /docker-context/ && \
    find . -name "*.gradle" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "*.gradle.kts" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "libs.versions.toml" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name ".editorconfig" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "gradle.properties" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "*module-info.java" -exec cp --parents "{}" /gradle-files/ \;


FROM --platform=$BUILDPLATFORM alpine as base_builder

RUN apk add --no-cache \
    bash \
    curl \
    zip

SHELL ["/bin/bash", "-c"]

ARG username
ARG work_dir
ARG gid=1000
ARG uid=1001

RUN addgroup --system $username --gid $gid && \
    adduser --system $username --ingroup $username --uid $uid

USER $username
RUN mkdir -p $work_dir
WORKDIR $work_dir

RUN curl -s "https://get.sdkman.io" | bash

COPY --link .sdkmanrc .
RUN source /home/$username/.sdkman/bin/sdkman-init.sh && sdk env install && sdk env
ENV PATH=/home/$username/.sdkman/candidates/java/current/bin:$PATH

# Download gradle in a separate step to benefit from layer caching
COPY --link --chown=$username gradle/wrapper gradle/wrapper
COPY --link --chown=$username gradlew gradlew

RUN ./gradlew --version

ARG gradle_cache_dir=/home/$username/.gradle/caches

RUN mkdir -p $gradle_cache_dir/8.5

ENV GRADLE_OPTS="\
-Dorg.gradle.daemon=false \
-Dorg.gradle.logging.stacktrace=all \
-Dorg.gradle.vfs.watch=false \
-Dorg.gradle.console=plain \
"

# Build the configuration cache & download all deps in a single layer
COPY --link --chown=$username --from=gradle-files /gradle-files ./
RUN  --mount=type=cache,gid=$gid,uid=$uid,target=$work_dir/.gradle \
     --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/8.5/generated-gradle-jars \
     --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/8.5/kotlin-dsl \
     --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/8.5/scripts \
     --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/modules-2 \
     --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/transforms-3 \
     --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/jars-9 \
     --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/build-cache-1 \
     ./gradlew build --dry-run

COPY --link --chown=$username . .

# So the tests can run without network access. Proves no tests rely on external services.
RUN --mount=type=cache,gid=$gid,uid=$uid,target=$work_dir/.gradle \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/8.5/generated-gradle-jars \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/8.5/kotlin-dsl \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/8.5/scripts \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/modules-2 \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/transforms-3 \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/jars-9 \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/build-cache-1 \
    --network=none \
    ./gradlew --offline build || (mkdir -p build && touch build/failed)


FROM --platform=$BUILDPLATFORM scratch as build-output
ARG work_dir

COPY --link --from=base_builder $work_dir/build .

# The builder step is guaranteed not to fail, so that the build output can be extracted.
# You run this as:
# `docker build . --target build-output --output build && docker build .`
# to retrieve the build reports whether or not the previous line exited successfully.
# Workaround for https://github.com/moby/buildkit/issues/1421
FROM --platform=$BUILDPLATFORM base_builder as builder
RUN --mount=type=cache,gid=$gid,uid=$uid,target=$work_dir/.gradle \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/8.5/generated-gradle-jars \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/8.5/kotlin-dsl \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/8.5/scripts \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/modules-2 \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/transforms-3 \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/jars-9 \
    --mount=type=cache,gid=$gid,uid=$uid,target=$gradle_cache_dir/build-cache-1 \
    if [ -f build/failed ]; then ./gradlew --offline build; fi
