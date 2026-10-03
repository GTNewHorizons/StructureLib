package com.gtnewhorizon.structurelib.structure;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * Factory methods for common structure channel policies.
 */
public class ChannelDefinitions {

    /**
     * Creates a channel with a bounded inclusive range and clamping normalization.
     *
     * @param channel      channel identifier
     * @param minimumValue inclusive minimum value
     * @param maximumValue inclusive maximum value
     * @return bounded channel definition
     */
    public static IChannelDefinition bounded(String channel, int minimumValue, int maximumValue) {
        validateChannel(channel);
        if (minimumValue < 0 || maximumValue < minimumValue) {
            throw new IllegalArgumentException("Invalid channel bounds");
        }
        return new BoundedChannelDefinition(channel, minimumValue, maximumValue);
    }

    /**
     * Creates a channel accepting every non-negative integer.
     *
     * @param channel channel identifier
     * @return unbounded channel definition
     */
    public static IChannelDefinition unbounded(String channel) {
        validateChannel(channel);
        return new UnboundedChannelDefinition(channel);
    }

    /**
     * Validates a channel definition before it is attached to a structure definition.
     *
     * @param definition definition to validate
     */
    public static void validate(IChannelDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("Channel definition cannot be null");
        validateChannel(definition.getChannel());
        int minimum = definition.getMinimumValue();
        int maximum = definition.getMaximumValue();
        if (minimum < 0 || maximum < minimum) {
            throw new IllegalArgumentException("Invalid channel definition values");
        }
        int normalized = definition.normalizeValue(minimum);
        if (normalized < minimum || normalized > maximum) {
            throw new IllegalArgumentException(
                    "Channel normalization must return a value within the definition bounds");
        }
    }

    /**
     * Validates a channel identifier.
     *
     * @param channel channel identifier
     */
    public static void validateChannel(String channel) {
        if (StringUtils.isEmpty(channel) || !channel.toLowerCase(Locale.ROOT).equals(channel)) {
            throw new IllegalArgumentException("Channel name must be non-empty and lower case");
        }
    }

    public static class BoundedChannelDefinition implements IChannelDefinition {

        private final String channel;
        private final int minimumValue;
        private final int maximumValue;

        private BoundedChannelDefinition(String channel, int minimumValue, int maximumValue) {
            this.channel = channel;
            this.minimumValue = minimumValue;
            this.maximumValue = maximumValue;
        }

        @Override
        public String getChannel() {
            return channel;
        }

        @Override
        public int getMinimumValue() {
            return minimumValue;
        }

        @Override
        public int getMaximumValue() {
            return maximumValue;
        }

    }

    public static class UnboundedChannelDefinition implements IChannelDefinition {

        private final String channel;

        private UnboundedChannelDefinition(String channel) {
            this.channel = channel;
        }

        @Override
        public String getChannel() {
            return channel;
        }

    }
}
