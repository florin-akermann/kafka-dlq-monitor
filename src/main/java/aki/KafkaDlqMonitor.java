package aki;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Attributes;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.List;

@Slf4j
public class KafkaDlqMonitor {

    public static void main(String[] args) {
        var mirrorStream = new MonitorStream();
        mirrorStream.start();
        Runtime.getRuntime().addShutdownHook(new Thread(mirrorStream::stop));
    }

    private static class MonitorStream {

        private final Config config;
        private final ActorSystem system;
        private final ConsumerSettings<byte[], byte[]> consumerSettings;
        private Consumer.Control control;

        private MonitorStream() {

            this.config = ConfigFactory.load();

            consumerSettings = ConsumerSettings.create(
                    config.getConfig("akka.kafka.consumer"),
                    new ByteArrayDeserializer(),
                    new ByteArrayDeserializer()
            );

            this.system = ActorSystem.create();
        }

        void start() {

            control = Consumer
                    .plainSource(consumerSettings, Subscriptions.topics(config.getString("topic")))
                    .groupedWithin(config.getInt("monitor.max-dead-letters-in-duration"), config.getDuration("monitor.duration"))
                    .filter(group -> group.size() >= config.getInt("monitor.max-dead-letters-in-duration"))
                    .map(List::size)
                    .log("dlq threshold reached")
                    .addAttributes(getLogLevels())
                    .to(Sink.ignore())
                    .run(Materializer.matFromSystem(system));
        }

        private Attributes getLogLevels() {
            return Attributes.createLogLevels(
                    Attributes.logLevelWarning(),
                    // onElement we only see elements if threshold is reached
                    Attributes.logLevelInfo(),
                    // onFinish
                    Attributes.logLevelError()
                    // onFailure
            );
        }

        void stop() throws RuntimeException {
            control.stop().whenComplete((done, throwable) -> {
                if (throwable != null) log.warn("encountered trouble when stopping the stream", throwable);
                log.info("stream {}", done);
            });
        }

    }

}
