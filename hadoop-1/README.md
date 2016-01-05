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


