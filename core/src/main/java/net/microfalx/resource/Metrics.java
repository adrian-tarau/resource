package net.microfalx.resource;

import java.lang.module.ResolutionException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.microfalx.resource.ResourceUtils.requireNonNull;

/**
 * An abstraction to track various metrics.
 * <p>
 * It also carries a group name (namespace) so all metrics are covering the same group of metrics without the new to
 * keep adding the same namespace.
 * <p>
 * It also abstracts counters, gauges, timers and histograms with simple method calls.
 */
public abstract class Metrics implements Cloneable {

    private static final String METRICS_IMPLEMENTATION_CLASS = "net.microfalx.resource.MicrometerMetrics";
    private static final String GROUP_SEPARATOR = ".";

    private String group;
    protected Map<String, String> tags = new HashMap<>();
    protected String[] tagsArray = new String[0];

    /**
     * Returns an instance of a metric group.
     *
     * @return a non-null instance
     */
    public static Metrics of(String group) {
        return doCreate(group);
    }

    /**
     * Creates an instance with a given group (namespace)
     *
     * @param group the group name
     */
    public Metrics(String group) {
        requireNonNull(group);
        this.group = group;
    }

    /**
     * Returns the final name of a metrics within this group.
     *
     * @param name the name
     * @return the final name
     */
    public String getName(String name) {
        return finalName(name);
    }

    /**
     * Registers a common tag.
     *
     * @param key   the key
     * @param value the value
     */
    public void addTag(String key, String value) {
        requireNonNull(key);
        requireNonNull(value);

        tags.put(key, value);
        Collection<String> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            values.add(entry.getKey());
            values.add(entry.getValue());
        }
        this.tagsArray = values.toArray(new String[0]);
    }

    /**
     * Creates a copy of the current metrics and adds a new subgroup.
     *
     * @param name the name of the group
     * @return a new instance
     */
    public Metrics withGroup(String name) {
        requireNonNull(name);
        Metrics copy = copy();
        copy.group += GROUP_SEPARATOR + name;
        return copy;
    }

    /**
     * Creates a copy of the current metrics and adds a new tag.
     *
     * @param key   the key
     * @param value the value
     * @return a new instance
     */
    public Metrics withTag(String key, String value) {
        Metrics copy = copy();
        copy.addTag(key, value);
        return copy;
    }

    /**
     * Updates metrics specific tags.
     */
    protected void updateTagsCache() {
        // empty on purpose
    }

    /**
     * Increases a gauge by one.
     *
     * @param name the name of the counter
     */
    public long increment(String name) {
        return doCount(name);
    }

    /**
     * Decreases a gauge by one.
     *
     * @param name the name of the counter
     */
    public long decrement(String name) {
        return doCount(name);
    }

    /**
     * Increments a counter within a group.
     *
     * @param name the name of the counter
     */
    public long count(String name) {
        return doCount(name);
    }

    /**
     * Registers a gauge which extracts the value from a supplier.
     *
     * @param name the name of the counter
     */
    public void gauge(String name, Supplier<Double> supplier) {
        doGauge(name, supplier);
    }

    /**
     * Times a block of code.
     *
     * @param name     the name of the timer
     * @param supplier the supplier
     */
    public <T> T time(String name, Supplier<T> supplier) {
        return doTime(name, supplier);
    }

    /**
     * Times a block of code.
     *
     * @param name     the name of the timer
     * @param callable the callable
     */
    public <T> T time(String name, Callable<T> callable) {
        return doTime(name, () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                return ResourceUtils.throwException(e);
            }
        });
    }

    /**
     * Times a block of code.
     *
     * @param name     the name of the timer
     * @param consumer the consumer
     */
    public <T> void time(String name, Consumer<T> consumer) {
        doTime(name, consumer, null);

    }

    /**
     * Times a block of code.
     *
     * @param name     the name of the timer
     * @param consumer the consumer
     * @param value    the value passed to the consumer
     */
    public <T> void time(String name, Consumer<T> consumer, T value) {
        doTime(name, consumer, value);
    }

    /**
     * Returns the group name (namespace) associated with this instance of metrics.
     *
     * @return a non-null instance
     */
    public final String getGroup() {
        return group;
    }

    /**
     * Returns the tags associated with this metrics.
     *
     * @return a non-null instance
     */
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    /**
     * Increments a counter within a group.
     *
     * @param name the name of the counter
     */
    public abstract long doCount(String name);

    /**
     * Increments a gauge within a group.
     *
     * @param name the name of the counter
     */
    public abstract long doIncrement(String name);

    /**
     * Decrements a gauge within a group.
     *
     * @param name the name of the counter
     */
    public abstract long doDecrement(String name);

    /**
     * Registers a gauge which extracts the value from a supplier.
     *
     * @param name the name of the counter
     */
    public abstract void doGauge(String name, Supplier<Double> supplier);

    /**
     * Times a block of code.
     *
     * @param name the name of the timer
     */
    public abstract <T> T doTime(String name, Supplier<T> supplier);

    /**
     * Times a block of code.
     *
     * @param name the name of the timer
     */
    public abstract <T> void doTime(String name, Consumer<T> consumer, T value);

    /**
     * Creates the first available implementation of metrics.
     *
     * @param group the group name
     * @return the instance
     */
    private static Metrics doCreate(String group) {
        try {
            Class<?> clazz = Metrics.class.getClassLoader().loadClass(METRICS_IMPLEMENTATION_CLASS);
            return (Metrics) clazz.getConstructor(String.class).newInstance(group);
        } catch (Exception e) {
            return new NoMetrics(group);
        }
    }

    /**
     * Creates a deep copy of the metrics.
     *
     * @return a new copy
     */
    private Metrics copy() {
        try {
            Metrics metrics = (Metrics) clone();
            metrics.tags = new HashMap<>(tags);
            metrics.tagsArray = null;
            return metrics;
        } catch (CloneNotSupportedException e) {
            throw new ResolutionException("Cannot clone ", e);
        }
    }

    protected final String finalName(String name) {
        return normalize(group) + GROUP_SEPARATOR + normalize(name);
    }

    private static String normalize(String name) {
        if (name == null) return "na";
        name = name.toLowerCase();
        name = name.replace(' ', '_');
        return name;
    }

    /**
     * Default implementation in case there is no other present.
     */
    static class NoMetrics extends Metrics {

        public NoMetrics(String group) {
            super(group);
        }

        @Override
        public long doCount(String name) {
            return 0;
        }

        @Override
        public long doIncrement(String name) {
            return 0;
        }

        @Override
        public long doDecrement(String name) {
            return 0;
        }

        @Override
        public void doGauge(String name, Supplier<Double> supplier) {

        }

        @Override
        public <T> T doTime(String name, Supplier<T> supplier) {
            return supplier.get();
        }

        @Override
        public <T> void doTime(String name, Consumer<T> consumer, T value) {
            consumer.accept(value);
        }
    }
}
