index=1
TO=600

mkdir -p work

while read p; do
    b=($p)
    folder=${b[0]}
    degree=${b[1]}
    eq_ds=${b[2]}
    sting=${b[3]}
    aspic=${b[4]}

    if [[ $sting == "sting" ]]; then
        sting_b=1
    else
        sting_b=0
    fi

    if [[ $aspic == "aspic" ]]; then
        aspic_b=1
    else
        aspic_b=0
    fi
    output_dir=outputs-$eq_ds/$folder/output-$sting_b$aspic_b-$degree.txt
    log_dir=outputs-$eq_ds/$folder/log-$sting_b$aspic_b-$degree.txt
    mkdir -p outputs-$eq_ds/$folder
    echo "$index. running on $folder $degree $eq_ds"
    time timeout $TO ./run.sh $folder/input-1.t2 $folder/input-2.t2 $degree $sting $aspic $eq_ds > $output_dir 2> $log_dir
    if grep -q "Successfully refuted equivalence!" "$output_dir"; then
        echo "SUCCESS"
    fi

    if grep -q "Succesfully refuted similarity" "$output_dir"; then
        echo "SUCCESS"
    fi
    
    index=$((index+1))
done <best_configs.csv