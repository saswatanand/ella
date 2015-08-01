#!/bin/bash

ELLA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ "$1" == 'i' ]; then
shift
java -ea -classpath $ELLA_DIR/bin/ella.instrument.jar com.apposcopy.ella.EllaLauncher i $*
exit
fi

if [ "$1" == 'e' ]; then
adb shell am broadcast -a com.apposcopy.ella.COVERAGE --es action \"e\"
exit
fi

#start the server
if [ "$1" == 's' ]; then
java -ea -classpath bin/ella.server.jar com.apposcopy.ella.server.ServerController start
exit
fi

#start the server
if [ "$1" == 'k' ]; then
java -ea -classpath bin/ella.server.jar com.apposcopy.ella.server.ServerController kill
exit
fi

#set up port-forwarding
#do it after emulator is running
if [ "$1" == 'p' ]; then
ant -f $ELLA_DIR/frontend/build.xml set-up-adb-port-forwarding
exit
fi