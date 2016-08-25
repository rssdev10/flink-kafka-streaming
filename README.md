# flink-kafka-streaming
streaming and partitioning test

#  How to run
  ```git clone https://github.com/rssdev10/flink-kafka-streaming.git```
  
  ```cd flink-kafka-streaming```

  # downloads kafka and zookeeper
  
  ```./gradlew setup```

  # run zookeeper, kafka, and run messages generation
  
  ```./gradlew test_data_prepare```

To break data generation press CTRL-C. And continue it by same command

```./gradlew test_data_prepare```

And in other console just run:

   ```./gradlew test_flink```

Stop all:
  ```./gradlew stop_all```
  
Build jar for deploying in Flink cluster:
  ```./gradlew shadowJar```

Spark test must generate messages each 10 seconds like:
```text
***************************************************************************
Processing time: 10054
Expected time: 10000
Processed messages: 98302
Unique items: 1000
```

*message* is number of fist message in the window. Time values are in milliseconds.

# Additional parameters

[src/main/java/KafkaDataProducer.java](src/main/java/KafkaDataProducer.java)
```MESSAGES_NUMBER = 100L * 1000 * 1000;```

Number of partitions:
[src/main/java/FlinkStreamingConsumer.java](src/main/java/FlinkStreamingConsumer.java)
```partNum = 10; ```
