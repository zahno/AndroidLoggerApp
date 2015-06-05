# AndroidLoggerApp

# Import and compile in Eclipse 
Import project into Eclipse: 
1. Right click in package explorer
2. Select "Import..."
3. Select "General/Existing Project into Workspace"
4. Select root directory of source code click "Finish".

After that: 
1. Right click on the Project and select "Properties"
2. Go to Java Build Path
3. Choose "Libraries" 
4. Add "AChartEngine/achartengine-1.0.0.jar" and "Android_Java_D2xx/d2xx.jar" from the libs folder (Javadocs can be found in the same folders in the folder "doc") 
5. Choose "Order and Export", select those jars and move them up below "AndroidLogger/src" and "AndroidLogger/gen" 

# Troubleshooting:
If Eclipse doesn't recognize the project's structure correctly, do this: 
1.Right click on the project and go to "Properties"
2.Select "Java Build Path" on the left
3.Open "Source" tab
4.Click "Add Folder..." and check "gen" and "src"

