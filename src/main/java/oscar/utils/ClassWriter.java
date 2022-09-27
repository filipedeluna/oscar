package oscar.utils;

import org.apache.commons.io.FileUtils;
import oscar.utils.logger.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.logging.Logger;

public final class ClassWriter {
  private static final Logger logger = LoggerFactory.getInstance(ClassWriter.class);

  public static void writeToFile(Class<?> clazz, String directory) {
    String classFile = clazz.getSimpleName() + ".class";
    URL url = clazz.getResource(classFile);
    String directoryAppended = directory + "/" + clazz.getPackageName().replace(".", "/");
    String filename = directoryAppended + "/" + classFile;

    if (url == null)
      throw new RuntimeException("Failed to find class '" + classFile + "'.");

    byte[] classBytes;

    logger.fine("Attempting to write class '" + clazz.getName() + "' to file.");

    try {
      classBytes = Files.readAllBytes(Paths.get(url.toURI()));
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("Failed to read class '" + classFile + "'", e);
    }

    try {
      Files.deleteIfExists(Paths.get(directoryAppended));
    } catch (IOException e) {
      logger.fine("File '" + filename + "' seems to already exist.");
    }

    try {
      Files.createDirectories(Paths.get(directoryAppended));
    } catch (IOException e) {
      logger.fine("Directory '" + directoryAppended + "' seems to already exist.");
    }

    try {
      Files.write(Paths.get(filename), classBytes, StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      logger.fine("File '" + filename + "' seems to already exist, proceeding to write to it.");
    }

    try {
      Files.write(Paths.get(filename), classBytes, StandardOpenOption.WRITE);
    } catch (IOException e) {
      throw new RuntimeException("Failed to find class '" + classFile + "'", e);
    }
  }

  public static void writeClassPackageToFile(Class<?> clazz, String directory) {
    String classFile = clazz.getSimpleName() + ".class";
    URL url = clazz.getResource(classFile);

    if (url == null)
      throw new RuntimeException("Failed to find class '" + classFile + "'.");

    String packagePathLocation = Paths.get(url.getPath())
                                      .getParent()
                                      .toString()
                                      .replace("%20", " ");

    String packageOutputLocation = url.getPath()
                                                .substring(
                                                    0,
                                                    url.getPath().length() - classFile.length() - 1
                                                )
                                                .replace("%20", " ")
                                                .split(File.separator + "oscar" + File.separator)[1];

    packageOutputLocation = directory + File.separator + "oscar" + File.separator + packageOutputLocation;

    try {
      FileUtils.copyDirectory(new File(packagePathLocation), new File(packageOutputLocation));
    } catch (IOException e) {
      throw new RuntimeException("Failed to copy package '" + packagePathLocation + "'.", e);
    }
  }
}
