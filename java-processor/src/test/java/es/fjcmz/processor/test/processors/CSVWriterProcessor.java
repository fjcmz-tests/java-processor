package es.fjcmz.processor.test.processors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;

import es.fjcmz.processor.BaseProcessor;
import es.fjcmz.processor.UnhandledException;

public class CSVWriterProcessor extends BaseProcessor<List<String[]>, Object> {

	protected String filePrefix = null;
	protected String fileSuffix = null;
	protected CSVWriter<String[]> writer = null;
	
	public CSVWriterProcessor(String name, String filePrefix, String fileSuffix) {
		super(name);
		this.filePrefix = filePrefix;
		this.fileSuffix = fileSuffix;
	}

	@Override
	public void prepare() throws UnhandledException {
		super.prepare();
		//
		try {
			File tempFile = File.createTempFile(filePrefix, fileSuffix);
			FileWriter fileWriter = new FileWriter(tempFile);
			this.writer = CSVWriterBuilder.newDefaultWriter(fileWriter);
		} catch (IOException ex) {
			throw new UnhandledException(ex);
		}
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
	
}
