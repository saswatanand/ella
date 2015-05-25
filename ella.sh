#!/bin/bash

ELLA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ "$1" == 'i' ]; then
shift
java -Xmx2g -ea -classpath $ELLA_DIR/bin/ella.instrument.jar com.apposcopy.ella.Main -ella.dir $ELLA_DIR -ella.settings $ELLA_DIR/ella.settings -ella.runtime $ELLA_DIR/bin/ella.runtime.dex -ella.apktool $ELLA_DIR/bin/apktool_2.0.0rc4.jar -ella.exclude $ELLA_DIR/ella-exclude.txt $*
exit
fi

if [ "$1" == 'e' ]; then
adb shell am broadcast -a com.apposcopy.ella.COVERAGE --es action \"e\"
exit
fi

#deploy the web server
if [ "$1" == 'd' ]; then
ant -f $ELLA_DIR/frontend/build.xml deploy
exit
fi

#set up port-forwarding
#do it after emulator is running
if [ "$1" == 'p' ]; then
ant -f $ELLA_DIR/frontend/build.xml set-up-adb-port-forwarding
exit
fi