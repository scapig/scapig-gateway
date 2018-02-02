#!/bin/sh
sbt universal:package-zip-tarball
docker build -t scapig-gateway .
docker tag scapig-gateway scapig/scapig-gateway
docker push scapig/scapig-gateway
