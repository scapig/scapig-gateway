## scapig-gateway

## Building
``
sbt clean test it:test component:test
``

## Packaging
``
sbt universal:package-zip-tarball
docker build -t scapig-gateway .
``

## Running
``
docker run -p8030:8030 -i -a stdin -a stdout -a stderr scapig-gateway sh start-docker.sh
``