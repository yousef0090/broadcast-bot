FROM wire/bots.runtime:latest

COPY target/broadcast.jar      /opt/broadcast/broadcast.jar
COPY certs/keystore.jks        /opt/broadcast/keystore.jks

WORKDIR /opt/broadcast
EXPOSE  8060
