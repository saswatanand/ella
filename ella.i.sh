#!/bin/bash

ELLA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

java -Xmx2g -ea -classpath $ELLA_DIR/bin/ella.instrument.jar com.apposcopy.ella.Main -ella.dir $ELLA_DIR -ella.settings $ELLA_DIR/ella.settings -ella.runtime $ELLA_DIR/bin/ella.runtime.dex -ella.apktool $ELLA_DIR/bin/apktool-cli-1.5.3-SNAPSHOT.jar $*