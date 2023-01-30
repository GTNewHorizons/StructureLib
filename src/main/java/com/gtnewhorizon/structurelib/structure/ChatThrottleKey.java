package com.gtnewhorizon.structurelib.structure;

class ChatThrottleKey {

    static class NoExplicitChannel {

        private static final int hashOffset = NoExplicitChannel.class.hashCode();
        private final String subchannel;

        public NoExplicitChannel(String subchannel) {
            this.subchannel = subchannel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NoExplicitChannel)) return false;

            NoExplicitChannel that = (NoExplicitChannel) o;

            return subchannel.equals(that.subchannel);
        }

        @Override
        public int hashCode() {
            // as this one will be placed within a map along with instances of different classes, it'd be best to add
            // an offset based off class type to avoid collision
            return subchannel.hashCode() + hashOffset;
        }
    }
}
