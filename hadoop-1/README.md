Build, Test, Jar
--------------------------------------------
```
./gradlew test
./gradlew build
./gradlew jar
```

在hadoop的master机器上:
```
hadoop fs -conf src/main/conf/hadoop-localhost.xml -ls /
```

```
export HADOOP_CLASSPATH=build/classes/main/
/vagrant_downloads/hadoop/hadoop-1# hadoop ConfigurationPrinter -conf src/main/conf/hadoop-localhost.xml
```

Temperature Data:
ftp://ftp.ncdc.noaa.gov/pub/data/noaa/
```
export HADOOP_CLASSPATH=build/classes/main/
hadoop MaxTemperatureDriver -fs file:/// -jt local src/test/fixtures/temperature.txt output
cat output/part-r-00000
```

```
HADOOP_CLASSPATH=share/hadoop/yarn/test/hadoop-yarn-server-tests-2.6.2-tests.jar hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-client-jobclient-2.6.2-tests.jar minicluster
```

```
unset HADOOP_CLASSPATH
hadoop jar build/libs/hadoop-1-1.0.jar -conf src/main/conf/hadoop-cluster.xml -fs file:/// -jt local src/test/fixtures/temperature.txt max-temp/
```
We unset the HADOOP_CLASSPATH environment variable because we don’t have any third-party dependencies for this job. If it were left set to target/classes/ (from earlier in the chapter), Hadoop wouldn’t be able to find the job JAR; it would load the MaxTempera tureDriver class from target/classes rather than the JAR, and the job would fail.


```
hadoop fs -copyFromLocal src/test/fixtures/temperature.txt /input/temperature.txt
hadoop jar build/libs/hadoop-1-1.0.jar -conf src/main/conf/hadoop-cluster.xml /input/temperature.txt /output/max-temp/
```

```
unset HADOOP_CLASSPATH
hadoop fs -rm -R /output/max-temp
hadoop jar build/libs/hadoop-1-1.0.jar -conf src/main/conf/hadoop-cluster.xml -D mapreduce.map.log.level=DEBUG /input/temperature.txt /output/max-temp/
```

Install oozie
--------------------------------------------

http://oozie.apache.org/docs/4.1.0/DG_QuickStart.html

```
apt-get install maven
tar -zxvf oozie-4.1.0.tar.gz . 
bin/mkdistro.sh -DskipTests -Dhadoop.version=2.3.0  #mvn clean package assembly:single -Dhadoop.version=2.6.2 -DskipTests
```
编译完成后
```
 tar zxvf distro/target/oozie-4.1.0-distro.tar.gz -C /opt
 cd /opt/oozie-4.1.0
 mkdir libex
```

安装mysql
```
sudo apt-get install mysql-server
mysql
create database oozie;
grant all privileges on oozie.* to oozie@'%' identified by 'oozie';
FLUSH PRIVILEGES;
```

vim conf/oozie-site.xml
修改其中的一些属性
```
<property>
    <name>oozie.service.JPAService.create.db.schema</name>
    <value>true</value>
</property>
<property>
    <name>oozie.service.JPAService.jdbc.driver</name>
    <value>com.mysql.jdbc.Driver</value>
</property>
<property>
    <name>oozie.service.JPAService.jdbc.url</name>
    <value>jdbc:mysql://localhost:3306/oozie?createDatabaseIfNotExist=true</value>
</property>

<property>
    <name>oozie.service.JPAService.jdbc.username</name>
    <value>oozie</value>
</property>

<property>
    <name>oozie.service.JPAService.jdbc.password</name>
    <value>oozie</value>
</property>
<property>
    <name>oozie.service.HadoopAccessorService.hadoop.configurations</name>
    <value>*=/opt/hadoop-2.6.2/etc/hadoop</value>
</property> 

```


vim $HADOOP_HOME/etc/hadoop/core-site.xml
```
<property>
   <name>hadoop.proxyuser.oozie.hosts</name>
   <value>*</value>
</property>
<property>
   <name>hadoop.proxyuser.oozie.groups</name>
   <value>*</value>
</property>
```

Refresh hdfs and yarn
```
hdfs dfsadmin -refreshSuperUserGroupsConfiguration
yarn rmadmin -refreshSuperUserGroupsConfiguration
```


```
bin/oozie-setup.sh prepare-war
bin/oozie-setup.sh sharelib create -fs hdfs://node1:9000

```

但是发现有异常:
```
Error: User: root is not allowed to impersonate root
```

于是增加oozie用户组和用户名
```
groupadd oozie
useradd -g oozie oozie
chgrp -hR oozie .
chown -R oozie .
sudo su 
hadoop fs -mkdir -p /user/oozie
hadoop fs -put max-temp-flow max-temp-workflow
hadoop fs -chown oozie:oozie /user/oozie
su oozie
bin/oozie-setup.sh sharelib create -fs hdfs://node1:9000
```

这时候就可以通过web访问oozie http://node1:11000.

su oozie
vim ~/.bashrc
```
export JAVA_HOME=/opt/jdk1.7.0_79
export PATH=$JAVA_HOME/bin:$PATH

export HADOOP_HOME=/opt/hadoop-2.6.2
export PATH=$HADOOP_HOME/bin:$PATH

export SCALA_HOME=/opt/scala-2.10.4
export PATH=$SCALA_HOME/bin:$PATH

export SPARK_HOME=/opt/spark-1.5.2-bin-hadoop2.6
export PATH=$SPARK_HOME/bin:$PATH

export PATH=/opt/oozie-4.1.0/bin:$PATH
```

