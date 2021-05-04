### Kafka Dlq Monitor

This small tool can be used to monitor a dead letter queue in the form of a kafka topic.
It rings the alarm bells by logging an error to stdout.
Depending on your logging stack (ELK or similar) this is enough to see a flag in your centralized logging system. 

The following configuration snippets indicates how this application works. 

    topic: "in"
    monitor: {
        duration: 1m
        max-dead-letters-in-duration: 500
    }

It subscribes to a single topic and logs a warning if a certain threshold is reached within a given time period (e.g. 500 messages within 1 minute).

Everything can be configured easily with Hocon.
Either via .conf file or as CONFIG_FORCE_* variables when running a container. The minimal configuration needed is presented in ./src/main/resources/application.conf

### Local Kafka & Zookeeper

run

    docker-compose up -d

### Docker / Jib

Build the image for your local image repo by running

    ./gradlew jibDockerBuild

run container 

    docker run --network=host kafka-dlq-monitor

consider overriding configs when running the containerized application, configure env variables as follows.

    docker run -e CONFIG_FORCE_topic=some-other-dlq --network=host kafka-dlq-monitor

### Jar

To run create and run jar directly

    ./gradlew shadowJar
    java -jar ./build/libs/kafka-dlq-monitor-1.0.0-all.jar

or with external config override:
    
    ./gradlew shadowJar
    java -jar -Dconfig.file=./path/to/config.conf ./build/libs/kafka-dlq-monitor-1.0.0-all.jar