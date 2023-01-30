package com.gtnewhorizon.structurelib.structure;

/**
 * Implement this and use {@link StructureUtility#withContext(IStructureElement)} in case you need to record some
 * temporary states that don't need to live in your main context, which would usually live for the whole lifetime of the
 * structure than just during structure check. Usually just save some few bytes, but becomes non-trivial if you need to
 * record too much stuff.
 * <p>
 * You will have to reset the context yourself though. The API can't guess when the old context becomes invalid.
 *
 * @param <CTX> the temporary state context
 */
public interface IWithExtendedContext<CTX> {

    CTX getCurrentContext();
}
