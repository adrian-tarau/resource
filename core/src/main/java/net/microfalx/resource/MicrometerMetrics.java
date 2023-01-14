package net.microfalx.resource;

import io.micrometer.core.instrument.*;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.microfalx.resource.ResourceUtils.requireNonNull;

public class MicrometerMetrics extends Metrics {

    private final MeterRegistry registry = io.micrometer.core.instrument.Metrics.globalRegistry;
    private Iterable<Tag> mtags;

    public MicrometerMetrics(String group) {
        super(group);
    }

    @Override
    public long doCount(String name) {
        requireNonNull(name);
        Counter counter = registry.counter(finalName(name), tagsArray);
        counter.increment();
        return (long) counter.count();
    }

    @Override
    public long doIncrement(String name) {
        AtomicLong gauge = registry.gauge(finalName(name), new AtomicLong());
        return gauge.incrementAndGet();
    }

    @Override
    public long doDecrement(String name) {
        AtomicLong gauge = registry.gauge(finalName(name), new AtomicLong());
        return gauge.decrementAndGet();
    }

    @Override
    public void doGauge(String name, Supplier<Double> supplier) {
        registry.gauge(finalName(name), mtags, null, value -> supplier.get());
    }

    @Override
    public <T> T doTime(String name, Supplier<T> supplier) {
        Timer timer = registry.timer(finalName(name), mtags);
        return timer.record(supplier);
    }

    @Override
    public <T> void doTime(String name, Consumer<T> consumer, T value) {
        Timer timer = registry.timer(finalName(name), mtags);
        timer.record(() -> consumer.accept(value));
    }

    @Override
    protected void updateTagsCache() {
        mtags = Tags.of(tagsArray);
    }
}
