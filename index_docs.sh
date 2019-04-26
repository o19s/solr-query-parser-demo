#!/usr/bin/env bash

curl 'http://localhost:8983/solr/demo/update?commit=true' \
      -H 'Content-type: application/json' \
      --data-binary '[
      {
        "id": "0002",
        "title_t": "The Green New Deal legislation from democrats in congress",
        "title_txt": "The Green New Deal legislation from democrats in congress",
        "title_s": "The Green New Deal legislation from democrats in congress",
        "pub_dt": "2019-04-21T14:00:00Z",
        "popularity_i": 5
      }
      ]'
