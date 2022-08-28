# StructureLib Developer Guide v0.1-SNAPSHOT

[//]: # (TODO move these to javadoc )
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
We will also cover a few common utilities here.

### `ofChain`

`ofChain` allows you to compose different `IStructureElement` to form a **OR** chain.
As with any other OR operator, this one exhibits short-circuiting behavior, i.e. it will not call next structure element
if previous one succeeded. (*)

This allows you e.g. accept both a glass block using `ofBlock()` and a piece of air using `ofAir()`.
It will not attempt to try next structure element if it errors though.

(*): For survival auto place, it will
* REJECT, if all structure element REJECT
* SKIP, if 1 or more structure element SKIP and the rest structure element (0 or more) REJECT
* any other result, **immediately** upon any structure element returns these other results.
This behavior is not 100% fixed and might change later on, but we will send the notice with best effort.

### `defer` and `lazy`

`defer` will defer the actual instantiation of structure element until the structure code is actually called.
`lazy` will defer the actual instantiation of structure element until the **first time** structure code is actually called.

These both allow the structure element **constructor** to access properties only present on the context object
(e.g. GT5 multiblock controller), e.g. hatch texture index to use.

Use `lazy` if the data you access tends to remain constant.
Using `defer` will not break per se, but would incur unnecessary performance overhead.

Use `defer` if it might change for the same structure definition, e.g. if your structure might switch mode and change
some parameter, while basically remain the same shape.

### `onlyIf`

`onlyIf` will return false if given predicate returns false, or call the downstream structure element if otherwise.

Basically a `if` block for structure code.

### `ofBlock`, `ofBlockFlatMap`, `ofBlockAnyMeta`, `ofBlockUnlocalizedName`, `ofBlocksMap`

These are quite similar.
They accept a few predefined blocks as the structure element.
Their main difference is how these predefined blocks are supplied to the IStructureElement.

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
