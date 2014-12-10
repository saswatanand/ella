#!/bin/bash

ELLA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

#. $ELLA_DIR/ella.settings

java -Xmx2g -classpath $ELLA_DIR/bin/ella.instrument.jar com.apposcopy.ella.Main -ella.settings $ELLA_DIR/ella.settings -ella.runtime $ELLA_DIR/bin/ella.runtime.dex $*