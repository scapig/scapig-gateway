#!/bin/sh
SCRIPT=$(find . -type f -name scapig-gateway)
rm -f scapig-gateway*/RUNNING_PID
exec $SCRIPT -Dhttp.port=9018 $JAVA_OPTS -J-Xms16M -J-Xmx64m
