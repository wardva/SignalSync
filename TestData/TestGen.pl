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
	`sox "$outputName.wav" "$outputName\_20.wav" trim 0.020 =$length`;
	`sox "$outputName.wav" "$outputName\_80.wav" trim 0.080 =$length`;
	`sox "$outputName.wav" "$outputName\_300.wav" trim 0.300 =$length`;
}

closedir($musicDir);
