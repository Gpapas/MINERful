#!/bin/bash

# Author:       Claudio Di Ciccio
# Date:         2013/09/20
# Description:  This script launches the MinerFulMinerStarter, in order to discover the declarative process model out of a log, provided as input.
#               Run this launcher with "-h" to understand the meaning of options you can pass.

## Exec-specific parametres
DEBUGLEVEL="none"
MEMORY_MAX="16G"
THRESHOLD=1.0

## Runtime environment constants
MAINCLASS="minerful.MinerFulMinerStarter"

## Preliminary checks
if [ ! -f ./libs.cfg ]
then
 echo "Please download the file named libs.cfg from the GitHub repository"
 exit 1
fi

## Import the libraries and store it into $LIBS
. ./libs.cfg

## Clean up the screen
clear

## Run!
java -Xmx$MEMORY_MAX -classpath $LIBS $MAINCLASS $* # -d $DEBUGLEVEL -t $THRESHOLD
