package be.signalsync.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import be.signalsync.teensy.TeensyConverter;
import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class StreamSetFactory {
	public static StreamSet createFromFiles(String... paths) {
		try {
			List<StreamGroup> streamGroups = new ArrayList<>();
			for(String path : paths) {
				File file = new File(path);
				AudioDispatcher audioStream = AudioDispatcherFactory.fromFile(file, Config.getInt(Key.NFFT_BUFFER_SIZE), 0);
				StreamGroup group = new StreamGroup();
				group.setDescription(file.getName());
				group.setAudioStream(audioStream);
				streamGroups.add(group);
			}
			StreamSet streamSet = new StreamSet(streamGroups);
			return streamSet;
			
		} 
		catch (UnsupportedAudioFileException | IOException e) {
			throw new IllegalArgumentException("Invalid path or file passed to the createFromFile method.");
		}
	}
	
	public static StreamSet createRecordedStreamSet() {
		return StreamSetFactory.createFromFiles(
			"./testdata/Recorded/opname-reference.wav",
			"./testdata/Recorded/opname-1.wav", 
			"./testdata/Recorded/opname-2.wav", 
			"./testdata/Recorded/opname-3.wav");
	}
	
	public static StreamSet createCleanStreamSet() {
		return StreamSetFactory.createFromFiles(
			"./testdata/Clean/Sonic Youth - Star Power_90_0hz.wav", 
			"./testdata/Clean/Sonic Youth - Star Power_0_0hz.wav",
			"./testdata/Clean/Sonic Youth - Star Power_2000_0hz.wav", 
			"./testdata/Clean/Sonic Youth - Star Power_300_0hz.wav");
	}
	
	public static StreamSet createRecordedTeensyStreamSet() {
		return StreamSetFactory.createFromFiles(
			"./testdata/TeensyRecorded/teensy test soundboard 3.wav",
			"./testdata/TeensyRecorded/origineel test soundboard 3.wav");
	}
	
	public static StreamSet createTestTeensyStreamSet() {
		StreamSet streamSet = StreamSetFactory.createFromFiles("./testdata/Origineel/starpower11k.wav");
		StreamGroup teensyStreamGroup = new StreamGroup();
		teensyStreamGroup.setDescription("Live teensy stream");
		TeensyConverter teensy = new TeensyConverter();
		teensyStreamGroup.setAudioStream(teensy.getAudioDispatcher(0));
		streamSet.addStreamGroup(teensyStreamGroup);
		return streamSet;
	}
}
