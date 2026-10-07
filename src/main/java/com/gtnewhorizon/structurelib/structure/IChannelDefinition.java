package com.gtnewhorizon.structurelib.structure;

/**
 * Describes a channel used by one structure definition.
 *
 * <p>
 * Channel values are integers stored on the constructable trigger item. Implementations may override the default
 * validation and normalization policy when a simple bounded range is not sufficient.
 * </p>
 */
public interface IChannelDefinition {

    /**
     * Creates a channel with a bounded inclusive range and clamping normalization.
     *
     * @param channel      channel identifier
     * @param minimumValue inclusive minimum value
     * @param maximumValue inclusive maximum value
     * @return bounded channel definition
     */
    static IChannelDefinition bounded(String channel, int minimumValue, int maximumValue) {
        if (minimumValue < 0 || maximumValue < minimumValue) {
            throw new IllegalArgumentException("Invalid channel bounds");
        }
        return new IChannelDefinition() {

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
        };
    }

    /**
     * Creates a channel accepting every non-negative integer.
     *
     * @param channel channel identifier
     * @return unbounded channel definition
     */
    static IChannelDefinition unbounded(String channel) {
        return () -> channel;
    }

    /**
     * Returns the lower-case channel identifier.
     *
     * @return channel identifier
     */
    String getChannel();

    /**
     * Returns the smallest value advertised by this definition. The default is {@code 0}.
     *
     * @return minimum value
     */
    default int getMinimumValue() {
        return 0;
    }

    /**
     * Returns the largest value advertised by this definition. The default is {@link java.lang.Integer#MAX_VALUE}.
     *
     * @return maximum value
     */
    default int getMaximumValue() {
        return Integer.MAX_VALUE;
    }

    /**
     * Checks whether a value is already valid without normalization.
     *
     * @param value value to check
     * @return true when the value is valid
     */
    default boolean isValueValid(int value) {
        return value >= getMinimumValue() && value <= getMaximumValue();
    }

    /**
     * Converts a stored value to the value used by the structure definition.
     *
     * @param value stored value
     * @return normalized value
     */
    default int normalizeValue(int value) {
        return Math.max(getMinimumValue(), Math.min(getMaximumValue(), value));
    }
}
