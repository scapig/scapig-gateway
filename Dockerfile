FROM openjdk:8

COPY target/universal/scapig-gateway-*.tgz .
COPY start-docker.sh .
RUN chmod +x start-docker.sh
RUN tar xvf scapig-gateway-*.tgz
EXPOSE 8030

CMD ["sh", "start-docker.sh"]