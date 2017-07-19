#!/bin/sh

export TERM=xterm-color

for dir in *-example; do
  if (cd $dir; sbt $@); then
    echo "Test(s) succeeded in $dir"
  else
    echo "Test(s) failed in $dir"
    exit 1
  fi
done
