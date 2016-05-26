//Imports
public class mainclass {
   public static void main(String[] args) {
      //Een stream aanmaken van een microfoon
      AudioDispatcher micDispatcher = 
         AudioDispatcherFactory.fromDefaultMicrophone(512, 0);

      Stream micStream = 
         new AudioDispatcherStream(micDispatcher);

      //Er kan een datastream met sensorData aan de microfoon 
      //gekoppeld worden. Waar deze datastream van afkomstig is 
      //hangt af van de implementatie van de klasse Stream.
      Stream dataStreamAttachedToMicrophone = ...
      
      //Een stream aanmaken van een bestand
      File file = new File("recorded.wav");
      AudioDispatcher fileDispatcher = 
         AudioDispatcherFactory.fromFile(file, 512, 0);
      Stream fromFileStream = 
         new AudioDispatcherStream(fileDispatcher);
      
      //Per gekoppelde groep streams een StreamGroup aanmaken
      StreamGroup micGroup = new StreamGroup();
      micGroup.setDescription("The microphone streamgroup");
      micGroup.setAudioStream(micStream);
      micGroup.addDataStream(dataStreamAttachedToMicrophone);
      
      StreamGroup fileGroup = new StreamGroup();
      fileGroup.setDescription("The file streamgroup");
      fileGroup.setAudioStream(fromFileStream);
      
      //Van alle StreamGroups een StreamSet aanmaken
      StreamSet allStreams = new StreamSet();
      allStreams.addStreamGroup(micGroup);
      allStreams.addStreamGroup(fileGroup);
      
      //De latencies van alle Streams uit de StreamSet bepalen met behulp van een
      //RealtimeSignalSync object.
      RealtimeSignalSync syncer = new RealtimeSignalSync(allStreams);
      
      //Een anonieme inner-class registeren als SyncEventListener.
      syncer.addEventListener(new SyncEventListener() {
         @Override
         public void onSyncEvent(Map<StreamGroup, LatencyResult> latencies) {
            //Resultaten afdrukken
            for(Entry<StreamGroup, LatencyResult> entry : latencies.entrySet()) {
               StreamGroup group = entry.getKey();
               LatencyResult result = entry.getValue();
               
               System.out.println(group.getDescription() + ": ");
               System.out.println(result.toString());
            }
         }
      });
      
      //Alle streams starten
      allStreams.start();
   }
}

