for x in inputs-*/*; do
    echo "========== running equivalence $x started =========="
    mkdir -p outputs-equivalence/$x
    for((i=1;i<=5;i++)) do
        echo "========== degree: $i ----- nosting =========="
        timeout 300 ./run.sh $x/input-1.t2 $x/input-2.t2 $i nosting aspic equivalence > outputs-equivalence/$x/output-01-$i.txt 2> outputs-equivalence/$x/log-01-$i.txt
        
        if grep -qlr "Successfully" outputs-equivalence/$x/*; then 
            break
        fi 
        
        echo "========== degree: $i ----- sting =========="
        timeout 300 ./run.sh $x/input-1.t2 $x/input-2.t2 $i sting aspic equivalence > outputs-equivalence/$x/output-11-$i.txt 2> outputs-equivalence/$x/log-11-$i.txt
        
        if grep -qlr "Successfully" outputs-equivalence/$x/*; then 
            break
        fi 
    done
    echo "========== running equivalence $x ended =========="
done 