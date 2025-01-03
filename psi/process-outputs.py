import sys

file1 = sys.argv[1]
file2 = sys.argv[2]

with open(file1,'r') as fr:
    lines = fr.readlines()
    for line in lines:
        print(line)

with open(file2,'r') as fr:
    lines = fr.readlines()
    lines[0]=lines[0].replace("p[","q[")
    for line in lines:
        print(line)
print("TrueQ[p[x]==q[x]]")

        
    