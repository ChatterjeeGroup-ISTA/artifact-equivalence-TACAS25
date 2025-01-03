input1=$1
input2=$2
base=$(basename $(dirname $1))
output_dir=$3

# >&2 echo $input1 
# >&2 echo $input2 
# >&2 echo $base
# >&2 echo $work_dir 
# >&2 echo $output_dir

echo "Running PSI on first program..."
./PSI-core/psi --mathematica $input1 > $output_dir/$base-psi-output1.nb
echo "Running PSI on second program..."
./PSI-core/psi --mathematica $input2 > $output_dir/$base-psi-output2.nb

echo "Generating mathematica input..."
python3 process-outputs.py $output_dir/$base-psi-output1.nb $output_dir/$base-psi-output2.nb > $output_dir/$base-merged.nb

echo "Running mathematica..."
math -run < $output_dir/$base-merged.nb > $output_dir/math_output.txt



if grep -q "False" "$output_dir/math_output.txt"; then
    echo "Equivalence Refuted Successfully"
fi
