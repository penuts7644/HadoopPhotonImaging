# Hadoop Photon Imaging #

---------------------

### What is this repository for? ###

* Authors: Lonneke Scheffer & Wout van Helvoirt
* Version: 1.0
* This project is a modified version of the PhotonImaging plug-in for ImageJ. The Hadoop version is able to process
single photon event data, by locating the center point of each photon. The reduce stage combines all the two D arrays
from the mappers into one 16-bit greyscale image. Each pixel contains the amount of found photons and are mapped to the
correct pixel value.

### How do I get set up? ###

* You need a Hadoop enabled cluster and a Hadoop client from which you can run this program. More information about
Hadoop can be found [Here](http://hadoop.apache.org).
* This software requires at least [Java 7](https://www.oracle.com/downloads/index.html) to function.
* The source has been written in IntelliJ IDEA 2016.

### How do I use this application? ###

The jar file can be run via the Hadoop client's command-line. With the command below, you can run the program.

    yarn jar HadoopPhotonImaging.jar nl.bioinf.lscheffer_wvanhelvoirt.HadoopPhotonImaging.ParallelPhotonImageProcessor
    -D input.files=[input file/files]
    -D output.dir=[output directory]
    -D mapreduce.job.name=[job name]
    -D method=[method type]
    -D tolerance=[amount of noise tolerance]
    -D preprocessing=[true or false]

The command consists out of:

* Main Hadoop yarn command, path to the jar file and main class location.
* Required: The input file or files in an directory. Only TIFF files will be used.
* Required: An output directory, which will contain the created a PNG file.
* Optional: Set the job name (mapreduce.job.name). Default value is 'PhotonImageProcess'.
* Optional: Set the method type (method), 'Fast', 'Accurate' and 'Subpixel'. Default value is 'Fast'.
* Optional: Set the noise tolerance (tolerance). Default value is '100'.
* Optional: Enable preprocessing (preprocessing), 'true' or 'false'. Default value is 'true'.

### Future idea's ###

* Look into removing Sorting option.
* User definable, amount of images to be processed by one mapper.
* Looking into image processing interface's like [HIPI](http://hipi.cs.virginia.edu), which could greatly improve
efficiency of storing the image files on the HDFS as well as the creation of the splits/readers, mapping and reducing
stage.