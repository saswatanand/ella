#!/bin/bash

ELLA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


adb shell am broadcast -a com.apposcopy.ella.COVERAGE --es action \"e\"


echo "The first (and only) parameter to the script must be either b or e."
