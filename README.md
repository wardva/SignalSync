# SignalSync
This project contains:
 * the source code of my master thesis research project: Java code from the library, Teensy/Arduino code, Max/MSP patches.
 * the raw LaTeX code of my thesis.

## Abstract

Many experiments executed at IPEM use sensors such as accelerometers and pressure sensors. A common problem after these experiments is synchronization of data of each sensor. The current synchronization system (developed by Joren Six) requires that each sensor is connected to a microphone recording the sound of the environment. With techniques like acoustic fingerprinting and calculating the cross-covariance of the signals, latency can be detected very accurately. The current synchronization system is performed as a post-processing step. A real-time and more user-friendly solution is desirable. The task for my master's thesis was to research if this is possible. I changed and optimized the synchronization algorithms in order to make them usable in real-time. I also wrapped the whole system in a Max/MSP patch so the synchronization can be executed without writing a single line of code.
