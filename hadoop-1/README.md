在hadoop的master机器上:
```
hadoop fs -conf src/main/conf/hadoop-localhost.xml -ls /
```

```
export HADOOP_CLASSPATH=build/classes/main/
/vagrant_downloads/hadoop/hadoop-1# hadoop ConfigurationPrinter -conf src/main/conf/hadoop-localhost.xml
```