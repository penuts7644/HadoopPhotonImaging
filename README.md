# Hadoop Photon Imaging #

---------------------

### About this project ###

* Authors: Lonneke Scheffer & Wout van Helvoirt
* Version: 1.0
* This project is a modified version of the PhotonImaging plug-in for ImageJ. The Hadoop version is able to process
single photon event data, by locating the center point of each photon. The reduce stage combines the mapper/shuffle
outputs and the output-formatter adds the found values to a two D array and writes one 16-bit greyscale image. Each
pixel contains the amount of found photons and are mapped to the correct pixel value.

### Getting set up ###

* You need a Hadoop enabled cluster and a Hadoop client from which you can run this program. More information about
Hadoop can be found [Here](http://hadoop.apache.org).
* This software requires at least [Java 7](https://www.oracle.com/downloads/index.html) to function.
* The source has been written in IntelliJ IDEA 2016 and the project uses Maven for package management and building.

### How to use this application ###

The jar file can be run via the Hadoop client's command-line. With the command below, you can run the program.

    yarn jar HadoopPhotonImaging-1.0-jar-with-dependencies.jar
    -D input.files=[input file/files]
    -D output.dir=[output directory]
    -D max.image.height=[the max height of your input images]
    -D max.image.width=[the max width of your input images]
    -D mapreduce.job.name=[job name]
    -D method=[method type]
    -D tolerance=[amount of noise tolerance]
    -D preprocessing=[true or false]

The command consists out of:

* Main Hadoop yarn command and the path to the jar file.
* Required: The input file or files in an directory. Only TIFF files will be used.
* Required: An output directory, which will contain the created a PNG file.
* Required: The maximum height of your input images.
* Required: The maximum width of your input images.
* Optional: Set the job name (mapreduce.job.name). Default value is 'PhotonImageProcess'.
* Optional: Set the method type (method), 'Simple', 'Accurate' and 'Subpixel'. Default value is 'Simple'.
* Optional: Set the noise tolerance (tolerance). Default value is 100.
* Optional: Enable pre-processing (preprocessing), 'true' or 'false'. Default value is 'true'.

### Troubleshooting ###

If you run want to run the Hadoop job using a Macintosh machine, you could get the following error:

    Exception in thread "main" java.io.IOException: Mkdirs failed to create /var/folders/1k/799h3b_s4pd87bg9d2mfv7k00000gn/T/hadoop-unjar7265077405644854771/META-INF/license
        at org.apache.hadoop.util.RunJar.ensureDirectory(RunJar.java:128)
        at org.apache.hadoop.util.RunJar.unJar(RunJar.java:104)
        at org.apache.hadoop.util.RunJar.unJar(RunJar.java:81)
        at org.apache.hadoop.util.RunJar.run(RunJar.java:209)
        at org.apache.hadoop.util.RunJar.main(RunJar.java:136)

This error can be fixed by removing 'META-INF/LICENSE' (note the capitals) from the jar file. This can be done by
executing the command below in the same directory where the jar file is located.

    zip -d HadoopPhotonImaging-1.0-jar-with-dependencies.jar META-INF/LICENSE

### Our use case ###

We used 300000 small TIFF files of about 650 Kb each. We first created a plugin for ImageJ to process these images,
which was able to process all the files within 40 minutes on one Intel Core i7 3770 chip. The Hadoop version creates a mapper
for each input file and is able to process one tenth of all the files, within 12 minutes. The cluster had 97 operating 8
core nodes available for this job.

So why does it take so long process the files on the cluster? As said, the files are very small and each file is
probably processed within a few seconds. If one mapper could receive multiple files, the efficiency could be improved
a lot.

### Future idea's ###

* A TIFF file writer that writes the output image as TIFF to the hdfs instead of png.
* Looking into a image processing interface's like [HIPI](http://hipi.cs.virginia.edu), which could greatly improve
efficiency of storing the image files on the HDFS as well as the creation of the splits/readers. Does not read TIFF
files though.