# StructureLib

A standalone version of [TecTech structure api](https://github.com/GTNewHorizons/TecTech/tree/master/src/main/java/com/github/technus/tectech/mechanics/structure).

Originally created by TecTech authors, taken with permission. It's under MIT license anyway.

![permission](./.github/permission.png)

## Using

1. Add this to your build.gradle
    ```groovy
    dependencies {
      compile "com.github.GTNewHorizons:StructureLib:master-SNAPSHOT:deobf"
    }
    repositories {
        maven {
            name = "jitpack.io"
            url = "https://jitpack.io"
        }
    }
    ```
   Replace `master-SNAPSHOT` with a commit hash or a tag name (if any) of your choice to prevent unexpected upstream changes.
2. Add `required-after:structurelib;` to your `@Mod(dependencies = "..")` tring.
3. Add `structurelib` to your `mcmod.info`.

## Contribution

The project is developed using IDEA. Please manually configure the build to not delegate to gradle for textures to show up in dev.

## Current license
Due to me adding in ItemStackMap, which was derived from code from CodeChicken's LGPL mod NEI. This mod is effectively considered
licensed under LGPL in its entirety since commit ebef584b8c518f8202cfe0984fb28a6d23e52b3d or any squash commit containing it, e.g.
609e50428ee4e293f65f67f554a70b9fcfae97c4.
