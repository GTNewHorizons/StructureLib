package com.gtnewhorizon.structurelib.structure;

import java.util.Collection;

/**
 * Identifies the channels selected by structure element wrappers.
 *
 * @param <T> structure context type
 */
public interface IChannelAwareElement<T> extends IStructureElement<T> {

    /**
     * Returns the channels used by this element.
     *
     * @return lower-case channel identifiers
     */
    Collection<String> getChannels();
}
