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

package nl.bioinf.lscheffer_wvanhelvoirt.HadoopPhotonImaging;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * CoordinatesMapper
 *
 * The Mapper class will receive one RecordReader, and process it. One RecordReader contains one image, from which the
 * photons will be counted. The xy coordinates will be the key and count the value.
 *
 * @author Lonneke Scheffer and Wout van Helvoirt
 */
public class CoordinatesMapper extends Mapper<NullWritable, BytesWritable, Text, IntWritable> {

    /** Text for the xy coordinates. */
    private Text coordinates = new Text();
    /** IntWritable with count. */
    private final IntWritable count = new IntWritable(1);

    /**
     * Override method that processes one RecordReader item and send it's output to the reducing step.
     *
     * @param key     NullWritable because key will not be used.
     * @param value   BytesWritable containing a byte array from one input image.
     * @param context Context containing job information.
     * @throws IOException          When something went wrong.
     * @throws InterruptedException When connection was interrupted.
     */
    @Override
    public void map(NullWritable key, BytesWritable value, Context context)
            throws IOException, InterruptedException {

        // Get the configuration and initialize the PhotonImageProcessor class.
        Configuration conf = context.getConfiguration();

        // Try to instantiate the processor.
        try {
            PhotonImageProcessor pip = new PhotonImageProcessor(new ByteArrayInputStream(value.getBytes()),
                    conf.getInt("tolerance", 100), conf.get("method", "Fast"), conf.getBoolean("preprocessing", true));

            // Run the PhotonImageProcessor on the given image and retrieve the xy coordinates.
            for (int[] cor : pip.createPhotonCountMatrix()) {

                // Write output to context.
                this.coordinates.set(cor[0] + "|" + cor[1]);
                context.write(this.coordinates, this.count);
            }
        } catch (NullPointerException e) {

            // If error on file, skip this map.
        }
    }
}
