#!/usr/bin/env bash

LOGGING_LEVEL=$1

echo "Set Solr UI Logging to $LOGGING_LEVEL"

curl -X GET -G 'http://localhost:8983/solr/admin/info/logging?since=0' -d threshold=$LOGGING_LEVEL