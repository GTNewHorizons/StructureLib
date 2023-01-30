package com.gtnewhorizon.structurelib;

import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import cpw.mods.fml.common.eventhandler.Event;

/**
 * Subscribers are <b>required</b> to not keep reference to these events, nor to depend on values of these events after
 * the method call returns.
 * <p>
 * All of these events will be fired on the {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS}, if they are
 * enabled by {@link StructureLibAPI#enableInstrument(Object)}.
 * <p>
 * All public stuff in this class and its enclosed classes are considered part of the public API. Backwards
 * compatibility is maintained to the maximum extend possible.
 */
public abstract class StructureEvent extends Event {

    /*
     * In case the event is passed to another thread for processing.
     */
    private final Object identifier = StructureLibAPI.instrument.get();

    StructureEvent() {}

    /**
     * The instrument identifier. Subscribers should filter by these identifiers to prevent listening to a structure
     * event for other people.
     */
    public Object getInstrumentIdentifier() {
        return identifier;
    }

    /**
     * Fired <b>just before</b> any structure element is visited.
     */
    public static final class StructureElementVisitedEvent extends StructureEvent {

        private final World world;
        private final int x, y, z;
        private final int a, b, c;
        private final IStructureElement<?> element;

        /**
         * Fire this event on {@link MinecraftForge#EVENT_BUS} by populating it with given arguments. Do nothing if
         * instrumenting isn't enabled for current thread.
         * <p>
         * Any {@link Exception} thrown by event listeners will be logged and ignored. {@link Error} will be propagated
         * as is.
         */
        public static void fireEvent(World world, int x, int y, int z, int a, int b, int c,
                IStructureElement<?> element) {
            if (StructureLibAPI.isInstrumentEnabled()) {
                try {
                    MinecraftForge.EVENT_BUS.post(new StructureElementVisitedEvent(world, x, y, z, a, b, c, element));
                } catch (Exception e) {
                    // logging is done by event bus, we just ignore the consequence here
                }
            }
        }

        StructureElementVisitedEvent(World world, int x, int y, int z, int a, int b, int c,
                IStructureElement<?> element) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.a = a;
            this.b = b;
            this.c = c;
            this.element = element;
        }

        /**
         * A position in ABC coordinate, relative to (0,0,0) in the piece, i.e. without baseOffset
         * <p>
         * Submit a feature request if you need the baseOffset.
         */
        public int getA() {
            return a;
        }

        /**
         * B position in ABC coordinate, relative to (0,0,0) in the piece, i.e. without baseOffset
         * <p>
         * Submit a feature request if you need the baseOffset.
         */
        public int getB() {
            return b;
        }

        /**
         * C position in ABC coordinate, relative to (0,0,0) in the piece, i.e. without baseOffset
         * <p>
         * Submit a feature request if you need the baseOffset.
         */
        public int getC() {
            return c;
        }

        /**
         * Element being visited.
         */
        public IStructureElement<?> getElement() {
            return element;
        }

        @Override
        public String toString() {
            return "StructureElementVisitedEvent{" + "a="
                    + a
                    + ", b="
                    + b
                    + ", c="
                    + c
                    + ", element="
                    + element
                    + ", world="
                    + getWorld()
                    + ", x="
                    + getX()
                    + ", y="
                    + getY()
                    + ", z="
                    + getZ()
                    + '}';
        }

        /**
         * Location of this event.
         */
        public World getWorld() {
            return world;
        }

        /**
         * Location of this event. Absolute world coordinate.
         */
        public int getX() {
            return x;
        }

        /**
         * Location of this event. Absolute world coordinate.
         */
        public int getY() {
            return y;
        }

        /**
         * Location of this event. Absolute world coordinate.
         */
        public int getZ() {
            return z;
        }
    }
}
