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

vim conf/oozie-site.xml, 修改其中的一些属性

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

或通过http://node1:11000/oozie来查看job信息.

查看oozie的log /opt/oozie-4.1.0/logs/oozie.log

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

```
hive
CREATE TABLE records (year STRING, temperature INT, quality INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t';
LOAD DATA LOCAL INPATH 'src/test/fixtures/hive-sample.txt' OVERWRITE INTO TABLE records;
select * from records;
SELECT year, MAX(temperature) from records group by year;
```

可以看到hadoop任务运行:

```
Query ID = root_20160122124846_e8974997-b93e-4a54-b67b-e0dfd934fbef
Total jobs = 1
Launching Job 1 out of 1
Number of reduce tasks not specified. Estimated from input data size: 1
In order to change the average load for a reducer (in bytes):
  set hive.exec.reducers.bytes.per.reducer=<number>
In order to limit the maximum number of reducers:
  set hive.exec.reducers.max=<number>
In order to set a constant number of reducers:
  set mapreduce.job.reduces=<number>
Starting Job = job_1453462787840_0001, Tracking URL = http://node1:8088/proxy/application_1453462787840_0001/
Kill Command = /opt/hadoop-2.6.2/bin/hadoop job  -kill job_1453462787840_0001
Hadoop job information for Stage-1: number of mappers: 1; number of reducers: 1
2016-01-22 12:48:56,336 Stage-1 map = 0%,  reduce = 0%
2016-01-22 12:49:03,651 Stage-1 map = 100%,  reduce = 0%, Cumulative CPU 0.9 sec
2016-01-22 12:49:12,992 Stage-1 map = 100%,  reduce = 100%, Cumulative CPU 2.07 sec
MapReduce Total cumulative CPU time: 2 seconds 70 msec
Ended Job = job_1453462787840_0001
MapReduce Jobs Launched:
Stage-Stage-1: Map: 1  Reduce: 1   Cumulative CPU: 2.07 sec   HDFS Read: 7165 HDFS Write: 17 SUCCESS
Total MapReduce CPU Time Spent: 2 seconds 70 msec
OK
1949	112
1950	46
Time taken: 28.032 seconds, Fetched: 2 row(s)
```

```
hive -hiveconf fs.defaultFS=hdfs://node1:9000  -hiveconf mapreduce.framework.name=yarn  -hiveconf yarn.resourcemanager.address=node1:8032
set -v;
```

```
CREATE TABLE sales (name STRING, id INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t';
CREATE TABLE things (id INT, name STRING) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t';
EXPLAIN SELECT sales.*, things.* FROM sales JOIN things ON (sales.id = things.id);
```

### User mysql to store HIVE metastore
#### Create The configuration files

```
cd apache-hive-1.0.0-bin/conf
cp hive-default.xml.template hive-site.xml
cp hive-env.sh.template hive-env.sh
cp hive-exec-log4j.properties.template hive-exec-log4j.properties
cp hive-log4j.properties.template hive-log4j.properties
```

#### dowload mysql connect drive and copy to $HIVE_HOME/lib/

#### Create mysql user for HIVE

```
mysql
create database hive;
grant all privileges on hive.* to hive@'%' identified by 'hive';
FLUSH PRIVILEGES;
```

修改mysql bind-address, 开启MySQL远程访问功能:

vim vim /etc/mysql/my.cnf, 注释掉`bind-address = 127.0.0.1`,而后`service mysql restart`

#### vim hdfs-site.xml
将文件中的system.替换成/tmp;

配置mysql的连接相关参数如下所示:

```
<property>
     <name>javax.jdo.option.ConnectionURL</name>
     <value>jdbc:mysql://node1:3306/hive?createDatabase
       IfNotExist=true</value>
     <description>JDBC connect string for a JDBC metastore
       </description>
   </property>
   <property>
     <name>javax.jdo.option.ConnectionDriverName</name>
     <value>com.mysql.jdbc.Driver</value>
     <description>Driver class name for a JDBC metastore
       </description>
   </property>
   <property>
     <name>javax.jdo.option.ConnectionUserName</name>
     <value>hive</value>
     <description>username to use against metastore database
       </description>
   </property>
   <property>
     <name>javax.jdo.option.ConnectionPassword</name>
     <value>hive</value>
     <description>password to use against metastore database
     </description>
</property>
```

### hiveserver2 简单测试
#### Start the hiveserver2

```
hive --service hiveserver2 --hiveconf hive.server2.thrift.port=10001
```

#### vi employee.txt

```
Michael|Montreal,Toronto|Male,30|DB:80|Product:Developer^DLead
Will|Montreal|Male,35|Perl:85|Product:Lead,Test:Lead
Shelley|New York|Female,27|Python:80|Test:Lead,COE:Architect
Lucy|Vancouver|Female,57|Sales:89,HR:94|Sales:Lead
```
#### Beeline client

Log in to Beeline with the proper HiveServer2 hostname, port number, database name, username, and password:

```
beeline
> !connect jdbc:hive2://localhost:10001/default
```

输入用户为root, 密码为空.而后在beeline console下

```
CREATE TABLE employee(name string, work_place ARRAY<string>, sex_age STRUCT<sex:string,age:int>, skills_score MAP<string,int>, depart_title MAP<string, ARRAY<string>>) ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' COLLECTION ITEMS TERMINATED BY ',' MAP KEYS TERMINATED BY ':';
!table employee
!column employee
LOAD DATA LOCAL INPATH '/vagrant_downloads/hadoop/hadoop-1/src/test/fixtures/exployee.txt' OVERWRITE INTO TABLE employee;
SELECT * FROM employee;
SELECT work_place FROM employee;
SELECT work_place[0] AS col_1, work_place[1] AS col_2, work_place[2] AS col_3 FROM employee;
SELECT sex_age.sex, sex_age.age FROM employee;
SELECT name, skills_score['DB'] AS DB, skills_score['Perl'] AS Perl, skills_score['Python'] AS Python, skills_score['Sales'] as Sales, skills_score['HR'] as HR FROM employee;
SELECT name, depart_title['Product'][0] AS product_col0, depart_title['Test'][0] AS test_col0 FROM employee;
```
