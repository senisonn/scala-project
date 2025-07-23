# Utiliser une image de base Ubuntu avec Java 17
FROM eclipse-temurin:17-jdk-jammy

# Installer SBT
RUN apt-get update && \
    apt-get install -y curl gpg && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz" | tar xfz - -C /opt && \
    ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers de configuration SBT d'abord (pour le cache)
COPY build.sbt .
COPY project/ ./project/

# Télécharger les dépendances (mise en cache)
RUN sbt update

# Copier le reste du code source
COPY . .

# Exposer le port
EXPOSE 8080

# Commande par défaut
CMD ["sbt", "run"]