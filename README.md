## SOEN 423: Distributed System Group Project - DRRS

Software failure tolerant and highly available distributed system under process crash failure using active replication.

Our actively replicated DRRS server system has four replicas each running a different implementation (in different hosts on the network). Each replica has a Replica Manager (RM) which detects and recovers from a failure. The front end (FE) receives a client request and forwards it to a failure-free sequencer. The sequencer assigns a unique sequence number and reliably multicasts the request to all the replicas. 

In order to handle a software failure, the four replicas execute client requests in total order and return the results back to the FE which in turn returns a single correct result back to the client as soon as two identical (correct) results are received from the replicas. If any one of the replicas produces incorrect result, the FE informs all the RMs about that replica. If the same replica produces incorrect results for three consecutive client requests, then the RMs replace that replica with another correct one. If the FE does not receive the result from a replica within a reasonable amount of time (twice the time taken for the slowest result so far), it suspects that replica may have crashed and informs all the RMs of the potential crash. The RMs then check the replica that did not produce the result and replace it with another working replica if they agree among themselves that the replica has crashed. This communication is made reliable using acks in order to avoid message loss.


## Team 🦄
| Name | Github Username |
|---|---|
| Donya Meshgin | [meshgin](https://github.com/meshgin) |
| Laila Chamma'a | [laila-chammaa](https://github.com/laila-chammaa) |
| Razvan Pirvu | [razvy942](https://github.com/razvy942) |
| Tommy Andrews | [t-andrews](https://github.com/t-andrews) |
