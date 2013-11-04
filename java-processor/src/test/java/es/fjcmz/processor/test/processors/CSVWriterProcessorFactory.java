package es.fjcmz.processor.test.processors;

import java.util.List;

import es.fjcmz.processor.BaseProcessorFactory;
import es.fjcmz.processor.Processor;

public class CSVWriterProcessorFactory extends BaseProcessorFactory<List<String[]>, Object> {

	protected int counter = 0;
	
	public CSVWriterProcessorFactory() {
		super(NULL);
	}

	@Override
	protected Processor<List<String[]>, Object> doCreateProcessor() {
		counter++;
		CSVWriterProcessor processor = new CSVWriterProcessor("CSVWriterProcessor_" + counter, "example_", ".csv");
		return processor;
	}

	// //
	
	protected static final Object NULL = new Object();
	
}
