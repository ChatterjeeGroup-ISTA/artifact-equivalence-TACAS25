#!/bin/bash

folder=$1
work='work/'
output='outputs-psi/'
input1=$folder/input-1.psi
input2=$folder/input-2.psi

mkdir -p outputs/$folder
mkdir -p work

cd psi

{ time ./equivalence-refutation.sh ../$input1 ../$input2 ../$output/$folder > ../$output/$folder/psi_final_output.psi; } 2> "../$output/$folder/psi_time.txt"
