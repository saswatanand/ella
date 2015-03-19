#!/bin/bash

ELLA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ "$1" == 'b' ]; then
  echo "" > $ELLA_DIR/ella_record_coverage
  adb push $ELLA_DIR/ella_record_coverage /sdcard/ella_record_coverage
  adb push $ELLA_DIR/ella_url.txt /sdcard/ella_url.txt
  exit
fi

if [ "$1" == "e" ]; then
  adb shell rm /sdcard/ella_record_coverage
  exit
fi

echo "The first (and only) parameter to the script must be either b or e."
