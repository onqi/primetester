Prime tester
======================
## Application


## Cluster Launch
App consists of 2 deployables - Frontend and Worker cluster which are launched separately
To launch worker cluster (of 3 nodes on `127.0.1` using local seed-nodes), type

```
mvn exec:java@allInOne
```

### Launching Worker Cluster nodes remotely
To launch worker node on the remote host (bind to port 21738), type 
```
mvn -Dakka.cluster.seed-nodes.0=akka.tcp://PrimeTesterCluster@host1:2552 \
-Dakka.cluster.seed-nodes.1=akka.tcp://PrimeTesterCluster@host2:2552 \
-Dexec.args="21738" jetty:run@singleNode
```
Note that `host1:2552` and `host2:2552` should reference cluster seed nodes. 

## Frontend Launch
To launch Frontend on the host, type 
```
mvn jetty:run
```
Jetty is configured to listen on port 8000. Web UI should be available at http://localhost:8000 once started. 
Cluster should be started first.
 
# Test data
Pre-calculated prime numbers are used as test data set.
Numbers are available online and for simplicity of use are converted to single row. 
If another data set is to be used (instead of provided), it can be prepared using following script:

```
cd test/resources \
&& wget -qO- -O tmp.zip --no-check-certificate https://primes.utm.edu/lists/small/millions/primes50.zip \
&& unzip tmp.zip \
&& rm tmp.zip \
&& tail -n+3 primes50.txt | sed 's/\s/\n/g' | sed '/^[ \t]*$/d' > primes50.txt
```

This process can be automated, but left as is for now because of the following reasons:

- there's no way to bypass cert issues without fixing keychain (running `keytool`) on the machine where code runs
- `exec-maven-plugin` is platform-dependent
