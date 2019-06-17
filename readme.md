# Gutter Synthesis
A Max-based synthesis program exploring a set of eight coupled [Duffing Oscillators](https://en.wikipedia.org/wiki/Duffing_equation).

Requires the following max objects to be in your search path:
- Peter Elsea’s [Lobjects](http://peterelsea.com/lobjects.html) (Lsub, Ladd, Lmult)
- 

Requires the [NESS binary](http://www.ness.music.ed.ac.uk/music-and-tools/releases):
- Download and unzip
- place the "ness-brass" binary inside the Ness-Brass-Interface folder

Currently OS X only (probably) as it relies on the [shell.mxo object](https://github.com/jeremybernstein/shell/releases/tag/1.0b2)

# How to use

The interface allows you to create score and instrument files, and to create and listen to the audio files within Max.
Edit the score parameters or instrument parameters in the score and instrument builder windows, then hit the green button to build the score and instrument files. Hit the button again to actually generate the audio.

![main interface](http://tommudd.co.uk/ness/brass_main.png)

![score builder interface](http://tommudd.co.uk/ness/brass_score.png)

![instrument builder interface](http://tommudd.co.uk/ness/brass_instrument.png)
