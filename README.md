# pdac_tech_assignment
The project represents five most popular colors of the image from camera input in real time.
## Architecrure:
* MVC design pattern
* Multy-activity architecture
* User can choose between Camera and Camera2 activities to view usage of old and new camera API accordingly
* Technologies and framework used: android.hardware.Camera API, android.hardware.camera2 API,Executors, HandlerThread, SurfaceView, Imagereaderm Yuv-Bitmap convertor
## About:
* The application gives user a choice of two actvities Camera and Camera2. Each uses corresponding camera API.
* There is 5 color boxes places on top or a side of the screen according screen rotation. Each of them represent one of 5 most popular colors of the image in current time.
* Each box colored with the target color. 
* Text inside the represents the ammount of certain color present in the image in percents. 
The text below the box represents RGB (Red, Green, Blue) colors in digital 8-bit per channel representation. (for example R:255 G:255 B:255)
