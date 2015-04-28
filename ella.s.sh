#!/bin/bash

ELLA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

ant -f $ELLA_DIR/frontend/build.xml set-up-adb-port-forwarding
