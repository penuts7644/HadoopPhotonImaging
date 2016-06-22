/*
 * Copyright (c) 2016 Lonneke Scheffer and Wout van Helvoirt
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.bioinf.lscheffer_wvanhelvoirt.HadoopPhoton;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * ImageFileRecordWriter
 *
 * This is a custom class to write the output of the Reducer to a png file.
 *
 * @author Lonneke Scheffer and Wout van Helvoirt
 */
public class ImageFileRecordWriter extends RecordWriter<Text, IntWritable> {

    /** The Configuration. */
    private final Configuration mConf;
    /** Output file path. */
    private final Path mOutputPath;
    /** The matrix for each xy count. */
    private int[][] countMatrix;

    /**
     * Implementation detail: This constructor is built to be called via
     * reflection from within FileRecordWriter.
     *
     * @param context The context for this task.
     */
    public ImageFileRecordWriter(TaskAttemptContext context) {
        this.mConf = context.getConfiguration();
        this.mOutputPath = new Path(this.mConf.get("output.dir"), "HadoopPhoton.png");
        this.countMatrix = new int[this.mConf.getInt("max.image.height", 0)][this.mConf.getInt("max.image.width", 0)];
    }

    /**
     * Override method that writes the Reducer output to a file.
     *
     * @param key   Text with xy location.
     * @param value IntWritable containing the count.
     * @throws IOException          Returns default exception.
     * @throws InterruptedException If connection problem.
     */
    @Override
    public void write(Text key, IntWritable value)
            throws IOException, InterruptedException {

        // Add the value count on the correct xy location.
        try {
            this.countMatrix[Integer.parseInt(key.toString().split("\\|")[0])]
                    [Integer.parseInt(key.toString().split("\\|")[1])] = value.get();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("The given max.image.width or max.image.height is to small.");
        }
    }

    /**
     * Closes any connection.
     *
     * @throws IOException          Returns default exception.
     * @throws InterruptedException If connection problem.
     */
    @Override
    public void close(TaskAttemptContext context)
            throws IOException, InterruptedException {

        // Set the filesystem and delete path if it exists.
        FileSystem hdfs = FileSystem.get(this.mConf);
        if (hdfs.exists(this.mOutputPath)) {
            hdfs.delete(this.mOutputPath, false);
        }

        // Get the buffered image from the PhotonImageProcessor and write it to a png file.
        BufferedImage bi = new PhotonImageProcessor().createBufferedImage(this.countMatrix);
        ImageIO.write(bi, "png", hdfs.create(this.mOutputPath));
        hdfs.close();
    }
}
