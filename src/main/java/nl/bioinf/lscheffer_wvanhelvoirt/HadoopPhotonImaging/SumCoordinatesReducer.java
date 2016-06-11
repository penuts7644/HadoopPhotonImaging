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

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * SumCoordinatesReducer
 *
 * The Reducer class that combines the count after the shuffle stage from all the mappers.
 *
 * @author Lonneke Scheffer and Wout van Helvoirt
 */
public class SumCoordinatesReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

    /** intWritable with summed counts. */
    private IntWritable sumCount = new IntWritable();

    /**
     * Override method that processes all mapper outputs to one IntWritable.
     *
     * @param key     Text with xy coordinates.
     * @param values  Iterable with IntWritable items.
     * @param context Context containing job information.
     * @throws IOException          When something went wrong.
     * @throws InterruptedException When connection was interrupted.
     */
    @Override
    public void reduce(Text key, Iterable<IntWritable> values, Context context)
            throws IOException, InterruptedException {

        // Set sum default.
        int sum = 0;

        // For each count, combine.
        for (IntWritable value : values) {
            sum += value.get();
        }

        // Set the sumCount and return the key with the summed counts.
        this.sumCount.set(sum);
        context.write(key, this.sumCount);
    }
}