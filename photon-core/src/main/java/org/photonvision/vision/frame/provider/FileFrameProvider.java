/*
 * Copyright (C) Photon Vision.
 *
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.photonvision.vision.frame.provider;

import edu.wpi.first.math.geometry.Rotation2d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.photonvision.vision.calibration.CameraCalibrationCoefficients;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;

/**
* A {@link FrameProvider} that will read and provide an image from a {@link java.nio.file.Path
* path}.
*/
public class FileFrameProvider implements FrameProvider {
    public static final int MAX_FPS = 120;
    private static int count = 0;

    private final int thisIndex = count++;
    private final Path path;
    private final int millisDelay;
    private final Frame originalFrame;

    private final FrameStaticProperties properties;

    private long lastGetMillis = System.currentTimeMillis();

    /**
    * Instantiates a new FileFrameProvider.
    *
    * @param path The path of the image to read from.
    * @param fov The fov of the image.
    * @param maxFPS The max framerate to provide the image at.
    */
    public FileFrameProvider(Path path, double fov, int maxFPS) {
        this(path, fov, maxFPS, null, null);
    }

    public FileFrameProvider(
            Path path, double fov, Rotation2d pitch, CameraCalibrationCoefficients calibration) {
        this(path, fov, MAX_FPS, pitch, calibration);
    }

    public FileFrameProvider(
            Path path,
            double fov,
            int maxFPS,
            Rotation2d pitch,
            CameraCalibrationCoefficients calibration) {
        if (!Files.exists(path))
            throw new RuntimeException("Invalid path for image: " + path.toAbsolutePath().toString());
        this.path = path;
        this.millisDelay = 1000 / maxFPS;

        Mat rawImage = Imgcodecs.imread(path.toString());
        if (rawImage.cols() > 0 && rawImage.rows() > 0) {
            properties =
                    new FrameStaticProperties(rawImage.width(), rawImage.height(), fov, pitch, calibration);
            originalFrame = new Frame(new CVMat(rawImage), properties);
        } else {
            throw new RuntimeException("Image loading failed!");
        }
    }

    /**
    * Instantiates a new File frame provider.
    *
    * @param pathAsString The path of the image to read from as a string.
    * @param fov The fov of the image.
    */
    public FileFrameProvider(String pathAsString, double fov) {
        this(Paths.get(pathAsString), fov, MAX_FPS);
    }

    /**
    * Instantiates a new File frame provider.
    *
    * @param path The path of the image to read from.
    * @param fov The fov of the image.
    */
    public FileFrameProvider(Path path, double fov) {
        this(path, fov, MAX_FPS);
    }

    @Override
    public Frame get() {
        Frame outputFrame = new Frame(new CVMat(), properties);
        originalFrame.copyTo(outputFrame);

        // block to keep FPS at a defined rate
        if (System.currentTimeMillis() - lastGetMillis < millisDelay) {
            try {
                Thread.sleep(millisDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        lastGetMillis = System.currentTimeMillis();
        return outputFrame;
    }

    @Override
    public String getName() {
        return "FileFrameProvider" + thisIndex + " - " + path.getFileName();
    }
}
