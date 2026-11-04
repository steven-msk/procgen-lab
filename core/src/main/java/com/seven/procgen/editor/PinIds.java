package com.seven.procgen.editor;

/// Maps editor objects to the unique long ids imgui-node-editor supports.
/// - node id:  KIND_NODE | nodeId
/// - pin id:   KIND_PIN  | nodeId << 8 | slot   (slot 0 = the output pin, slots 1.. = inputs, in spec order)
/// - link id:  KIND_LINK | the id bits of the input pin it ends on. An input pin holds at most one link.
final class PinIds {
    private static final int KIND_SHIFT = 56;
    private static final long KIND_MASK = 0xFFL << KIND_SHIFT;
    private static final long NODE = 1L << KIND_SHIFT;
    private static final long PIN = 2L << KIND_SHIFT;
    private static final long LINK = 3L << KIND_SHIFT;
    private static final int SLOT_BITS = 8; // up to 255 inputs per node

    private PinIds() {
    }

    static long node(int nodeId) {
        return NODE | nodeId;
    }

    static int nodeOf(long nodeObjectId) {
        return (int)(nodeObjectId & 0xFFFFFFFFL);
    }

    static long output(int nodeId) {
        return PIN | ((long)nodeId << SLOT_BITS);
    }

    static long input(int nodeId, int inputIndex) {
        return PIN | ((long)nodeId << SLOT_BITS) | (inputIndex + 1);
    }

    static boolean isPin(long id) {
        return (id & KIND_MASK) == PIN;
    }

    static boolean isOutput(long pinId) {
        return slot(pinId) == 0;
    }

    static int pinNode(long pinId) {
        return (int)((pinId & ~KIND_MASK) >> SLOT_BITS);
    }

    /// Index into NodeSpec.inputs(). Only meaningful for input pins.
    static int inputIndex(long pinId) {
        return slot(pinId) - 1;
    }

    static long link(long inputPinId) {
        return (inputPinId & ~KIND_MASK) | LINK;
    }

    static long linkInputPin(long linkId) {
        return (linkId & ~KIND_MASK) | PIN;
    }

    private static int slot(long pinId) {
        return (int)(pinId & ((1L << SLOT_BITS) - 1));
    }
}