在当前项目的根目录下
```
export OOZIE_URL="http://localhost:11000/oozie"
oozie job -config src/test/resources/max-temp-workflow.properties -run
```
查看job信息
```
oozie job -info 0000000-160109012023892-oozie-oozi-W
```
或通过
http://node1:11000/oozie来查看job信息.

查看oozie的log
/opt/oozie-4.1.0/logs/oozie.log
```
JOB[0000001-160109012023892-oozie-oozi-W] ACTION[0000001-160109012023892-oozie-oozi-W@max-temp-mr] Error starting action [max-temp-mr]. ErrorType [TRANSIENT], ErrorCode [JA009], Message [JA009: Permission denied: user=oozie, access=EXECUTE, inode="/tmp":root:supergroup:drwxrwx---
```
登陆root用户执行oozie的命令,还有错误
```
org.apache.oozie.action.ActionExecutorException:   JA006: Call From node1/192.168.33.201 to node1:10020 failed on connection exception: java.net.ConnectException: Connection refused; For more details see:  http://wiki.apache.org/hadoop/ConnectionRefused
	at org.apache.oozie.action.ActionExecutor.conv
```
10020端口是jobhistory的端口

hadoop常用端口及定义方法: http://blog.csdn.net/xygl2009/article/details/44813727

```
$HADOOP_HOME/sbin/mr-jobhistory-daemon.sh start historyserver
```

重新执行oozie的命令后,可以在historyserver的web端查看任务执行情况:
http://node1:19888/jobhistory
在我的三台虚拟机构成的虚拟机集群中,我的输入数据是1.7G的2014的Temperature数据, 运行
```
HDFS: Number of read operations: Map(42) Reduce(3)
Launched map tasks: 18
Total time spent by all map tasks (ms): 1292891
Map input records: 7684857
```

可以在oozie中web页面查看结果.


也可以在hdfs中查看结果
```
hadoop fs -cat /output/max-temp/part-r-00000
```


oozie的命令
```
oozie job -oozie http://localhost:11000/oozie -kill 14-20090525161321-oozie-joe
oozie job -oozie http://localhost:11000/oozie -config job.properties -rerun 14-20090525161321-oozie-joe 
bin/oozied.sh start #重启oozie
```

Install PIG
--------------------------------------------

http://apache.fayea.com/pig/pig-0.15.0/pig-0.15.0.tar.gz
下载PIG的包而后解压.

```
tar zxvf pig-0.15.0.tar.gz -C /opt
```

vim /root/.bashrc

```
export PIG_HOME=/opt/pig-0.15.0
export PATH=$PIG_HOME/bin:$PATH
```

source /root/.bashrc


```
pig -x local  #local mode
pig -x mapreduce #mapreduce mode 

```


拷贝/etc/passwd到当前目录,而后运行pig的local模式或者mapreduce模式.
```
grunt> A = load 'passwd' using PigStorage(':');
grunt> B = foreach A generate $0 as id;
grunt> dump B;

```

另pig本身的examples在`/opt/pig-0.15.0/tutorial`目录下.


Install HBase
--------------------------------------------

### HBase in local mode

```

tar zxvf hbase-1.0.2-bin.tar.gz -C /opt
```

vim conf/hbase-site.xml
```
<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>file:///home/testuser/hbase</value>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>/home/testuser/zookeeper</value>
  </property>
</configuration>
```
You do not need to create the HBase data directory. HBase will do this for you. If you create the directory, HBase will attempt to do a migration, which is not what you want.


```
bin/start-hbase.sh
./bin/hbase shell
create 'test', 'cf'
put 'test', 'row1', 'cf:a', 'value1'
put 'test', 'row2', 'cf:b', 'value2'
put 'test', 'row3', 'cf:c', 'value3'
scan 'test'
get 'test', 'row1
bin/stop-hbase.sh'
```

### HBase in distribution mode
vim conf/hbase-site.xml
```
<configuration>
<property>
  <name>hbase.cluster.distributed</name>
  <value>true</value>
</property>
<property>
  <name>hbase.rootdir</name>
  <value>hdfs://node1:9000/hbase</value>
</property>
</configuration>
```


Install Hive
--------------------------------------------
http://apache.fayea.com/hive/hive-1.2.1/apache-hive-1.2.1-bin.tar.gz
https://cwiki.apache.org/confluence/display/Hive/GettingStarted
```
tar zxvf apache-hive-1.2.1-bin.tar.gz -C /opt
```

vim /root/.bashrc
```
export HIVE_HOME=/opt/apache-hive-1.2.1-bin
export PATH=HIVE_HOME/bin:$PATH
```

source /root/.bashrc

```
hadoop fs -mkdir /tmp
hadoop fs -chmod g+w /tmp
hadoop fs -mkdir -p /user/hive/warehouse
hadoop fs -chmod g+w  /user/hive/warehouse
hive
```

发现有error
```
Found class jline.Terminal, but interface was expected
```

拷贝最新的jline库到hadoop相应目录下:
```
cp lib/jline-2.12.jar $HADOOP_HOME/share/hadoop/yarn/lib
```

```
CREATE TABLE pokes (foo INT, bar STRING);
CREATE TABLE invites (foo INT, bar STRING) PARTITIONED BY (ds STRING);
SHOW TABLES;
```