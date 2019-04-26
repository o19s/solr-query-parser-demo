#!/usr/bin/env bash

curl 'http://localhost:8983/solr/demo/update?commit=true' \
      -H 'Content-type: application/xml' \
      --data-binary '<delete><query>*:*</query></delete>'