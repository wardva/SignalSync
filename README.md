# SignalSync
This project contains:
 * the source code of my master thesis research project: Java code from the library, Teensy/Arduino code, Max/MSP patches.
 * the raw LaTeX code of my thesis.

## Abstract

### English

Many experiments executed at IPEM use sensors such as accelerometers and pressure sensors. A common problem after these experiments is synchronization of data of each sensor. The current synchronization system (developed by Joren Six) requires that each sensor is connected to a microphone recording the sound of the environment. With techniques like acoustic fingerprinting and calculating the cross-covariance of the signals, latency can be detected very accurately. The current synchronization system is performed as a post-processing step. A real-time and more user-friendly solution is desirable. The task for my master's thesis was to research if this is possible. I changed and optimized the synchronization algorithms in order to make them usable in real-time. I also wrapped the whole system in a Max/MSP patch so the synchronization can be executed without writing a single line of code.

### Dutch

De meeste experimenten die aan het IPEM worden uitgevoerd maken gebruik van verschillende soorten sensoren (accelerometers, druksensoren,...). Een veelvoorkomend probleem is de de synchronisatie van de data van elke sensor. Bij het huidige synchronisatiesysteem wordt elke sensor verbonden met een microfoon die het omgevingsgeluid opneemt. Met technieken zoals acoustic fingerprinting en het berekenen van de kruiscovariantie kan de latency tussen de audiosignalen zeer nauwkeurig bepaald worden. Met deze latency kan vervolgens de sensordata gesynchroniseerd worden. Het huidige systeem kan enkel als naverwerking uitgevoerd worden. Een realtime en meer gebruiksvriendelijk systeem is erg gewenst. In dit onderzoek is er nagegaan of dit mogelijk is. Om de huidige synchronisatiealgoritmen in realtime bruikbaar te maken waren aanpassingen en optimalisaties nodig. Dit onderzoek heeft ook geleid tot enkele Max/MSP modules. Met behulp van deze modules is het mogelijk om het volledige synchronisatieproces in realtime uit te voeren in zonder het schrijven van één lijn code.
