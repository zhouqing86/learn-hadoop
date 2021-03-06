import junit.framework.TestCase;
import org.apache.hadoop.conf.Configuration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HadoopConfigurationTest extends TestCase {

    public void testConfiguration(){
        Configuration conf = new Configuration();
        conf.addResource("configuration-1.xml");
        assertThat(conf.get("color"), is("yellow"));
        assertThat(conf.getInt("size", 0), is(10));
        assertThat(conf.get("breadth", "wide"), is("wide"));
    }

}