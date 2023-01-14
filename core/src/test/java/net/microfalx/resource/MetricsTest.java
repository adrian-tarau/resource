package net.microfalx.resource;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsTest {

    private Metrics metrics = Metrics.of("resource");

    @BeforeAll
    static void registerRegistry() {
        io.micrometer.core.instrument.Metrics.globalRegistry.clear();
        io.micrometer.core.instrument.Metrics.addRegistry(new SimpleMeterRegistry());
    }

    @Test
    void count() {
        metrics.count("c1");
        assertEquals(1, (long) io.micrometer.core.instrument.Metrics.counter("resource.c1").count());
    }

    @Test
    void time() {
        metrics.time("t1", (t) -> sleep(200));
        assertEquals(1, io.micrometer.core.instrument.Metrics.timer("resource.t1").count());
        org.assertj.core.api.Assertions.assertThat(io.micrometer.core.instrument.Metrics.timer("resource.t1").totalTime(TimeUnit.MILLISECONDS))
                .isGreaterThan(150).isLessThan(300);
    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // ignore
        }
    }

}