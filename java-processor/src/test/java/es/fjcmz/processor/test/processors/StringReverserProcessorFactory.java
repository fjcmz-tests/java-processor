package es.fjcmz.processor.test.processors;

import java.util.List;

import com.google.common.collect.Lists;

import es.fjcmz.processor.BaseProcessorFactory;
import es.fjcmz.processor.Processor;

public class StringReverserProcessorFactory extends BaseProcessorFactory<List<String[]>, List<String[]>> {

	protected int counter = 0;
	
	public StringReverserProcessorFactory() {
		super(NULL);
	}

	@Override
	protected Processor<List<String[]>, List<String[]>> doCreateProcessor() {
		counter++;
		StringReverserProcessor processor = new StringReverserProcessor("StringReverserProcessor_" + counter);
		return processor;
	}

	// //
	
	private static final List<String[]> NULL = Lists.newArrayList();
	
}
