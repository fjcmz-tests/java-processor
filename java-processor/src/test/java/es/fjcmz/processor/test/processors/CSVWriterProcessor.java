package es.fjcmz.processor.test.processors;

import java.io.IOException;
import java.util.List;

import com.googlecode.jcsv.writer.CSVWriter;

import es.fjcmz.processor.BaseProcessor;
import es.fjcmz.processor.UnhandledException;

public class CSVWriterProcessor extends BaseProcessor<List<String[]>, Object> {

	protected CSVWriter<String[]> writer = null;
	
	public CSVWriterProcessor(String name, CSVWriter<String[]> writer) {
		super(name);
		this.writer = writer;
	}

	@Override
	protected Object doProcess(List<String[]> product)
			throws UnhandledException {
		if (product != null) {
			try {
				writer.writeAll(product);
			} catch (IOException ex) {
				throw new UnhandledException(ex);
			}
		}
		return null;
	}
	
	@Override
	public void dispose() throws UnhandledException {
		super.dispose();
		//
		try {
			writer.flush();
		} catch (IOException ex) {
			throw new UnhandledException(ex);
		}
	}
	
}
