# Building
You can build this by cloning this repo and the submodule. You also need to add opencv to your classpath. Using eclipse this should work by importing this project and following this tutorial: http://docs.opencv.org/doc/tutorials/introduction/java_eclipse/java_eclipse.html

# Usage
You can pass the work dir using the first command line parameter. testloc.world will try to load any saved files in this folder. Creating new tests works by entering a URL of a maps image into the "Create File" popup. You can simulate GPS input by drawing on the globe. Clicking on the image inserts a marker at this point using the last provided GPS position. Using the step buttons and the slider you can move in the history of the current test. Clicking on the "Mode: Move"/"Mode: Draw" Button changes the effect of the left mouse button. The list on the left shows all found tests.

The triangle specific settings at the top can be disregarded and will probably be removed in the future.
