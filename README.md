## tapi-gateway

## Building
``
sbt clean test it:test component:test
``

## Packaging
``
sbt universal:package-zip-tarball
docker build -t tapi-gateway .
``

## Running
``
docker run -p8030:8030 -i -a stdin -a stdout -a stderr tapi-gateway sh start-docker.sh
``