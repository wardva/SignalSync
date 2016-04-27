package be.signalsync.stream;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import be.signalsync.util.Config;
import be.signalsync.util.Key;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;

public class StreamSetFactory {
	public static StreamSet createFromFiles(String... paths) {
		List<StreamGroup> streamGroups = new ArrayList<>();
		for(String path : paths) {
			File file = new File(path);
			AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(path, Config.getInt(Key.SAMPLE_RATE), Config.getInt(Key.NFFT_BUFFER_SIZE), 0);			Stream stream = new AudioDispatcherStream(dispatcher);
			StreamGroup group = new StreamGroup();
			group.setDescription(file.getName());
			group.setAudioStream(stream);
			streamGroups.add(group);
		}
		StreamSet streamSet = new StreamSet(streamGroups);
		return streamSet;
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
			"./testdata/Clean/Sonic Youth - Star Power_2000_0hz.wav", 
			"./testdata/Clean/Sonic Youth - Star Power_300_0hz.wav",
			"./testdata/Clean/Sonic Youth - Star Power_0_0hz.wav");
	}
	
	public static StreamSet createRecordedTeensyStreamSet() {
		return StreamSetFactory.createFromFiles(
			"./testdata/TeensyRecorded/teensy test soundboard 3.wav",
			"./testdata/TeensyRecorded/origineel test soundboard 3.wav");
	}
}
