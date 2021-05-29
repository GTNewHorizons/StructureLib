package com.gtnewhorizon.structurelib.alignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import static com.gtnewhorizon.structurelib.alignment.IAlignment.STATES_COUNT;

public interface IAlignmentLimits {

    IAlignmentLimits UNLIMITED = (direction, rotation, flip) -> true;

    boolean isNewExtendedFacingValid(ForgeDirection direction, Rotation rotation, Flip flip);

    default boolean isNewExtendedFacingValid(ExtendedFacing alignment) {
        return isNewExtendedFacingValid(
                alignment.getDirection(),
                alignment.getRotation(),
                alignment.getFlip());
    }

    class Builder {
        protected final boolean[] validStates = new boolean[STATES_COUNT];

        public static Builder allowAll() {
            Builder b = new Builder();
            Arrays.fill(b.validStates, true);
            return b;
        }

        public static Builder denyAll() {
            Builder b = new Builder();
            Arrays.fill(b.validStates, true);
            return b;
        }

        public Builder deny(ForgeDirection fd) {
            ExtendedFacing.getAllWith(fd).stream().mapToInt(ExtendedFacing::getIndex).forEach(v -> validStates[v] = false);
            return this;
        }

        public Builder allow(ForgeDirection fd) {
            ExtendedFacing.getAllWith(fd).stream().mapToInt(ExtendedFacing::getIndex).forEach(v -> validStates[v] = true);
            return this;
        }

        public Builder deny(ExtendedFacing o) {
            validStates[o.getIndex()] = false;
            return this;
        }

        public Builder allow(ExtendedFacing o) {
            validStates[o.getIndex()] = true;
            return this;
        }

        public Builder deny(Rotation fd) {
            ExtendedFacing.getAllWith(fd).stream().mapToInt(ExtendedFacing::getIndex).forEach(v -> validStates[v] = false);
            return this;
        }

        public Builder allow(Rotation fd) {
            ExtendedFacing.getAllWith(fd).stream().mapToInt(ExtendedFacing::getIndex).forEach(v -> validStates[v] = true);
            return this;
        }

        public Builder deny(Flip fd) {
            ExtendedFacing.getAllWith(fd).stream().mapToInt(ExtendedFacing::getIndex).forEach(v -> validStates[v] = false);
            return this;
        }

        public Builder allow(Flip fd) {
            ExtendedFacing.getAllWith(fd).stream().mapToInt(ExtendedFacing::getIndex).forEach(v -> validStates[v] = true);
            return this;
        }

        public Builder filter(Function<ExtendedFacing, Optional<Boolean>> predicate) {
            for (ExtendedFacing value : ExtendedFacing.VALUES) {
                predicate.apply(value).ifPresent(bool -> validStates[value.getIndex()] = bool);
            }
            return this;
        }

        public Builder ensureDuplicates() {
            for (ExtendedFacing value : ExtendedFacing.VALUES) {
                if (validStates[value.getIndex()]) {
                    validStates[value.getDuplicate().getIndex()] = true;
                }
            }
            return this;
        }

        /**
         * Prefers rotation over flip, so both flip will get translated to opposite rotation and no flip
         *
         * @param flip the preferred flip to be used Horizontal or vertical
         * @return this
         */
        public Builder ensureNoDuplicates(Flip flip) {
            if (flip == Flip.BOTH || flip == Flip.NONE) {
                throw new IllegalArgumentException("Preffered Flip must be Horizontal or Vertical");
            }
            flip = flip.getOpposite();
            for (ExtendedFacing value : ExtendedFacing.VALUES) {
                if (validStates[value.getIndex()]) {
                    if (value.getFlip() == Flip.BOTH || value.getFlip() == flip) {
                        validStates[value.getIndex()] = false;
                        validStates[value.getDuplicate().getIndex()] = true;
                    }
                }
            }
            return this;
        }

        public IAlignmentLimits build() {
            return new AlignmentLimits(validStates);
        }
    }
}
