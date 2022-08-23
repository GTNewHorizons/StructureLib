# Constructable Tool

The convention is to use the stack size of tool stack to pass tier info.

## Channels

When one multi have multiple tiered component that can have interleaving tier, this becomes an obnoxious restriction, as
one ItemStack cannot have two stack sizes at the same time.
Here we use the item NBT to bypass this restriction.

Basically, one hologram can contain multiple channels.
Each channel can be identified with lower case strings and has the value being one positive Java int.
There will be one and only one master channel and 0 or more sub channel.

When a non-existent sub channel is queried, master channel will be used instead to fill its place.

Structure element designers will usually not need to consider channels, i.e. proceed as if this concept does not exist, unless it must take input from multiple channels.

Multi Designers will consider how to map each channel to each tiered component.
He will be using `com.gtnewhorizon.structurelib.structure.StructureUtility.withChannel` to direct structure element on which channel to use.
Conceptually, it will return an ItemStack with the master channel being the value of selected channel.
In implementation, withChannel will return a new ItemStack with same item, nbt but different stack size.

In any case, it is NOT safe to mutate or query this ItemStack beyond its stack size.
What information this carries (beyond the stack size) is an implementation detail and should not be relied on.
__For the future of mankind please do not rely on this.__

### why is this called channel?

IDK man, just give me a better name and I'd do the renaming.

### Example: Multiblock Structure Designer

GT++ Chemical Plant is a multi with 4 distinctly tiered component.

It will use
* master channel for its outer shell
* channel `"casing"` for machine casings
* channel `"pipe"` for pipe casings
* channel `"coil"` for heating coils

Its structure code will then look like
```java
.addElement('a', shellStructureElement())
.addElement('b', withChannel("casing", machineCasingStructureElement()))
.addElement('c', withChannel("pipe", pipeCasingStructureElement()))
.addElement('d', withChannel("coil", coilStructureElement()))
```

whereas all these structure elements are designed without considering channels whatsoever

### Example: Multi channel input Structure Element

Suppose your hatch element has a required facing and can accept arbitrary meta tile entity, so it will need both meta id and facing input.


It will use
* master channel for its outer shell
* channel `"casing"` for machine casings
* channel `"pipe"` for pipe casings
* channel `"coil"` for heating coils

Its structure code will then look like
```java
.addElement('a', shellStructureElement())
.addElement('b', withChannel("casing", machineCasingStructureElement()))
.addElement('c', withChannel("pipe", pipeCasingStructureElement()))
.addElement('d', withChannel("coil", coilStructureElement()))
```

whereas all these structure elements are designed without considering channels whatsoever

