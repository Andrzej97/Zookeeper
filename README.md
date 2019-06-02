How to conifgure it, run it:

Add following jar files to your project(all files can be found in apache-zookeeper-3.5.5-bin/lib):
  slf4j-api-1.7.25.jar
  zookeeper-3.5.5.jar
  zookeeper-jute-3.5.5.jar

To run the application, firstly you should start a Zookeeper server (or servers if it is replicated mode).
Than you can run app with correct parameters.
To observe how app works you should also run a client by console and try some operations like 'create /z' or 'delete /z'

For replicated zookeeper conf files are as follows:

zoo1.cfg:
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/var/zookeeper/data/zk1
clientPort=2181
admin.serverPort=8081
server.1=localhost:2888:3888
server.2=localhost:2889:3889
server.3=localhost:2890:3890

zoo2.cfg
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/var/zookeeper/data/zk2
clientPort=2182
admin.serverPort=8081
server.1=localhost:2888:3888
server.2=localhost:2889:3889
server.3=localhost:2890:3890

zoo3.cfg
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/var/zookeeper/data/zk3
clientPort=2183
admin.serverPort=8081
server.1=localhost:2888:3888
server.2=localhost:2889:3889
server.3=localhost:2890:3890

remember to create directories :
/var/zookeeper/data/zk1   with file myid containing 1
/var/zookeeper/data/zk2   with file myid containing 2
/var/zookeeper/data/zk3   with file myid containing 3


To run replicated mode, firstly start all 3 servers, then client.
Use following commands in different consoles:
zkServer2.cmd \zoo1.cfg
zkServer2.cmd \zoo2.cfg
zkServer2.cmd \zoo3.cfg
zkCli.cmd

The above zkServer2.cmd is a programme downloaded from UPEL zookeeper lab directory.
