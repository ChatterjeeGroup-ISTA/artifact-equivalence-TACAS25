#!/bin/bash

mkdir -p work

TO=300

for folder in inputs-*/*/; do
    echo "------------------$folder----------------"
    if [ -d "psi" ]; then
        echo "===psi started==="
        timeout $TO ./run_psi.sh $folder
        echo "===psi ended==="
    fi
done
