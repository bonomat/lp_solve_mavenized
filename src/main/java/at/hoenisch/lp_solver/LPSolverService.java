package at.hoenisch.lp_solver;

import net.sf.javailp.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.*;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 */
public class LPSolverService implements BundleActivator {

    static {
        try {
            // load the class from class path (<Bundle-NativeCode> from pom xml or system classpath)
            System.loadLibrary("lpsolve55j_x642"); //TODO use the platform dependent one
            System.out.println("Loading from classpath successful");
        } catch (UnsatisfiedLinkError e) {
            try {
                //during runtime. .DLL within .JAR
                loadFromJarFiles("lpsolve55j_x64");
                System.out.println("Loading hardcoded successful");
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    /**
     * @param libname to be loaded,
     */
    private static void loadFromJarFiles(String libname) {
        try {

            String fileSeparator = System.getProperty("file.separator");
            String pathSeparator = System.getProperty("path.separator");
            String classpath = System.getProperty("java.class.path");
            String qualifiedLibname = System.mapLibraryName(libname);

            System.out.println("Checking paths and filenames:");
            System.out.println("Full library name is: " + qualifiedLibname);

            // We must search for the library in all jar-files, because we don't know in which jar-file the solver came
            Pattern filename = Pattern.compile(".*jar");
            String[] files = classpath.split(pathSeparator);
            System.out.println(files.length + " files in classpath");
            for (String s : files) {
                System.out.println(s);
            }
            // Searching for the libraries in the jars with a regexp enables that the libraries can be stored in a subdirectory
            Pattern ziplibname = Pattern.compile(".*" + qualifiedLibname);

            InputStream in = null;
            for (String s : files) {
                if (!s.contains("LPSOLVE")) //only take the jar file containing that so, i.e. LPSOLVESolverPack.jar
                    continue;
                // Looking up all the contents of every jar-file
                if (s != null) {
                    if (!filename.matcher(s).matches())
                        continue;
                    System.out.println("Checking " + s);
                    try {

                        ZipFile jarfile = new ZipFile(s);
                        Enumeration<? extends ZipEntry> e = jarfile.entries();

                        while (e.hasMoreElements() && in == null) {
                            ZipEntry ze = e.nextElement();

                            //System.out.println("Current file is: " + ze.getName(), 2);
                            if (ziplibname.matcher(ze.getName()).matches()) {
                                System.out.println("FOUND!!!");

                                in = jarfile.getInputStream(ze);
                            }
                        }
                        System.out.println("Finished " + s);
                    } catch (Exception e) {
                        System.out.println("An " + e.getMessage() + " exception occured while trying to open as a ZIP-File.");
                    }

                    if (in != null) {
                        System.out.println("File found in " + s);
                        break;
                    }
                }
            }

            if (in == null) {
                System.out.println("Could not find required library: " + libname);
                return;
            }

            // Okay, we found the library
            // Now we have to write it to the file-system somewhere convenient
            // The following locations are tried in this order:
            // - any directory in the library path
            // - the current working directory
            // (- directories containing jar-files) <-- not implemented yet!
            // (- user.home) <-- not requested so far
            // - the system temp-directory

            // Trying library path locations:
            String libpath = System.getProperty("java.library.path");
            System.out.println("java.library.path is: " + libpath);

            if (libpath != null && libpath.length() > 0) {
                System.out.println("java.library.path seems to contain valid paths");

                String[] paths = libpath.split(pathSeparator);
                for (String fullname : paths) {
                    fullname = fullname + fileSeparator + qualifiedLibname;
                    if (writeInStreamToFile(in, fullname)) {
                        System.out.println("Wrote library to library path: " + fullname);
                        System.loadLibrary(libname);
                        return;
                    }
                }
            } else {
                System.out.println("java.library.path contains no valid paths");
            }

            // Now test the rest locations ...
            String[] paths = new String[2];
            paths[0] = System.getProperty("user.dir");
            paths[1] = System.getProperty("java.io.tmpdir");

            for (String fullname : paths) {
                fullname = fullname + fileSeparator + qualifiedLibname;
                if (writeInStreamToFile(in, fullname)) {
                    System.out.println("Wrote library to regular path: " + fullname);
                    System.load(fullname);
                    return;
                }
            }
            // No more locations to write file
            return;

        } catch (Exception e) {
            // Looking up jar-files failed
            System.err.println("An error occured while searching for the library in the solver module:");
            e.printStackTrace();
        }

    }

    /**
     * @param bundleContext
     */
    @Override
    public void start(final BundleContext bundleContext) {
        System.out.println("Java ILP service started");
        testILP();
    }

    /**
     * Constructing a Problem:
     * Maximize: 143x+60y
     * Subject to:
     * 120x+210y <= 15000
     * 110x+30y <= 4000
     * x+y <= 75
     * <p/>
     * With x,y being integers
     */
    protected void testILP() {

        Problem problem = new Problem();

        Linear linear = new Linear();
        linear.add(143, "x");
        linear.add(60, "y");

        problem.setObjective(linear, OptType.MAX);

        linear = new Linear();
        linear.add(120, "x");
        linear.add(210, "y");

        problem.add(linear, "<=", 15000);

        linear = new Linear();
        linear.add(110, "x");
        linear.add(30, "y");

        problem.add(linear, "<=", 4000);

        linear = new Linear();
        linear.add(1, "x");
        linear.add(1, "y");

        problem.add(linear, "<=", 75);

        problem.setVarType("x", Integer.class);
        problem.setVarType("y", Integer.class);

        Result result = solveProblem(problem);

        System.out.println("---------------------------------------");
        System.out.println(result);
        System.out.println("should have printed: \n Objective: 6266 {y=52, x=22}\n");
        System.out.println("---------------------------------------");

    }

    /**
     * Tries to write the input stream to the location specified in filename. If
     * copying was successful, the method returns true, otherwise it returns false.
     *
     * @param in       the input stream containing the source file
     * @param filename the destiny location the file is to be written to
     * @return <code>true</code> if copying was successful, <code>false</code> otherwise
     */
    private static boolean writeInStreamToFile(InputStream in, String filename) {
        try {
            File f = new File(filename);
            if (f.exists()) {
                return true;
            }
            // Opening an appropriate output stream and copy the library
            FileOutputStream fileout = new FileOutputStream(filename);
            BufferedOutputStream out = new BufferedOutputStream(fileout);

            byte[] buffer = new byte[1024];
            int len;

            while ((len = in.read(buffer)) >= 0)
                out.write(buffer, 0, len);

            in.close();
            out.close();

            System.out.println("Copied file successfully to: " + filename);

            return true;

        } catch (IOException e) {
            System.out.println("Failed to write file to: " + filename);
            return false;
        }
    }

    public void destroy() {
        System.out.println("Java ILP service destroyed");
    }

    /**
     * @param bundleContext bundle context
     */
    @Override
    public void stop(final BundleContext bundleContext) {
        System.out.println("Example Service deleted");
    }

    /**
     * solvces the given problem and returns the result
     *
     * @param problem to be solved
     * @return the result of the problem
     */
    public Result solveProblem(Problem problem) {
        SolverFactory factory = new SolverFactoryLpSolve(); // use lp_solve
        factory.setParameter(Solver.VERBOSE, 0);
        factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds

        Solver solver = factory.get(); // you should use this solver only once for one problem
        return solver.solve(problem);
    }
}
