FROM openjdk:11

COPY . ./home
RUN apt-get update
RUN apt-get install nano libxml2 gcc python3-distutils rsync -y
RUN mkdir /opt/gurobi
RUN cp /home/gurobi.lic /opt/gurobi/
RUN ln -s /usr/lib/x86_64-linux-gnu/libmpfr.so.6 /usr/lib/x86_64-linux-gnu/libmpfr.so.4
ENV GUROBI_HOME="gurobi/linux64"
ENV PATH="${PATH}:${GUROBI_HOME}/bin"
ENV LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:${GUROBI_HOME}/lib"
ENTRYPOINT ["/bin/bash"]
WORKDIR "/home"