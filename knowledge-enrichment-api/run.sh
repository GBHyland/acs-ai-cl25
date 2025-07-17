#!/bin/bash

set -a  # automatically export all variables
source .env
set +a
java -jar target/knowledge-enrichment-api-0.8.0.jar
