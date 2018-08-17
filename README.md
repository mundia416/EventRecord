# eventrecord
library to record different types of events and be able to play them back. it works by storing a Plain Old java objects into an sql database and playing them back at exact time intervals as they were inserted in.

## Usage

add jitpack to your project dependency
```
repositories {
     maven { url='https://jitpack.io'}
}    
```

add the library to your module dependency
```
dependencies {
    implementation 'com.github.mundia416:eventrecord:{LATEST_RELEASE}'
}
```

## Recorder
Recording is done by the ActionRecorder class
instantiate the recorder you want to use parameterised with a type parameter of the POJO(data class) you want to use to store data. i.e
```
        recorder = ActionRecorder<RecordingData>(this)
```

notify the recorder when the state changes (when values in the POJO(data class) change
```
             val recordingData = RecordingData(x, y, isChecked)
            recorder.actionPerformed(recordingData)
```
## stop recording
```
 actionRecorder.stopRecording()
```

## start playback. 
recorded trigger events are recieved in the onTrigger() method of the ActionTriggerListener. the data
is recieved as a JsonObject so using google Gson library you can convert the jsonObject into your POJO(data class) object

```
    
        val actionTrigger = object: ActionTriggerListener{
            override fun onTrigger(data: JsonObject) {
                val recordingData = Gson().fromJson<RecordingData>(data,RecordingData::class.java)
                
            }

        }
})
```

## Add a Recorder Callback
add a recorder callback to recieve events of the actions that the recorder is taking
```
actionRecorder.addRecorderCallback(object : RecorderCallback{})
```

## Add Recorder Callback on multiple recorders
you can add the same recorder callback on multiple recorders,in this situation the methods in the callback will execute multiple times
as each recorder will make its own call to the method in the callback.if you want the method to only be called once
then you need to add the Recorder callback using the RecorderManager 
```
RecorderManager.getInstance(this).setRecorderCallback(arrayOf(movementRecorder,actionRecorder),object : RecorderCallback{})
```
## Author

Mundia Mundia 



## License

Copyright 2018 Mundia Mundia

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


