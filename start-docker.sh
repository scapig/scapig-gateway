#!/bin/sh
SCRIPT=$(find . -type f -name tapi-gateway)
exec $SCRIPT -Dhttp.port=8030
