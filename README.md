# Artifact of "Refuting Equivalence in Probabilistic Programs with Conditioning"

## Introduction 

This repository contains the artifact of the paper titled "Refuting Equivalence in Probabilistic Programs with Conditioning" accepted at TACAS 2025. 

The tool takes two probabilistic transition systems with specified initial configurations as input and based on user preferences either (i) tries to prove whether the two programs generate equivalent output distributions, or (ii) tries to find a lowerbound on Kantorovich distance between the output distributions of the input programs. 

In order to run the baseline, Mathematica (`math` command in terminal) must be installed.

Additionally, the repository contains the files for Gurobi Optimizer v10.0.3. 

## Getting Started Guide - Running The Tool in Docker

IMPORTANT: The artifact depends on Gurobi optimizer which is academically licensed. The artifact will work as expected, only if a valid Gurobi license is activated. Moreover, because of Mathematica's license, we have not included our baseline in the docker image. Please see the "Building From Source" section for more info on how to run the baseline on the benchmark set. The docker image has been tested on a system running Ubuntu 22.04 and worked as expected.

In order to load and run the experiments in a docker container, a Gurobi WLS license should be stored in `gurobi.lic`, and then the following commands should be executed:

```
docker build -t artifact .
docker run -it artifact
```

Then, to build our tool run the following command in the docker terminal:
```
./build.sh
```

In order to reproduce the results in the paper, run the following commands:

```
./table-1.sh
```

This will run our tool on all input pairs in the `inputs-*/` directories and might take upto 36 hours to finish. Therefore, we have gathered the list of configurations for which our tool can disprove equivalence and/or find non-trivial distance lowerbound in `best_configs.csv` and the following command runs our tool on them (and terminates within 10 minutes):

```
./best_configs.sh
```

The outputs of the above commands will be stored in the `outputs-*` directories. Specifically, `outputs-equivalence` will contain the results of our equivalence disproving mechanism, `outputs-distance` will containt the distance lowerbounds found by our tool and `outputs-psi` will contain the baseline's (PSI) outputs. By running the following command `table-1-results.csv` will be created which corresponds to the results in table-1 of the paper:

```
python3 table-1-to-csv.py
```


## Building From Source

### Dependencies

#### Our Tool

The tool is written in Java and works well with Openjdk 11 on Linux machines (some of the dependencies are not available for Mac OS). The main dependency of the tool comes from Gurobi, where a valid Gurobi license should be visible to Gurobi. Please refer to Gurobi's [documentation](https://support.gurobi.com/hc/en-us/articles/14799677517585-Getting-Started-with-Gurobi-Optimizer) for guidance on how to obtain and activate such a license. 

Our tool also makes use of [ASPIC](https://laure.gonnord.org/pro/aspic/aspic.html) which depends on `libmpfr4`. However, if your machine has `libmpfr6` you can create a symbolic link to it using the following command:

```
ln -s /usr/lib/x86_64-linux-gnu/libmpfr.so.6 /usr/lib/x86_64-linux-gnu/libmpfr.so.4
```

#### Baseline 
The baseline used in the experiments uses [PSI](https://psisolver.org/) and [Mathematica](https://www.wolfram.com/mathematica/). The build process also builds PSI and its dependencies. Please refer to Mathematica's installation guide for its installation. If installed correctly, you should be able to run `math` in your terminal to run Mathematica. 

### Build

To build the tool together with PSI, simply run the following command:
```
./build.sh
``` 
Note that PSI requires a working internet connection during its installation.


### Reproducing Experimental Results

To reproduce the results in the paper, run `./table-1.sh` and then `python3 table-1-to-csv.py`. The former runs all the experiments on input files provided in the `inputs-*` folders and writes the results in the `outputs-*` folders. The latter reads the contents of the `outputs-*` folder and summarizes them in a fresh `table-1-results.csv` file.

Alternatively, one can run `./best_configs.sh` (instead of `./table-1.sh`) to run our tool only on those inputs that can be solved by our tool. Running this command takes about 10 minutes. 

### Tool Reusability: Running the Tool on New Examples

To use the tool on a single input pair the following commands should be executed:

```
./run.sh [input1] [input2] [degree] [sting/nosting] [aspic/noaspic] equivalence  
./run.sh [input1] [input2] [degree] [sting/nosting] [aspic/noaspic] distance
```
Where:
   - `input1` and `input2` are the addresses of input transition systems and their syntax follows the rules explained in the next section.
   - `degree` is the degree of templates used by the method. 
   - `sting` and `nosting` flags specify whether STING should be used or not, respectively. 
   - `aspic` and `noaspic` flags specify whether ASPIC should be used or not, respectively. 

 If the first command is used, the tool will try to refute equivalence of the two input programs and if the second command is used, the tool will try to find a lowerbound on the Kantorovich distance of the output distributions of the two input programs. 
 
### Tool Reusability: Syntax 

The input syntax follows that of [T2](https://github.com/mmjb/T2) with several additional conditions:
 - The initial valuation of variables must be provided before the transitions in an expression as follows:
 ```
 INIT: {var1==val1 && var2==val2 ...};
 ```
 - If a transition is taken with probability $p$, then it must contain a `prob(p)` expression. 
 - The syntax contains the following keywords for several well-known distributions:
    - `uniform(a,b)` for the uniform distribution on the inteval $[a,b]$ where $a,b$ can be rational expressions of the form $x/y$ with $x,y \in \mathbb{Z}$.
    - `unifInt(a,b)` for the uniform distribution on integers in the interval $[a,b]$. Note that in this case, $a,b$ must be integers.
    - `discrete`($a_1,a_2,...,a_n,p_1,p_2,...,p_n$), where $\sum p_i = 1$ and all parameters are rationals, for a distribution on $a_i$ values where $P(a_i)=p_i$. 
    - `normal`($\mu,\sigma^2$) where $\mu, \sigma^2$ are rational expressions.
 - Each transition is either probabilistic or deterministic. Each probabilistic transition can contain at most one probabilistic assignment or `prob` expression.
 - Non-polynomial expressions are not allowed.
 - Scoring is done by `score(p,M)` statements where `p` is the actual score and `M` is a uniform upperbound on `p`.
 - Conditioning is done by `observe(b)` statements, where `b` is the boolean expression being observed. 

 See the inputs provided in the `inputs-*` folders as examples.
