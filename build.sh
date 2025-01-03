#!/bin/bash

chmod +x solvers/aspicV3.4
chmod +x solvers/lsting
chmod +x best_configs.sh
mkdir -p work

rsync -av -f"+ */" -f"- *" "inputs-inference" "outputs-equivalence"
rsync -av -f"+ */" -f"- *" "inputs-pldi" "outputs-equivalence"
rsync -av -f"+ */" -f"- *" "inputs-psi" "outputs-equivalence"
cp -r outputs-equivalence outputs-inference
cp -r outputs-equivalence outputs-psi



echo "------------------Building the artifact------------------"
cd src
javac -cp ../gurobi/linux64/lib/gurobi.jar:. Main.java

cd ..
jar cfm artifact.jar src/META-INF/MANIFEST.MF src/*.class
echo "------------------Artifact Build finished successfully------------------"


if [ -d "psi" ]; then
    echo "------------------Fetching & Building PSI------------------"
    cd psi 
    git clone https://github.com/eth-sri/psi.git
    mv psi PSI-core
    cd PSI-core
    ./dependencies-release.sh && ./build-release.sh
    echo "------------------PSI Built Successfully------------------"
fi