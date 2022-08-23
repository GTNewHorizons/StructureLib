# StructureLib Developer Guide v0.1-SNAPSHOT

## `IStructureDefinition`

`IStructureDefinition` is the structure definition of your multi. You will have one `IStructureDefinition` for each multi.

### Caching?

You will construct this definition once per multi, either at class load or at first call to `getStructureDefinition()`.
You can use a `ClassValue<IStructureDefinition<?>>` if you have a template for subclass.
You can also use `StructureUtility.defer()` for simpler cases, but it is slightly less performant as it will allocate new
objects on each structure check, whereas `ClassValue` will allocate once and cache it onwards.

### Constructing

IStructureDefinition is constructed using `IStructureDefinition#builder()`.

Builder has two main methods
1. `addShape` is a 2d string array describing the layout of structure. You can optionally use `StructureUtility#transpose()`
to transpose the array to make the structure look nicer in source code.
2. `addElement`. It takes a char identifier (mapped to a char in any of `addShape()` calls) and a IStructureElement.

## `IStructureElement`

`IStructureElement` are how you define which block will be accepted.
They are simple stateless classes. They will rely on the multiblock controller to store any states they need.
If you don't want to store these transient states in multiblock controller, you can use `StructureUtility#withContext()`

They will usually come from `StructureUtility`.
There is a vast library of `IStructureElement` there to cover basic needs.
For more complex usage, you can define your own subclass, e.g. GregTech 5 Unofficial defines `ofHatchAdder()`, `ofCoil()`,
`ofFrame()` and `buildHatchAdder()` in its `GT_StructureUtility` class.

Here we will detail a few more complex `IStructureElement` that comes with standard library.

### `ofBlocksTiered`

This assumes you have a series of block, e.g. T1 motor, T2 motor, etc. Your multi will accept any of them (__as long as
player does not mix them (*)__), but would like to know which is used.

(*): Implementation wise, this structure element will only accept a block if its tier is the same as existing tier or
if existing tier is unset (i.e. value of third argument)

The first argument is tier extractor.
You will need to provide a function to convert block+meta to a tier.
This tier cam be an Enum, an Integer, a String, or basically anything.
It doesn't even have to implement `Comparable`.
The choice is yours.
The extractor can return null and will never be passed a null block or an invalid block meta.

The second argument is a list of all blocks you know that you will accept.
This list is only for hint particle spawning and survival build.
The hint/autoplace code will choose 1st pair to spawn/place if hologram has [channel data](./channels.md) of 1,
2nd pair if 2, and so on.

The third argument is the value when the tier has not been set by structure code.
This means getter (5th argument) will usually return this upon first invocation for each round of structure check.

The fourth argument will notify the multi of a tier change.

The fifth argument is how multi notify the structure element about current tier.
This is necessary as all structure element are stateless themselves.
