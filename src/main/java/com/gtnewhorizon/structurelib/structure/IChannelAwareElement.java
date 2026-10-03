package com.gtnewhorizon.structurelib.structure;

/**
 * Identifies the channel selected by a structure element wrapper.
 *
 * @param <T> structure context type
 */
public interface IChannelAwareElement<T> extends IStructureElement<T> {

    /**
     * Returns the channel used by this element.
     *
     * @return lower-case channel identifier
     */
    String getChannel();
}
