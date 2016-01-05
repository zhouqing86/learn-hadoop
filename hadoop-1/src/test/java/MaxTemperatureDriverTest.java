import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.mortbay.log.Log;
import org.mortbay.log.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MaxTemperatureDriverTest {
    @Test
    public void test() throws Exception {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "file:///");
        conf.set("mapreduce.framework.name", "local");
        conf.setInt("mapreduce.task.io.sort.mb", 1);
        Path input = new Path("src/test/fixtures/temperature.txt");
        Path output = new Path("output");
        FileSystem fs = FileSystem.getLocal(conf);
        fs.delete(output, true); // delete old output
        MaxTemperatureDriver driver = new MaxTemperatureDriver();
        driver.setConf(conf);
        int exitCode = driver.run(new String[] { input.toString(), output.toString() });
        assertThat(exitCode, is(0));
//        checkOutput(conf, output);
    }

}
