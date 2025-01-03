import os.path


benchmarks=["inputs-psi/bertrand",
"inputs-psi/burglarAlarm",
"inputs-psi/coinBiasSmall",
"inputs-psi/coinPattern",
"inputs-psi/coins",
"inputs-psi/ev-model1",
"inputs-psi/ev-model2",
"inputs-psi/gossip",
"inputs-psi/grass",
"inputs-psi/murderMystery",
"inputs-psi/noisyOr",
"inputs-psi/twoCoins",
"inputs-inference/add-uni",
"inputs-inference/cav-ex-5",
"inputs-inference/cav-ex-7",
"inputs-inference/pdmb-v3",
"inputs-inference/race",
"inputs-inference/rdwalk-v12",
"inputs-inference/rdwalk-v13",
"inputs-inference/rdwalk-v14",
"inputs-inference/rdwalk-v23",
"inputs-inference/rdwalk-v24",
"inputs-inference/rdwalk-v34",
"inputs-pldi/queuing",
"inputs-pldi/simple-example",
"inputs-pldi/nested-loop",
"inputs-pldi/random-walk",
"inputs-pldi/goods-discount",
"inputs-pldi/pollutant-disposal",
"inputs-pldi/2d-robot",
"inputs-pldi/bitcoin-mining",
"inputs-pldi/bitcoint-mining-pool",
"inputs-pldi/species-fight",
"inputs-pldi/coupon1",
"inputs-pldi/coupon4",
"inputs-pldi/random_walk_1d_intvalued",
"inputs-pldi/random_walk_1d_realvalued",
"inputs-pldi/random_walk_1d_adversary",
"inputs-pldi/random_walk_2d_demonic",
"inputs-pldi/random_walk_2d_variant",
"inputs-pldi/retransmission"]

def get_psi_result(loc):
    if not os.path.isfile(loc+"/psi_time.txt"):
        return (False,0)
    p_time = 0 
    with open(loc+"/psi_time.txt",'r') as fr:
        lines = fr.readlines()
        for line in lines:
            if "real\t" in line:
                T = line.split("\t")[1][:-2]
                (M,S)=T.split('m')
                p_time=int(M)*60+float(S)
    succeed = False
    with open(loc+"/psi_final_output.psi",'r') as fr:
        lines = fr.readlines()
        for line in lines:
            if "Equivalence Refuted Successfully" in line:
                succeed=True
    return (succeed,p_time)


def get_artifact_eq_result(loc):
    for deg in range(1,4):
        for sting in range(0,2):
            for aspic in range(0,2):
                if not os.path.isfile(f"{loc}/output-{sting}{aspic}-{deg}.txt"):
                    continue
                a_time = 0
                succeed = False 
                with open(f"{loc}/output-{sting}{aspic}-{deg}.txt",'r') as fr:
                    lines = fr.readlines()
                    for line in lines: 
                        if "Successfully refuted equivalence" in line:
                            succeed = True 
                        if "total time used:" in line: 
                            a_time = int(line.split(' ')[-1][:-1])
                if succeed:
                    return (succeed, float(a_time)/1000)
    return (False,0)

def get_artifact_ds_result(loc):
    for deg in range(1,4):
        for sting in range(0,2):
            for aspic in range(1,2):
                if not os.path.isfile(f"{loc}/output-{sting}{aspic}-{deg}.txt"):
                    continue
                a_time = 0
                succeed = False 
                distance = "0"
                with open(f"{loc}/output-{sting}{aspic}-{deg}.txt",'r') as fr:
                    lines = fr.readlines()
                    for line in lines: 
                        if "Successfully refuted equivalence" in line:
                            succeed = True 
                        if "Distance found" in line:
                            distance = float(line.split(' ')[-1][:-1])
                        if "total time used:" in line: 
                            a_time = int(line.split(' ')[-1][:-1])
                    if succeed:
                        return (succeed,distance, float(a_time)/1000)
    return (False,0,0)






# dir = "outputs/inputs/Table-1/"
with open("table-1-results.csv",'w') as fw:
    fw.write("benchmark, our eq result, our eq time, our ds result, our ds time, PSI result, PSI time\n")
    for bench  in benchmarks:
        # fw.write(bench)
        (psi_result, psi_time)=get_psi_result("outputs-psi/"+bench)
        # print("outputs-equivalence/"+bench)
        (artifact_eq_result, artifact_eq_time)=get_artifact_eq_result("outputs-equivalence/"+bench)
        (artifact_ds_result, artifact_ds, artifact_ds_time) = get_artifact_ds_result("outputs-distance/"+bench)
        if artifact_ds_result==False:
            artifact_ds="-"
        else:
            artifact_ds=round(artifact_ds, 3)
        if psi_result==False:
            psi_time="-"
        fw.write(bench+", "+str(artifact_eq_result)+", "+str(artifact_eq_time)+", "
                    +str(artifact_ds)+", "+str(artifact_ds_time)+", " 
                    +str(psi_result)+", "+str(psi_time)+"\n")
            # break
        # break
    
