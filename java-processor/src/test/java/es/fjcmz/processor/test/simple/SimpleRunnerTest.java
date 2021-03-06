package es.fjcmz.processor.test.simple;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.fjcmz.processor.Runner;
import es.fjcmz.processor.simple.impl.SimpleRunnerImpl;
import es.fjcmz.processor.test.processors.CSVReaderProcessorFactory;
import es.fjcmz.processor.test.processors.CSVWriterProcessorFactory;
import es.fjcmz.processor.test.processors.StringReverserProcessorFactory;

/**
 * This test will read all the CSV files from FILE_NAMES, reverse all the lines and write them all together into another CSV file created with File.createTempFile(); <br>
 * This test uses a simple runner, that is an execution that uses a single thread and runs in the same thread as the caller. 
 * 
 * @author "Javier Cano"
 *
 */
public class SimpleRunnerTest {

	private static final Logger LOG = LoggerFactory.getLogger(SimpleRunnerTest.class);
	
	@Test
	public void test() {
		// The factories representing the different steps of the process
		CSVReaderProcessorFactory readerFactory = new CSVReaderProcessorFactory();
		StringReverserProcessorFactory reverserFactory = new StringReverserProcessorFactory();
		CSVWriterProcessorFactory writerFactory = new CSVWriterProcessorFactory();
		// Initial products
		List<File> inputFiles = asFiles(FILE_NAMES);
		// Prepare a simple execution
		Runner runner = new SimpleRunnerImpl("CSV processing", readerFactory)
			.chain(reverserFactory)
			.chain(writerFactory);
		// Execute and measure it
		long start = System.currentTimeMillis();
		runner.executeOn(inputFiles, NULL);
		long elapsed = System.currentTimeMillis() - start;
		LOG.debug("Finished Simple executor for {} iput files in {} ms", FILE_NAMES.length, elapsed);
	}
	
	protected List<File> asFiles(String[] fileNames) {
		List<File> files = new ArrayList<>();
		for(String fileName : fileNames) {
			files.add(new File(fileName));
		}
		return files;
	}
	
	// //
	
	protected static final File NULL = new File("");

	// //
	
	protected static final String[] FILE_NAMES = new String[] {
		"src/test/resources/csv1.csv",
		"src/test/resources/csv2.csv",
		"src/test/resources/csv3.csv",
		"src/test/resources/csv4.csv",
		"src/test/resources/csv1.csv",
		"src/test/resources/csv2.csv",
		"src/test/resources/csv3.csv",
		"src/test/resources/csv4.csv",
		"src/test/resources/csv1.csv",
		"src/test/resources/csv2.csv",
		"src/test/resources/csv3.csv",
		"src/test/resources/csv4.csv",
		"src/test/resources/csv1.csv",
		"src/test/resources/csv2.csv",
		"src/test/resources/csv3.csv",
		"src/test/resources/csv4.csv",
		"src/test/resources/csv1.csv",
		"src/test/resources/csv2.csv",
		"src/test/resources/csv3.csv",
		"src/test/resources/csv4.csv",
		"src/test/resources/csv1.csv",
		"src/test/resources/csv2.csv",
		"src/test/resources/csv3.csv",
		"src/test/resources/csv4.csv",
		"src/test/resources/csv1.csv",
		"src/test/resources/csv2.csv",
		"src/test/resources/csv3.csv",
		"src/test/resources/csv4.csv",
		"src/test/resources/csv1.csv",
		"src/test/resources/csv2.csv",
		"src/test/resources/csv3.csv",
		"src/test/resources/csv4.csv"
	};
	
}
