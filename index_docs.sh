#!/usr/bin/env bash

curl 'http://localhost:8983/solr/demo/update?commit=true' \
      -H 'Content-type: application/json' \
      --data-binary '[{
        "id": "0001",
        "title_txt": "Will Donald Trump be impeached?",
        "title_s": "Will Donald Trump be impeached?",
        "pub_dt": "2019-03-01T13:00:00Z",
        "popularity_i": 10
      }]'
