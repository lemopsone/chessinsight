#!/usr/bin/env bash

set -euo pipefail

CORE_DIR=core/target/site/allure-maven-plugin
JPA_DIR=jpa/target/site/allure-maven-plugin

mvn -pl core,jpa -am allure:report

allure open $CORE_DIR &
allure open $JPA_DIR