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

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.io.Opener;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * PhotonImageProcessor
 *
 * This class is able to process a single photon event image and combine multiple of the images to one hi-res image.
 * Each light point within the image (based on user given tolerance value) is being processed as photon. Each photon
 * has a center that can be calculated in a simple or a more accurate way. There are two accurate calculations available.
 * One to create a higher resolution image with four times the amount of pixels (sub-pixel resolution) or one with
 * normal resolution. Photons are being counted and mapped to the correct pixel values to create a 16-bit image.
 *
 * @author Lonneke Scheffer and Wout van Helvoirt
 */
public class PhotonImageProcessor {

    /** The ImageProcessor. */
    private ImageProcessor ip;
    /** LinkedList containing int arrays with xy positions with one count. */
    private LinkedList<int[]> coordinateCounts;
    /** Noise tolerance, default is 100. */
    private double tolerance;
    /** This boolean tells whether the user wants to perform pre-processing. */
    private boolean preprocessing;
    /** The output method (simple/accurate/sub-pixel resolution) is set to simple. */
    private String method;

    /**
     * Constructor used in the ImageFileOutputFormat class.
     */
    public PhotonImageProcessor() {
    }

    /**
     * Constructor used for the Mapper.
     */
    public PhotonImageProcessor(ByteArrayInputStream bais, int tolerance, String method, boolean preprocessing) {
        this.ip = new Opener().openTiff(bais, "tiff_image").getProcessor();
        this.coordinateCounts = new LinkedList<>();
        this.tolerance = tolerance;
        this.method = method;
        this.preprocessing = preprocessing;

        if (this.tolerance < 0) {
            this.tolerance = 0;
        }
    }

    /**
     * This method will start the core of this class and calculates the xy coordinates.
     *
     * @return LinkedList Containing int arrays with xy coordinates.
     */
    public LinkedList<int[]> createPhotonCountMatrix() {
        Polygon rawCoordinates;

        // Pre-process the current slice.
        if (this.preprocessing) {
            this.preprocessImage(this.ip);
        }

        // Find the photon coordinates.
        rawCoordinates = this.findPhotons(this.ip);

        // If previewing enabled, show found maxima's on slice.
        if (this.method.equals("Simple")) {
            this.processPhotonsSimple(rawCoordinates);
        } else {

            // Calculating the auto threshold takes relatively long so this function is only called once per image.
            if (this.method.equals("Accurate")) {
                processPhotonsAccurate(this.ip, rawCoordinates);
                // Else method equals "Subpixel resolution"
            } else {
                processPhotonsSubPixel(this.ip, rawCoordinates);
            }
        }

        // Return the LinkedList with coordinate arrays.
        return this.coordinateCounts;
    }

    /**
     * This method is called when processing photons using the 'simple' method.
     * All photons are added to the LinkedList as array, without altering.
     *
     * @param rawCoordinates A polygon containing the coordinates as found by MaximumFinder
     */
    private void processPhotonsSimple(final Polygon rawCoordinates) {

        // Loop through all raw coordinates and add them to the LinkedList as array.
        for (int i = 0; i < rawCoordinates.npoints; i++) {
            int[] intArray = {rawCoordinates.xpoints[i], rawCoordinates.ypoints[i]};
            this.coordinateCounts.add(i, intArray);
        }
    }

    /**
     * This method is called when processing photons using the 'accurate' method.
     * The exact coordinates are calculated, and then floored and added to the LinkedList as array.
     *
     * @param ip             The ImageProcessor of the current image slice
     * @param rawCoordinates A polygon containing the coordinates as found by MaximumFinder
     */
    private void processPhotonsAccurate(final ImageProcessor ip, final Polygon rawCoordinates) {
        for (int i = 0; i < rawCoordinates.npoints; i++) {

            // Loop through all raw coordinates, calculate the exact coordinates, floor these and add them to the
            // LinkedList as array.
            double[] exactCoordinates = this.calculateExactCoordinates(rawCoordinates.xpoints[i],
                    rawCoordinates.ypoints[i], ip);
            int[] intArray = {(int) exactCoordinates[0], (int) exactCoordinates[1]};
            this.coordinateCounts.add(i, intArray);
        }
    }

