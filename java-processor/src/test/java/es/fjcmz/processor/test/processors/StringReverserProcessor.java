package es.fjcmz.processor.test.processors;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import es.fjcmz.processor.BaseProcessor;
import es.fjcmz.processor.UnhandledException;

public class StringReverserProcessor extends BaseProcessor<List<String[]>, List<String[]>> {

	public StringReverserProcessor(String name) {
		super(name);
	}

	@Override
	protected List<String[]> doProcess(List<String[]> product)
			throws UnhandledException {
		return reverse(product);
	}
	
	// //
	
	protected List<String[]> reverse(List<String[]> strs) {
		if (strs == null) {
			return null;
		}
		List<String[]> reversedStrs = Lists.newArrayList();
		for (int i = strs.size() - 1; i >= 0; i--) {
			reversedStrs.add(reverse(strs.get(i)));
		}
		return reversedStrs;
	}
	
	protected String[] reverse(String[] strs) {
		if (strs == null) {
			return null;
		}
		String[] reversedStrs = new String[strs.length];
		for(int i = 0; i < strs.length; i++) {
			reversedStrs[reversedStrs.length - 1 - i] = reverse(strs[i]);
		}
		return reversedStrs;
	}
	
	protected String reverse(String str) {
		return StringUtils.reverse(name);
	}

}
