for x in inputs-*/*; do
    echo "========== running distance $x started =========="
    mkdir -p outputs-distance/$x
    for((i=1;i<=5;i++)) do
        echo "========== degree: $i ----- nosting =========="
        timeout 300 ./run.sh $x/input-1.t2 $x/input-2.t2 $i nosting aspic distance > outputs-distance/$x/output-01-$i.txt 2> outputs-distance/$x/log-01-$i.txt
        
        if grep -qlr "Successfully" outputs-distance/$x/*; then 
            break
        fi 
        
        timeout 300 ./run.sh $x/input-1.t2 $x/input-2.t2 $i sting aspic distance > outputs-distance/$x/output-11-$i.txt 2> outputs-distance/$x/log-11-$i.txt
        
        if grep -qlr "Successfully" outputs-distance/$x/*; then 
            break
        fi 
    done
    echo "========== running distance $x ended =========="
done 