    /**
     * This method is called when processing photons using the 'subpixel resolution' method.
     * The exact coordinates are calculated, and then multiplied by two and added to the LinkedList as array.
     *
     * @param ip             The ImageProcessor of the current image slice
     * @param rawCoordinates A polygon containing the coordinates as found by MaximumFinder
     */
    private void processPhotonsSubPixel(final ImageProcessor ip, final Polygon rawCoordinates) {
        for (int i = 0; i < rawCoordinates.npoints; i++) {

            // Loop through all raw coordinates, calculate the exact coordinates, double these and add them to the
            // LinkedList as array.
            double[] exactCoordinates = this.calculateExactCoordinates(rawCoordinates.xpoints[i],
                    rawCoordinates.ypoints[i],
                    ip);
            int[] intArray = {(int) exactCoordinates[0] * 2, (int) exactCoordinates[1] * 2};
            this.coordinateCounts.add(i, intArray);
        }
    }


    /**
     * Pre-process the images. For instance: despeckle the image to prevent false positives.
     *
     * @param ip ImageProcessor.
     */
    private void preprocessImage(final ImageProcessor ip) {

        // Perform 'despeckle' using RankFilters.
        SilentRankFilters r = new SilentRankFilters();
        r.rank(ip, 1, RankFilters.MEDIAN);
    }

    /**
     * Find the photons in the image using MaximumFinder, and return their approximate coordinates.
     *
     * @param ip ImageProcessor.
     * @return Polygon with all maxima points found.
     */
    private Polygon findPhotons(final ImageProcessor ip) {
        int[][] coordinates;

        // Find the maxima using MaximumFinder
        SilentMaximumFinder maxFind = new SilentMaximumFinder();
        Polygon maxima = maxFind.getMaxima(ip, this.tolerance, true);

        coordinates = new int[2][maxima.npoints];
        coordinates[0] = maxima.xpoints; // X coordinates
        coordinates[1] = maxima.ypoints; // y coordinates

        return maxima;
    }

    /**
     * Calculate the exact sub-pixel positions of the photon events at the given coordinates.
     *
     * @param xCor Original x coordinate as found by MaximumFinder.
     * @param yCor Original y coordinate as found by MaximumFinder.
     * @param ip   ImageProcessor.
     * @return The new calculated coordinates.
     */
    private double[] calculateExactCoordinates(final int xCor, final int yCor, final ImageProcessor ip) {

        // Wand MUST BE created here, otherwise wand object might be used for multiple photons at the same time.
        Wand wd = new Wand(ip);
        double[] subPixelCoordinates = new double[2];

        // Outline the center of the photon using the wand tool.
        wd.autoOutline(xCor, yCor, this.tolerance, Wand.FOUR_CONNECTED);

        // Draw a rectangle around the outline.
        Rectangle rect = new PolygonRoi(wd.xpoints, wd.ypoints, wd.npoints, Roi.FREEROI).getBounds();

        // Check if the newly found coordinates are reasonable.
        // (If the original midpoint is too dark compared to the background,
        // the whole image might be selected by the wand tool, if the tolerance is too high.)
        if (rect.height == ip.getHeight() || rect.width > ip.getWidth()) {

            // If the width and height of the rectangle are too big, use the original coordinates.
            subPixelCoordinates[0] = xCor;
            subPixelCoordinates[1] = yCor;
        } else {

            // Otherwise, return the centers of the found rectangles as new coordinates.
            subPixelCoordinates[0] = rect.getCenterX();
            subPixelCoordinates[1] = rect.getCenterY();
        }

        return subPixelCoordinates;
    }

    /**
     * This method generates a BufferedImage from the int two D array and returns it.
     *
     * @param value int two D array containing all the count values.
     * @return BufferedImage from the int two D array.
     */
    public BufferedImage createBufferedImage(int[][] value) {

        // Create new ShortProcessor for output image with matrix data and it's width and height.
        ShortProcessor sp = new ShortProcessor(value[0].length, value.length);
        sp.setIntArray(value);

        // Add the amount of different values in array.
        List<Integer> diffMatrixCount = new ArrayList<>();
        for (int[] photonCountMatrix1 : sp.getIntArray()) {
            for (int photonCountMatrix2 : photonCountMatrix1) {
                if (!diffMatrixCount.contains(photonCountMatrix2)) {
                    diffMatrixCount.add(photonCountMatrix2);
                }
            }
        }

        // Use 0 as min and largest value in the matrix as max for grayscale mapping.
        if (diffMatrixCount.size() == 2) {
            sp.setMinAndMax(0, 1);
        } else {
            sp.setMinAndMax(0, (diffMatrixCount.size() - 2));
        }

        return sp.get16BitBufferedImage();
    }
}
