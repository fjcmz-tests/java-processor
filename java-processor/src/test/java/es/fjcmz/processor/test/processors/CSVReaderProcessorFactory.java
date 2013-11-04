package es.fjcmz.processor.test.processors;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;

import es.fjcmz.processor.BaseProcessorFactory;
import es.fjcmz.processor.Processor;

public class CSVReaderProcessorFactory extends BaseProcessorFactory<File, List<String[]>> {

	protected int counter = 0;
	
	public CSVReaderProcessorFactory() {
		super(NULL);
	}

	@Override
	protected Processor<File, List<String[]>> doCreateProcessor() {
		counter++;
		CSVReaderProcessor processor = new CSVReaderProcessor("CSVReaderProcessor_" + counter, 64);
		return processor;
	}	

	// //
	
	private static final List<String[]> NULL = Lists.newArrayList();
	
}
