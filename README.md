# DXARTS project 463

_For woodwind, voice and live electronics_

This is my project for DXARTS 463. An overview of the project is given below

## Requirements & Set-Up

### Classes 
This project requires the UGen ArrayMax, which is not included in this repository but is available through [sc3-plugins](https://github.com/supercollider/sc3-plugins/releases).

In /Classes/, these are the classes required for this project to work. You need to [install](http://doc.sccode.org/Guides/UsingExtensions.html) these classes for the project to work.

### Set-Up
In `final.sc`, in `~tree` variable, change the frequencies `\freq` and the gains `\amp` to better match your instrument. 


## Score

There are two scenes in this piece, toggled using Spacebar key. The last (third) scene is empty and silent.

There are four + three controls in this piece. The GUI will aid you with the controls.

There is no explicit score for this piece but here are general instructions to follow:

First Scene (four controls):
* The first and third note should be play extended
* The second note can be played both short and long. Its loudness will determine how much noise will result from the FM modulation. You can move your instrument close to the microphone.
* The fourth note can be played in both short and long. You should play short articulations at the beginning and longer succession towards the end.
* I choose to give a ritard at the end (at the fourth note), and then slowly traverse through the third, second then first note, and finally toggle the second Scene.

Second Scene (three controls):
* The first note controls the process gain. If it's measured to be over -30dB, the frozen reverb will accept signal into its tank one second later. The length of the accepted signal corresponds to how long the note is held over -30dB. This means that the longest signal it can take without overlap is 1 second.
* The fourth note (up) and fifth note (down) controls the pitchshifter inside the reverb tank. Like the first note, the pitchshift only happens for n-seconds if it sees an n-second fourth/fifth note measured to be over -30dB.
* You can improvise with these controls however you want. I personally choose to present the non-frozen reverb first, then introduce the frozen reverb. The distinction can be made clearer if you leave space between sounds for the inputs of non-frozen reverb. 
* An effect I used to end the piece exploits the distortion happens when you pitchshift things way too much. You can feed the frozen reverb some sounds, pitchshift it down then up to cause some artifacts, these sound very much like artificial storm-like sounds (low rumbles, impulse through reverb).

Personal procedure I used for second scene:
```
flute - "Hey" - flute - "Vsauce" (frozen) - flute (frozen) - 
improvisation -
(pitchdown - pitchup) until low rumbles are sufficiently loud with impulses -
"is there a storm?" - flute - "but what is a storm?" -
pitchdown to diminish most violent impulses and rumbles
```

## Tools

There are a few tools resulting from this project. I'm unsure if it will cooperate well with the rest of the ecosystem of SuperCollider but they work well for specific projects that don't require certain tasks. 

### KSpectro

to be written

### KTree

to be written

### Datorro Reverb

An implementation of the Datorro reverb (cite) is included in `sdefs.sc`. It has some modification such as the pitchshift in the tank.

## License

Public Domain (where applicable)

