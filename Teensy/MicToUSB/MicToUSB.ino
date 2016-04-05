/*
 * A simple hardware test which receives audio on the A2 analog pin
 * and sends it to the PWM (pin 3) output and DAC (A14 pin) output.
 *
 * This example code is in the public domain.
 */

#include <Audio.h>
#include <Wire.h>
#include <SPI.h>
#include <SD.h>
#include "resample.h"

AudioInputI2S      audioInput;         // audio shield: mic or line-in
AudioOutputI2S     audioOutput;        // audio shield: headphones & line-out
AudioConnection    patchCordEffectToOutputRight(audioInput,0, audioOutput,1); //connect mono audio input to right channel audio output
AudioConnection    patchCordEffectToOutputLeft(audioInput,0, audioOutput,0);


Resample           resample;          //xy=317,123
AudioConnection    patchCord1(audioInput, resample);

AudioControlSGTL5000 audioShield;

unsigned long last_time = millis();
int16_t audioBuffer[65];
int readIndex=0;
int writeIndex=0;
boolean started =false;
int timeIndex = 0;

void setup() {
  Serial.begin(115200);
  
  //only use pin 16 and 17
  //see https://www.pjrc.com/store/teensy3_audio.html
  pinMode(16, INPUT);
  pinMode(17, INPUT);
  
  //set analog read resolution
  analogReadResolution(13);
  
  // Audio connections require memory to work.  For more
  // detailed information, see the MemoryAndCpuUsage example
  AudioMemory(40);
  
  // Enable the audio shield and set the output volume.
  audioShield.enable();
  audioShield.inputSelect(AUDIO_INPUT_MIC);
  audioShield.volume(0.5);
}

void loop() {
  if (resample.available()) {
    started = true;
    int16_t* resampledAudioBlock = resample.readBuffer();
    
    //send data at 334Hz sync is only guaranteed for 1/334s 
    int value16 = analogRead(16);
    int value17 = analogRead(17);
    
    for(int i = 0 ; i < 32 ;i++){ 
      Serial.print("T");
      Serial.print(timeIndex,HEX);
      
      Serial.print(" ");
      //map to 16 to 13 bits!
      Serial.print((resampledAudioBlock[i] + 32768) >> 3,HEX);
      
      Serial.print(" ");
      Serial.print(value16,HEX);
      
      Serial.print(" ");
      Serial.print(value17,HEX);
      
      Serial.println();
      timeIndex++;
    }
  }
}


