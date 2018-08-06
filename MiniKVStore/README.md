Distributed Data Store Example
==============================

This is an example of distributed data store.

In the heart of this distributed data store lies the Gossip protocol.

A gossip protocol is a procedure or process of computer–computer communication that is based on the way social networks disseminate information or how epidemics spread. It is a communication protocol. Modern distributed systems often use gossip protocols to solve problems that might be difficult to solve in other ways, either because the underlying network has an inconvenient structure, is extremely large, or because gossip solutions are the most efficient ones available.

I have used Akka Distributed Data to implement this data store, which uses Gossip protocol internally to disseminate the data.

Akka Distributed Data is useful when you need to share data between nodes in an Akka Cluster. The data is accessed with an actor providing a key-value store like API. The keys are unique identifiers with type information of the data values. The values are Conflict Free Replicated Data Types (CRDTs).
All data entries are spread to all nodes, or nodes with a certain role, in the cluster via direct replication and gossip based dissemination. You have fine grained control of the consistency level for reads and writes.

The nature CRDTs makes it possible to perform updates from any node without coordination. 
It is eventually consistent and geared toward providing high read and write availability (partition tolerance), with low latency.


In order to run the application do the following: <br/>

1. Install Maven 3
2. Clone the repo and run seed nodes on 5551 and 5552):
```
./run.sh 4455 5551 (4455 -- Rest API port , 5551 -- Port where the 1st seed node runs)
```
```
./run.sh 4456 5552 (4456 -- Rest API port , 5552 -- Port where the 2nd seed node runs)
```
3. Additional nodes can be run without specifying a seed node port and only Rest API Port:
```bash
./run.sh 4457
```

Following are sample request for SET and GET 

```
curl -XPOST http://localhost:4455/set/name -d ‘“Pramati”’
```

```
 curl http://localhost:4457/get/name
````