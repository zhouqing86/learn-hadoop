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

