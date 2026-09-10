package com.gtnewhorizon.structurelib.fluid;

/**
 * How much fluid a position has to hold before a fluid structure element considers it satisfied.
 * <p>
 * Structure definitions pick a policy, which allows a structure to accept an already partially filled position, e.g.
 * one that got filled by fluid that spread from somewhere else, instead of insisting on a full source block.
 */
public enum FluidFillPolicy {

    /**
     * The position has to hold exactly the amount of fluid the structure element asks for. This is the default, and it
     * accepts a full source block while rejecting anything that holds less.
     */
    EXACT_STATE,
    /**
     * The position has to hold at least the amount of fluid the structure element asks for. A position holding more
     * fluid than asked for is accepted as is, and is never drained down by autoplace.
     */
    AT_LEAST,
    /**
     * The position has to hold any amount of the same fluid at all. Only a position that holds no fluid of that kind is
     * topped up.
     */
    ANY_FILLED;

    /**
     * Test whether the given amount is acceptable.
     *
     * @param existingAmount how much fluid the position holds right now
     * @param targetAmount   how much fluid the structure element asks for
     */
    public boolean isSatisfied(int existingAmount, int targetAmount) {
        switch (this) {
            case EXACT_STATE:
                return existingAmount == targetAmount;
            case AT_LEAST:
                return existingAmount >= targetAmount;
            default:
                return existingAmount > 0;
        }
    }
}
