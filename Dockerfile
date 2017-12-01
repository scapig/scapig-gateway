FROM openjdk:8

COPY target/universal/tapi-gateway-*.tgz .
COPY start-docker.sh .
RUN chmod +x start-docker.sh
RUN tar xvf tapi-gateway-*.tgz

EXPOSE 8030