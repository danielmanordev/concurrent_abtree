#!/bin/bash
for i in {1..16}
do
   let numOfNonScanThreads=40-$i
   fileName=${numOfNonScanThreads}_${i}_80_20_1000000
   java MppRunner $i > $fileName
done
