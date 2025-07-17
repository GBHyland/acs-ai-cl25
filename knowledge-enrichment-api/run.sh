#!/bin/bash

set -a  # automatically export all variables
source .env
set +a
mvn clean package
java -jar target/knowledge-enrichment-api-0.8.0.jar
