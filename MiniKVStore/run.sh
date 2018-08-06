#!/bin/bash
mvn compile exec:java -Dexec.args="$1 $2"
