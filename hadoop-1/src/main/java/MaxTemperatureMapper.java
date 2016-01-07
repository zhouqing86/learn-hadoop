import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.commons.logging.Log;

import java.io.IOException;

public class MaxTemperatureMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

    private NcdcRecordParser parser = new NcdcRecordParser();
    private static final Log LOG = LogFactory.getLog(MaxTemperatureMapper.class);

    enum Temperature {
        OVER_100
    }

    public void map(LongWritable key, Text value, Mapper.Context context) throws IOException, InterruptedException {
        parser.parse(value);
        if (parser.isValidTemperature()) {
            int airTemperature = parser.getAirTemperature();
            if (airTemperature > 1000) {
                System.err.println("Temperature over 100 degrees for input: " + value);
                context.setStatus("Detected possibly corrupt record: see logs.");
                context.getCounter(Temperature.OVER_100).increment(1);
            }
            LOG.info("Map key:" + key);
            if(LOG.isDebugEnabled()) {
                LOG.debug("Map value" + value);
            }
            context.write(new Text(parser.getYear()),
                    new IntWritable(airTemperature));
        }
    }
}