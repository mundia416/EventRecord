# eventrecord
library to record different types of events and be able to play them back

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
    implementation 'com.github.mundia416:eventrecord:0.0.9'
}
```

## Types Of Recorders
1.  OverlayMovementRecorder -
records and plays back the movements of an inflated overlay view
2.  ActionRecorder -
records trigger events and plays back those trigger events at exact time intervals


instantiate the recorder you want to use . i.e
```
val actionRecorder = ActionRecorder(context)
```

notify the recorder when the state changes
```
            actionRecorder.actionPerformed()
```
stop recording
```
 actionRecorder.stopRecording()
```

start playback. recorded trigger events are recieved in the onTrigger() method of the OnActionTriggerListener 
```
actionRecorder.startPlayback(object: OnActionTriggerListener{
    override fun onTrigger() {
        //do something
    }

    override fun onError(e: Throwable) {
        //handle error
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


