package invenio.montysolr.util;

import invenio.montysolr.jni.MontySolrVM;

import java.io.File;
import java.io.IOException;

import invenio.montysolr.util.MontySolrTestCaseJ4;
import invenio.montysolr.util.ProcessUtils;
import org.apache.solr.util.AbstractSolrTestCase;


public abstract class MontySolrAbstractTestCase extends AbstractSolrTestCase {
	
	public void setUp() throws Exception {
		super.setUp();
		
		// chaning PYTHONPATH (has no effect on the embedded interpereter)
		//String pythonpath = MontySolrTestCaseJ4.MONTYSOLR_HOME + "/src/python";
		//ProcessUtils.addEnv("PYTHONPATH", pythonpath);
		
		// the path added to sys.path is the parent
		System.setProperty("montysolr.modulepath", getChildModulePath());
		
		// discover and set -Djava.library.path
		String jccpath = ProcessUtils.getJCCPath();
		ProcessUtils.setLibraryPath(jccpath);
		
		// this is necessary to run in the main thread and because of the 
		// python loads the parent folder and inserts it into the pythonpath
		// we trick it
		MontySolrVM.INSTANCE.start(getModulePath());
		
		System.setProperty("montysolr.bridge", getModuleName());

		
	}
	
	public String getMontySolrHome() {
		return MontySolrTestCaseJ4.MONTYSOLR_HOME;
	}
	
	public String getSolrHome() {
		return MontySolrTestCaseJ4.TEST_HOME;
	}

	/** @see MontySolrTestCaseJ4#getFile */
	public static File getFile(String name) throws IOException {
		return MontySolrTestCaseJ4.getFile(name);
	}
	
	public String getModuleName() throws Exception {
		throw new Exception("You must implement this in your class!");
	}
	
	public String getModulePath() {
		return MontySolrTestCaseJ4.MONTYSOLR_HOME + "/src/python/montysolr";
	}
	
	/**
	 * Trick to find any existing folder/file inside the main module path
	 * and return it to be set by python into the PYTHONPATH
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getChildModulePath() throws Exception {
		File f = new File(getModulePath());
		if (f.isFile()) {
			return f.getAbsolutePath().toString();
		}
		else if(f.exists()) {
			for (String child: f.list()) {
				return new File(f.getAbsolutePath() + "/" + child).getAbsolutePath();
			}
		}
		throw new Exception("The module.path must exist: " + getModulePath());
	}
	
	
	public void addToSysPath(String... paths) {
		for (String path: paths) {
			MontySolrVM.INSTANCE.evalCommand("import sys;\'" + path + "\' in sys.path or sys.path.insert(0, \'" + path + "\')");
		}
	}
	
	public void addTargetsToHandler(String... modules) {
		for (String path: modules) {
			// this is a quick hack, i should make the handler to have a defined place (or find some better way of adding)
			MontySolrVM.INSTANCE.evalCommand("self._handler.discover_targets([\'" + path + "\'])");
		}
	}
}