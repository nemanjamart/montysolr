package invenio.montysolr.util;

import invenio.montysolr.jni.MontySolrVM;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class MontySolrSetup {
	private static Properties prop = getProperties();

	public static void init(String mainModuleName, String mainModulePath)
			throws Exception {

		// chaning PYTHONPATH (has no effect on the embedded interpereter)
		// String pythonpath = MontySolrTestCaseJ4.MONTYSOLR_HOME +
		// "/src/python";
		// ProcessUtils.addEnv("PYTHONPATH", pythonpath);

		// the path added to sys.path is the parent
		System.setProperty("montysolr.modulepath",
				getChildModulePath(mainModulePath));

		// discover and set -Djava.library.path
		String jccpath = ProcessUtils.getJCCPath();
		ProcessUtils.setLibraryPath(jccpath);

		// this is necessary to run in the main thread and because of the
		// python loads the parent folder and inserts it into the pythonpath
		// we trick it
		MontySolrVM.INSTANCE.start(getChildModulePath(mainModulePath));

		System.setProperty("montysolr.bridge", mainModuleName);

		// for testing purposes add what is in the pythonpath
		File f = new File(getMontySolrHome() + "/build/build.properties");
		if (f.exists()) {
			Properties p = new Properties();
			p.load(new FileInputStream(f));
			if (p.containsKey("python_path")) {
				String pp = p.getProperty("python_path");
				addToSysPath(pp);
			}
		}

		// other methods like starting a jetty instance need these too
		System.setProperty("solr.test.sys.prop1", "propone");
		System.setProperty("solr.test.sys.prop2", "proptwo");

	}

	public static Properties getProperties() {
		if (prop != null) {
			return prop;
		}
		return loadProperties();
	}

	/**
	 * MontySolr depends on the properties file for unittesting, as it was too
	 * much hassle to load interpolated strings from the build.properties file
	 * (without adding more jars) and the discovery of files using file
	 * structure crawling is ugly
	 * 
	 * @return
	 * @throws IOException
	 */
	private static Properties loadProperties() throws IllegalStateException {
		Properties prop = new Properties();
		String base = getMontySolrHome();
		try {
			prop.load(new FileInputStream(new File(base
					+ "/build/build.properties")));
		} catch (IOException e) {
			throw new IllegalStateException("Your montysolr installation does not have "
					+ base + "build/build.properties file! "
					+ "You should run ant build-all in the root folder");
		}
		return prop;
	}

	public static String getSolrHome() {
		String base = getMontySolrHome();

		String solr_home = prop.getProperty("solr.home");

		if (solr_home == null || !(new File(solr_home).exists())) {
			throw new IllegalStateException(
					"Your montysolr build.properties file has incorrect value for solr.home: "
							+ solr_home);
		}

		File s = new File(solr_home);
		File s_level_down = new File(s.getAbsolutePath() + "/solr");

		if (s_level_down.exists())
			return s_level_down.getAbsolutePath();

		if (s.exists())
			return s.getAbsolutePath();

		throw new IllegalStateException(
				"Cannot determine the folder with solr installation");
	}

	/**
	 * Gets a resource from the context classloader as {@link File}. This method
	 * should only be used, if a real file is needed. To get a stream, code
	 * should prefer {@link Class#getResourceAsStream} using
	 * {@code this.getClass()}.
	 */
	public static File getFile(String name) {
		try {
			File file = new File(name);
			if (!file.exists()) {
				file = new File(Thread.currentThread().getContextClassLoader()
						.getResource(name).toURI());
			}
			return file;
		} catch (Exception e) {
			/* more friendly than NPE */
			throw new RuntimeException("Cannot find resource: " + name);
		}
	}

	static String determineMontySourceHome() {
		File base = getFile("examples/README.txt").getAbsoluteFile();
		return new File(base.getParentFile().getParentFile(), "test-files/")
				.getAbsolutePath();
	}

	/**
	 * Returns the root folder of montysolr
	 * 
	 * @return
	 */
	public static String getMontySolrHome() {
		File base = new File(System.getProperty("user.dir"));
		// File base = getFile("solr/conf").getAbsoluteFile();
		while (!new File(base, "src/python").exists()) {
			base = base.getParentFile();
		}
		return base.getAbsolutePath();
	}

	public static void addToSysPath(String... paths) {
		for (String path : paths) {
			MontySolrVM.INSTANCE.evalCommand("import sys;\'" + path
					+ "\' in sys.path or sys.path.insert(0, \'" + path + "\')");
		}
	}

	public static void addTargetsToHandler(String... modules) {
		for (String path : modules) {
			// this is a quick hack, i should make the handler to have a defined
			// place (or find some better way of adding)
			MontySolrVM.INSTANCE
					.evalCommand("self._handler.discover_targets([\'" + path
							+ "\'])");
		}
	}

	/**
	 * Trick to find any existing folder/file inside the main module path and
	 * return it to be set by python into the PYTHONPATH
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String getChildModulePath(String modulePath) throws Exception {
		File f = new File(modulePath);
		if (f.isFile()) {
			return f.getAbsolutePath().toString();
		} else if (f.exists()) {
			for (String child : f.list()) {
				return new File(f.getAbsolutePath() + "/" + child)
						.getAbsolutePath();
			}
		}
		throw new Exception("The module.path must exist: " + modulePath);
	}
}