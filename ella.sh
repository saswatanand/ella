#!/bin/bash

ELLA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

. $ELLA_DIR/ella.settings

java -Xmx2g -classpath bin/ella.instrument.jar:${dxjar} com.apposcopy.ella.Main -ella.runtime bin/ella.runtime.dex $*