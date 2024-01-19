#!/usr/bin/bash

kafka-topics --bootstrap-server 172.30.162.50:9092 --topic mlp-demo --create --partitions 3 --replication-factor 1