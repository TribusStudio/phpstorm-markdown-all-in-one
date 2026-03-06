FROM eclipse-temurin:21-jdk-jammy

ARG USERNAME=dev
ARG USER_UID=1000
ARG USER_GID=$USER_UID

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    unzip \
    zip \
    xz-utils \
    libfreetype6 \
    fontconfig \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && mkdir -p /home/$USERNAME/.gradle \
    && chown -R $USERNAME:$USERNAME /home/$USERNAME

# Set environment
ENV JAVA_HOME=/opt/java/openjdk
ENV GRADLE_USER_HOME=/home/$USERNAME/.gradle
ENV PATH="${JAVA_HOME}/bin:${PATH}"

USER $USERNAME
WORKDIR /workspace
