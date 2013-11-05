package es.fjcmz.processor.test.processors;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;

import es.fjcmz.processor.BaseMultipartProcessor;
import es.fjcmz.processor.UnhandledException;

public class CSVReaderProcessor extends BaseMultipartProcessor<File, List<String[]>> {

	protected int chunkSize = 64;
	protected File file = null;
	protected CSVReader<String[]> csvReader = null;
	
	public CSVReaderProcessor(String name, int chunkSize) {
		super(name);
		this.chunkSize = chunkSize;
	}

	@Override
	protected void doPrepareFor(File product) throws UnhandledException {
		this.file = product;
		try {
			FileReader reader = new FileReader(file);
			this.csvReader = CSVReaderBuilder.newDefaultReader(reader);
		} catch (IOException ex) {
			throw new UnhandledException(ex);
		}
	}

	@Override
	protected List<String[]> doProcessStep() throws UnhandledException {
		List<String[]> parsedLines = new ArrayList<>();
		String[] parsedLine = null;
		try {
			while((parsedLine = csvReader.readNext()) != null) {
				parsedLines.add(parsedLine);
				if (parsedLines.size() >= chunkSize) {
					break;
				}
			}
		} catch (IOException ex) {
			throw new UnhandledException(ex);
		}
		return parsedLines.size() > 0 ? parsedLines : null;
	}
	
}
