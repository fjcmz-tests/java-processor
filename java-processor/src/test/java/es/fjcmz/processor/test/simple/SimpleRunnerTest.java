package es.fjcmz.processor.test.simple;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import es.fjcmz.processor.Runner;
import es.fjcmz.processor.simple.impl.SimpleRunnerImpl;
import es.fjcmz.processor.test.processors.CSVReaderProcessorFactory;
import es.fjcmz.processor.test.processors.CSVWriterProcessorFactory;
import es.fjcmz.processor.test.processors.StringReverserProcessorFactory;

public class SimpleRunnerTest {

	protected static final String[] FILE_NAMES = new String[] {
		"/src/test/resources/csv1.csv",
		"/src/test/resources/csv2.csv"
	};
	
	@Test
	public void test() {
		// Factories
		CSVReaderProcessorFactory readerFactory = new CSVReaderProcessorFactory();
		StringReverserProcessorFactory reverserFactory = new StringReverserProcessorFactory();
		CSVWriterProcessorFactory writerFactory = new CSVWriterProcessorFactory();
		// Initial products
		List<File> inputFiles = asFiles(FILE_NAMES);
		//
		Runner runner = new SimpleRunnerImpl("CSV processing", readerFactory)
			.chain(reverserFactory)
			.chain(writerFactory);
		runner.executeOn(inputFiles, NULL);
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

}
