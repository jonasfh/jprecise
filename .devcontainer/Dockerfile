FROM mcr.microsoft.com/devcontainers/java:1-21

# Allow matching the host UID/GID so files created inside the container
# match host permissions when the workspace is mounted.
ARG USER_UID=1000
ARG USER_GID=1000

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/emulator

# ---------- Stable system packages (rarely change) ----------
# Remove any problematic third-party apt sources from the base image (e.g., yarn)
RUN rm -f /etc/apt/sources.list.d/yarn.list* || true
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    wget \
    unzip \
    git \
    zip \
    ca-certificates \
    build-essential \
    procps \
    && rm -rf /var/lib/apt/lists/*

# ---------- Android command-line tools (expensive, stable) ----------
RUN mkdir -p ${ANDROID_SDK_ROOT} /tmp && cd /tmp \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip \
    && unzip -q cmdline-tools.zip -d ${ANDROID_SDK_ROOT} \
    && rm cmdline-tools.zip \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools-temp \
    && mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools-temp/* ${ANDROID_SDK_ROOT}/cmdline-tools/latest/ \
    && yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --licenses || true \
    && ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --install "platform-tools" "platforms;android-36" "build-tools;36.0.0" "build-tools;34.0.0" "cmdline-tools;latest" \
    && rm -rf /tmp/*

# ---------- Install Antigravity cli tool ----------
RUN curl -fsSL https://antigravity.google/cli/install.sh | bash -s -- --dir /usr/local/bin

# Make SDK directory writable so the non-root user (matching host UID) can use Gradle and SDK tools
RUN chmod -R a+rw ${ANDROID_SDK_ROOT}

# ---------- Finalize user and working dir (late, so source edits don't bust earlier layers) ----------
# Remove existing user with this UID if it exists (e.g. from base image)
RUN userdel -r $(getent passwd ${USER_UID} | cut -d: -f1) 2>/dev/null || true

# Create group and user
RUN groupadd -g ${USER_GID} dev 2>/dev/null || true \
    && useradd -m -u ${USER_UID} -g ${USER_GID} -s /bin/bash dev 2>/dev/null || true \
    && mkdir -p /workspace /home/dev/.gemini && chown -R ${USER_UID}:${USER_GID} /workspace /home/dev ${ANDROID_SDK_ROOT}

USER dev
WORKDIR /workspace
