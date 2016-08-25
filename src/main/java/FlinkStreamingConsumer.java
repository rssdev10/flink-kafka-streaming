import com.google.gson.Gson;
import org.apache.flink.api.common.functions.FoldFunction;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.ProcessingTimeTrigger;
import org.apache.flink.streaming.api.windowing.triggers.PurgingTrigger;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer08;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class FlinkStreamingConsumer {
    private static final int windowDurationTime = 10;
    private static final int emergencyTriggerTimeout = 3;

    public static final String KAFKA_TOPIC = "test-aggregation";
    public static final int partNum = 10;

    private static final Gson gson = new Gson();

    static class UniqAggregator extends Statistics {
        public Set<String> uniqIds = new HashSet<String>();
    }

    static class ProductAggregator extends Statistics{
        public Integer value = 0;
    }

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Properties props = new Properties();

        props.put("bootstrap.servers", "localhost:9092");
        props.put("zookeeper.connect", "localhost:2181");

        props.put("group.id", "test");

        props.put("auto.offset.reset", "smallest");
        props.put("partition.assignment.strategy", "range");

        DataStream<String> dataStream = env
                .addSource(new FlinkKafkaConsumer08<String>(
                        KAFKA_TOPIC,
                        new SimpleStringSchema(), props));

        DataStream<Event> eventStream = dataStream
                .map(json -> {
                    Event event = gson.fromJson(json, Event.class);
                    event.setInputTime();
                    return event;
                });

        KeyedStream<Event, Integer> userIdKeyed = eventStream.keyBy(event -> event.partition(partNum));

        WindowedStream<Event, Integer, TimeWindow> uniqUsersWin =
                userIdKeyed.timeWindow(Time.seconds(windowDurationTime));

        DataStream<ProductAggregator> uniqUsers = uniqUsersWin.trigger(ProcessingTimeTrigger.create())
                .fold(new UniqAggregator(), (FoldFunction<Event, UniqAggregator>) (accumulator, value) -> {
                    accumulator.uniqIds.add(value.getUserId());

                    accumulator.registerEvent(value);

                    return accumulator;
                })
                .map(x -> {
                    //System.out.print("*");
                    ProductAggregator product = new ProductAggregator();
                    product.value = x.uniqIds.size();
                    product.summarize(x);
                    return product;
                });

        AllWindowedStream<ProductAggregator, TimeWindow> combinedUniqNumStream =
                uniqUsers
                        .timeWindowAll(Time.seconds(emergencyTriggerTimeout))
                        .trigger(PurgingTrigger.of(CountOrTimeTrigger.of(partNum)));

        combinedUniqNumStream
                .fold(new ProductAggregator(),
                        (FoldFunction<ProductAggregator, ProductAggregator>) (accumulator, value) -> {
                    accumulator.value += value.value;

                    accumulator.summarize(value);

                    return accumulator;
                })
                .map(x -> {
                    return "***************************************************************************"
                            + "\nProcessing time: " + Long.toString(x.lastTime - x.firstTime)
                            + "\nExpected time: " + Long.toString(windowDurationTime * 1000)
                            + "\nProcessed messages: " + Long.toString(x.count)
                            + "\nUnique items: " + Long.toString(x.value);
                })
                .print();

        env.execute();
    }
}
