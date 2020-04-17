<p align="center">
  <img width="400px" src="https://novid-assets.s3.eu-central-1.amazonaws.com/media/logo/novid20-logo.svg"/>
</p>

# NOVID20 Android SDK

This is the source code of the SDK powering the NOVID20 Android apps. Note: This will not build as secrets and keys are missing but other than that the codebase is complete.

More info: [NOVID20.org](https://novid20.org)

## Authors
* [Christoph Kührer](https://christoph.tech/)
* [Dipl. Ing. Florian Knoll, Bsc.](http://www.mss-knoll.at/)


## Integrate the SDK

1. Setup your app module `build.gradle` file
```
...
dependencies {
    implementation project(':novid-sdk')
    ...
}
...
```
2. Initialize the `NovidSdk` in your `Application` class
```
import org.novid20.sdk.NovidSdk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ...
        
        ...
        NovidSdk.initialize(
            this, 
            yourAccessToken, 
            DEFAULT_BLE_DETECTION_CONFIG, 
            yourDeviceDataProvider)
    }
    
    private val DEFAULT_BLE_DETECTION_CONFIG = BleDetectionConfig(
        namePrefix = "nvSDK-",
        appUuid = UUID.fromString("de65c482-7a45-11ea-bc55-0242ac130003"),
        serviceUuid = UUID.fromString("e9143e04-7a45-11ea-bc55-0242ac130003"),
        characteristicUuid = UUID.fromString("f0626dc0-7a45-11ea-bc55-0242ac130003")
    )
}
```
3. Whenever you want to use the SDK you can retrieve it using the static function:
```
NovidSdk.getInstance()
```


## License
This project is released under the GNU General Public License v3.0, see [LICENSE.txt](./LICENSE.txt) for more details.