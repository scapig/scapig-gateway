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

## Publishing
``
docker tag scapig-gateway scapig/scapig-gateway:VERSION
docker login
docker push scapig/scapig-gateway:VERSION
``

## Running
``
docker run -p9018:9018 -d scapig/scapig-gateway:VERSION
``
