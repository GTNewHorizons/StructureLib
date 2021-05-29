package com.gtnewhorizon.structurelib.alignment.constructable;

import javax.annotation.Nullable;

/**
 * Implement this interface if this tile entity MIGHT be constructable
 */
public interface IConstructableProvider {
	/**
	 * @return null if not constructable, an instance otherwise.
	 */
	@Nullable
	IConstructable getConstructable();
}
