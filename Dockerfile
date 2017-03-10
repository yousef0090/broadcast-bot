FROM wire/bots.runtime:latest

COPY target/broadcast.jar      /opt/broadcast/broadcast.jar
COPY broadcast.yaml            /opt/broadcast/broadcast.yaml
COPY certs/keystore.jks        /opt/broadcast/keystore.jks

WORKDIR /opt/broadcast
EXPOSE  443
