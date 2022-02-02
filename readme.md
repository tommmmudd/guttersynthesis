# Gutter Synthesis
A Max-based synthesis program exploring a set of eight coupled [Duffing Oscillators](https://en.wikipedia.org/wiki/Duffing_equation).

Standalone versions of the synth can be found via these links:
- [OS X standalone app](http://tommudd.co.uk/gutter/software/guttersynth_64bit_osx.zip). Note that for Apple M1 computers you will need to [update Java](https://www.azul.com/downloads/?os=macos&architecture=arm-64-bit&package=jdk).
- [Windows standalone exe](http://tommudd.co.uk/gutter/software/guttersynth_windows.zip)

(note: if links above do not allow a download, try right clicking the link and selecting "Copy Link Address", then pasting the url into a new tab)

There is a SuperCollider implementation available here (courtesy of Matt Kjelgaard and Scott Carver): https://github.com/madskjeldgaard/guttersynth-sc

Gutter synthesis is a purely digital synthesis process that creates very physical, acoustic-like sounds using a network of resonant Duffing oscillators. The software was created specifically for this project, and is included with the release as an equal part of the creative output. This version uses an interrelated set of eight Duffing oscillators and associated filter banks.

The synth was used for the [Entr’acte release](https://entracte.co.uk/projects/tom-mudd-e226/) of the same name

![Tom Mudd, Gutter Synthesis CD, 2018](http://tommudd.co.uk/images/gutter_border.png) 

## Troubleshooting
For more recent operating systems, you may need to copy the ```gutterOsc.class``` file out of the ```gutterOsc for Java1.6``` folder, and overwrite the existing file of the same name on the top level.
Note that for Apple M1 computers you will need to [update Java](https://www.azul.com/downloads/?os=macos&architecture=arm-64-bit&package=jdk).

![Gutter Synthesis software](https://cycling74-web-uploads.s3.amazonaws.com/5926c2e0d5374cf822907baa/2018-04-22T11:25:11Z/gutter-synthesis%20-%20image%20small.png)
