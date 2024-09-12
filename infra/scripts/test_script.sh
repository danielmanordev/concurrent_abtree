#!/bin/bash
for i in {1..40}
do
   echo "Starting test #${i}"

   fileName=${i}_threads_contains_only_1000000
   java MppRunner "$i" > $fileName
done
