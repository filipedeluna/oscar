package oscar.engine;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import oscar.controller.Controller;
import oscar.transformers.IJimpleTransformer;
import oscar.transformers.JimpleSceneTransformer;
import oscar.transformers.JimpleTransformer;
import oscar.transformers.analysers.Variable;
import oscar.transformers.analysers.shared.SharedVariableAnalyser;
import oscar.transformers.analysers.shared.SharedVariableParser;
import oscar.transformers.injectors.ControllerInjector;
import oscar.transformers.injectors.ExitCaptureInjector;
import oscar.transformers.noisers.lock.ReentrantLockNoiser;
import oscar.transformers.noisers.misc.ClassInitializationNoiser;
import oscar.transformers.noisers.shared.SharedVariableNoiser;
import oscar.transformers.noisers.sync.SynchronizedBlockNoiser;
import oscar.transformers.noisers.sync.SynchronizedMethodCallNoiser;
import oscar.transformers.noisers.thread.ThreadCreationNoiser;
import oscar.transformers.noisers.misc.ThreadExternalFieldNoiser;
import oscar.utils.ClassWriter;
import oscar.utils.logger.LoggerFactory;
import oscar.utils.logger.LoggerFormatter;
import soot.*;
import soot.options.Options;
import soot.util.Chain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Engine {
  public static List<String> BlacklistedClasses = Arrays.asList(
      "java.",
      "sun.",
      "jdk.",
      "javax.",
      "com.",
      "org.",
      "kotlin.",
      "android.",
      "io.",
      "okhttp3.",
      "dagger.",
      "soot.",
      "oscar.",
      "$"
  );

  private static final Logger logger = LoggerFactory.getInstance(Engine.class);

  private static final String OSCAR_TEMP_DIR = ".oscar_temp";
  private static final String OSCAR_TEMP_EXTRACT_DIR = OSCAR_TEMP_DIR + File.separator + "extract";
  private static final String OSCAR_TEMP_GENERATED_DIR = OSCAR_TEMP_DIR + File.separator + "generated";

  private static final Random random = new Random();

  // Inject additional classes, not included in controller package
  private static final List<Class<?>> injectedClasses = Arrays.asList(
      LoggerFormatter.class,
      LoggerFactory.class
  );
  public static boolean JarMode;

  private final String targetFile;
  private static String mainClass = null;
  private final String targetDirectory;
  private final String outputDirectory;

  private final HashMap<String, ArrayList<Transform>> transformers = new HashMap<>();

  public Engine(String targetFile, String mainClass, String outputDirectory) {
    Engine.mainClass = mainClass;
    this.targetFile = targetFile;
    this.outputDirectory = outputDirectory;

    // Parse target path
    Path targetPath = Paths.get(targetFile);
    if (targetPath.toFile().isDirectory())
      this.targetDirectory = targetPath.toString();
    else
      this.targetDirectory = targetPath.getParent().toString();

    // Data structures for shared variable analysis and parsing
    HashMap<String, HashSet<String>> variableDependencies = new HashMap<>();
    DefaultDirectedGraph<Variable, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

    // Register all transformers
    List.of(
        // Analysers ------------------
        new SharedVariableAnalyser(graph), // Must be before svn and svp
        new SharedVariableParser(graph, variableDependencies), // Must be before svn, after sva

        // Scene Transformers  ------------------
        new ThreadCreationNoiser(),
        new SharedVariableNoiser(variableDependencies),

        // Transformers ------------------
        new SynchronizedBlockNoiser(),
        new SynchronizedMethodCallNoiser(),
        new ReentrantLockNoiser(),
        new ClassInitializationNoiser(),
        new ThreadExternalFieldNoiser(),

        // Injectors ------------------
        new ControllerInjector(),
        new ExitCaptureInjector()
    ).forEach(this::registerTransformer);
  }

  public void run() {
    // Set Soot configurations
    G.reset();

    logger.info("Initializing Soot engine...");

    Options.v().set_allow_phantom_refs(true);
    Options.v().set_whole_program(true);
    Options.v().set_prepend_classpath(true);
    Options.v().set_validate(true);
    Options.v().set_include_all(true);
    Options.v().set_output_format(Options.output_format_class);
    Options.v().set_output_dir(outputDirectory);
    Options.v().set_soot_classpath(OSCAR_TEMP_EXTRACT_DIR);
    Options.v().set_process_dir(Collections.singletonList(OSCAR_TEMP_EXTRACT_DIR));
    Options.v().set_force_overwrite(true);

    // Try to create temp folder
    try {
      FileUtils.deleteDirectory(new File(OSCAR_TEMP_DIR));
      Files.createDirectory(Paths.get(OSCAR_TEMP_DIR));
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete temp folder. Check directory permissions.", e);
    }

    // Delete output folder if exists
    try {
      FileUtils.deleteDirectory(new File(outputDirectory));
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete output directory. Check file permissions.", e);
    }

    // Check if JAR file and process accordingly
    if (JarMode) {
      Options.v().set_output_dir(OSCAR_TEMP_GENERATED_DIR);

      // Extract jar contents to directory
      //noinspection resource
      ZipFile jar = new ZipFile(targetFile);

      try {
        jar.extractAll(OSCAR_TEMP_EXTRACT_DIR);
      } catch (IOException e) {
        throw new RuntimeException("Failed to extract jar. Check permissions.", e);
      }
    } else {
      try {
        FileUtils.copyDirectory(new File(targetDirectory), new File(OSCAR_TEMP_EXTRACT_DIR));
      } catch (IOException e) {
        throw new RuntimeException("Failed to copy target files to temporary directory.", e);
      }
    }

    ClassWriter.writeClassPackageToFile(Controller.class, OSCAR_TEMP_EXTRACT_DIR);
    injectedClasses.forEach(c -> ClassWriter.writeToFile(c, OSCAR_TEMP_EXTRACT_DIR));

    // Check main exists after loading necessary classes
    Scene.v().loadNecessaryClasses();
    Chain<SootClass> sc = Scene.v().getClasses();

    if (sc.stream().noneMatch(c -> c.getName().equals(mainClass)))
      throw new RuntimeException("Failed to find provided main class.");

    logger.info("Soot engine initialization complete.");

    logger.info("Registering Soot packs.");
    transformers.forEach((p, t) -> t.forEach(PackManager.v().getPack(p)::add));
    logger.info("Soot packs registered.");

    // Run Soot packs
    logger.info("Running Soot packs.");
    PackManager.v().runPacks();
    logger.info("Soot packs finished running.");

    end();
  }

  public void end() {
    // Write the result of packs in outputPath
    logger.info("Writing Soot output.");
    PackManager.v().writeOutput();

    // If output is jar, create jar
    if (JarMode) {
      Options.v().set_output_dir(OSCAR_TEMP_GENERATED_DIR);

      String[] splitTargetJarPath = targetFile.split("/");
      String outputJarName = splitTargetJarPath[splitTargetJarPath.length - 1];

      // Try to create output folder
      try {
        Files.createDirectory(Paths.get(outputDirectory));
      } catch (IOException e) {
        throw new RuntimeException("Failed to delete output folder. Check directory permissions.", e);
      }

      //noinspection resource
      ZipFile jar = new ZipFile(outputDirectory + File.separator + outputJarName);

      try {
        for (File tempFile : getDirectoryContent(OSCAR_TEMP_EXTRACT_DIR))
          if (tempFile.isDirectory())
            jar.addFolder(tempFile);
          else
            jar.addFile(tempFile);

        for (File tempFile : getDirectoryContent(OSCAR_TEMP_GENERATED_DIR))
          if (tempFile.isDirectory())
            jar.addFolder(tempFile);
          else
            jar.addFile(tempFile);
      } catch (ZipException e) {
        throw new RuntimeException("Failed to add files from temp folder to zip.", e);
      }

      /* TODO does not seem necessary
      // Copy generated files over
      try {
        File srcDir = new File(OSCAR_TEMP_GENERATED_DIR);
        File destDir = new File(targetDirectory);
        FileUtils.copyDirectory(srcDir, destDir);
      } catch (IOException e) {
        throw new RuntimeException("Failed to copy generated sources to output folder. Check directory permissions.", e);
      }
       */

      // Delete temp folder
      try {
        FileUtils.deleteDirectory(new File(OSCAR_TEMP_DIR));
      } catch (IOException e) {
        throw new RuntimeException("Failed to delete temp folder. Check directory permissions.", e);
      }
    }

    logger.info("OSCAR instrumentation complete.");
  }

  public static void startTransformer(Class<? extends JimpleTransformer> transformerClass, Body body) {
    logger.fine("Starting regular transformer (Thread " + Thread.currentThread().getId() + ") '" +
                    transformerClass.getSimpleName() + "' for method '" +
                    body.getMethod().getSignature() + "'.");
  }

  public static void endTransformer(Class<? extends JimpleTransformer> transformerClass, Body body) {
    logger.fine("Finished regular transformer (Thread " + Thread.currentThread().getId() + ") '" +
                    transformerClass.getSimpleName() + "' for method '" +
                    body.getMethod().getSignature() + "'.");
  }

  public static void startSceneTransformer(Class<? extends SceneTransformer> transformerClass) {
    logger.fine("Starting scene transformer (Thread " + Thread.currentThread().getId() + ") '" +
                    transformerClass.getSimpleName() + "'.");
  }

  public static void endSceneTransformer(Class<? extends SceneTransformer> transformerClass) {
    logger.fine("Finished scene transformer (Thread " + Thread.currentThread().getId() + ") '" +
                    transformerClass.getSimpleName() + "'.");
  }

  public static void startCallgraphRoutine(Class<? extends JimpleSceneTransformer> transformerClass, SootMethod method) {
    logger.fine("Starting callgraph routine (Thread " + Thread.currentThread().getId() + ") '" +
                    transformerClass.getSimpleName() + "' for method '" +
                    method.getSignature() + "'.");
  }

  public static void endCallgraphRoutine(Class<? extends JimpleSceneTransformer> transformerClass, SootMethod method) {
    logger.fine("Ending callgraph routine (Thread " + Thread.currentThread().getId() + ") '" +
                    transformerClass.getSimpleName() + "' for method '" +
                    method.getSignature() + "'.");
  }

  public void registerTransformer(Transformer transformer) {
    String phase = "";
    String subPhase = "";

    if (transformer instanceof IJimpleTransformer) {
      phase = ((IJimpleTransformer) transformer).getPhase();
      subPhase = ((IJimpleTransformer) transformer).getSubPhase();
    }

    transformers.putIfAbsent(phase, new ArrayList<>());
    transformers.get(phase).add(new Transform(subPhase, transformer));
    logger.info("Registered transformer " + transformer.getClass().getSimpleName() +
                    " with subphase " + subPhase + " in phase " + phase);
  }

  private static List<File> getDirectoryContent(String dir) {
    File dirFile = new File(dir);
    int maxDirDepth = dirFile.getPath().split(File.separator).length + 1;

    return FileUtils.listFilesAndDirs(
                        dirFile,
                        FileFilterUtils.trueFileFilter(),
                        FileFilterUtils.trueFileFilter()
                    )
                    .stream()
                    .filter(f -> f.getPath().split(File.separator).length == maxDirDepth)
                    .collect(Collectors.toCollection(ArrayList::new));
  }

  public static String getMainClass() {
    return mainClass;
  }

  public static boolean isClassBlacklisted(SootMethod method) {
    String className = method.getDeclaringClass().getName();

    boolean ignored = Engine.BlacklistedClasses.stream().anyMatch(className::startsWith);

    if (ignored)
      logger.fine("Ignoring blacklisted class: " + className);

    return ignored;
  }
}
