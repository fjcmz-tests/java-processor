package es.fjcmz.processor.test.processors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;

import es.fjcmz.processor.BaseProcessorFactory;
import es.fjcmz.processor.Processor;
import es.fjcmz.processor.UnhandledException;

public class CSVWriterProcessorFactory extends BaseProcessorFactory<List<String[]>, Object> {

	protected CSVWriter<String[]> writer = null;
	protected int counter = 0;
	
	public CSVWriterProcessorFactory() {
		super(NULL);
	}

	@Override
	protected Processor<List<String[]>, Object> doCreateProcessor() {
		counter++;
		CSVWriterProcessor processor = new CSVWriterProcessor("CSVWriterProcessor_" + counter, getWriter());
		return processor;
	}
	
	protected CSVWriter<String[]> getWriter() throws UnhandledException {
		if (writer == null) {
			writer = createWriter();
		}
		return writer;
	}
	
	protected CSVWriter<String[]> createWriter() throws UnhandledException {
		try {
			File tempFile = File.createTempFile("example_", ".csv");
			FileWriter fileWriter = new FileWriter(tempFile);
			return CSVWriterBuilder.newDefaultWriter(fileWriter);
		} catch (IOException ex) {
			throw new UnhandledException(ex);
		}
	}

	// //
	
	protected static final Object NULL = new Object();
	
}
