use strict;
use warnings;
my $musicDirName = './Muziek';
my $outputDirName = './TestBestanden';
opendir(my $musicDir, $musicDirName) or die 'Could not open directory';

while(readdir($musicDir)) {
	next unless m{(.+).mp3$};
	my $file = "$musicDirName/$_";
	my $name = $1;
	my $outputName = "$outputDirName/$name";
	#Omzetten naar WAVE-file
	`ffmpeg -i "$file" -ar 8000 -ac 1 "$outputName.wav" 2>&1`;
	#Getting information
	(grep { /Length/ } split "\n", `sox "$outputName.wav" -n stat 2>&1`)[0] =~ /(\d+\.?\d*)/;
	my $length = $1;

	#different tests

	# 0ms latency
	`sox "$outputName.wav" "$outputName\_0\_0hz.wav"`;
	`sox "$outputName.wav" -p synth $length sin 50 vol 0.8 | sox -m "$outputName.wav" - "$outputName\_0\_50hz.wav"`;
	`sox "$outputName.wav" -p synth $length sin 100 vol 0.2 | sox -m "$outputName.wav" - "$outputName\_0\_100hz.wav"`;

	# 20ms latency
	`sox "$outputName.wav" "$outputName\_20\_0hz.wav" trim 0.020 =$length`;
	`sox "$outputName.wav" -p synth $length sin 50 vol 0.8 | sox -m "$outputName.wav" - "$outputName\_20\_50hz.wav" trim 0.020 =$length`;
	`sox "$outputName.wav" -p synth $length sin 100 vol 0.2 | sox -m "$outputName.wav" - "$outputName\_20\_100hz.wav" trim 0.020 =$length`;
	
	# 80ms latency
	`sox "$outputName.wav" "$outputName\_80\_0hz.wav" trim 0.080 =$length`;
	`sox "$outputName.wav" -p synth $length sin 50 vol 0.8 | sox -m "$outputName.wav" - "$outputName\_80\_50hz.wav" trim 0.080 =$length`;
	`sox "$outputName.wav" -p synth $length sin 100 vol 0.2 | sox -m "$outputName.wav" - "$outputName\_80\_100hz.wav" trim 0.080 =$length`;
	
	# 90ms latency
	`sox "$outputName.wav" "$outputName\_90\_0hz.wav" trim 0.090 =$length`;
	`sox "$outputName.wav" -p synth $length sin 50 vol 0.8 | sox -m "$outputName.wav" - "$outputName\_90\_50hz.wav" trim 0.090 =$length`;
	`sox "$outputName.wav" -p synth $length sin 100 vol 0.2 | sox -m "$outputName.wav" - "$outputName\_90\_100hz.wav" trim 0.090 =$length`;
	
	# 300ms latency
	`sox "$outputName.wav" "$outputName\_300\_0hz.wav" trim 0.300 =$length`;
	`sox "$outputName.wav" -p synth $length sin 50 vol 0.8 | sox -m "$outputName.wav" - "$outputName\_300\_50hz.wav" trim 0.300 =$length`;
	`sox "$outputName.wav" -p synth $length sin 100 vol 0.2 | sox -m "$outputName.wav" - "$outputName\_300\_100hz.wav" trim 0.300 =$length`;
	
	# 2000ms latency
	`sox "$outputName.wav" "$outputName\_2000\_0hz.wav" trim 2.000 =$length`;
	`sox "$outputName.wav" -p synth $length sin 50 vol 0.8 | sox -m "$outputName.wav" - "$outputName\_2000\_50hz.wav" trim 2.000 =$length`;
	`sox "$outputName.wav" -p synth $length sin 100 vol 0.2 | sox -m "$outputName.wav" - "$outputName\_2000\_100hz.wav" trim 2.000 =$length`;
}

closedir($musicDir);
