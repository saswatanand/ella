#!/bin/bash

ELLA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ "$1" == 'b' ]; then
  ant -f $ELLA_DIR/frontend/build.xml set-up-adb-port-forwarding
  adb shell am broadcast -a com.apposcopy.ella.COVERAGE --es action \"b\" --es url \"`cat ella_url.txt`\"
  exit
fi

if [ "$1" == "e" ]; then
  adb shell am broadcast -a com.apposcopy.ella.COVERAGE --es action \"e\"
  exit
fi

echo "The first (and only) parameter to the script must be either b or e."
