package es.fjcmz.processor.parallel.impl;

import es.fjcmz.processor.BaseProcessor;
import es.fjcmz.processor.MultiPartProcessor;
import es.fjcmz.processor.Processor;

/**
 * Marker interface for those {@link Processor}s that can have its production
 * throttled. <br>
 * Throttling happens internally and client of {@link BaseProcessor} or
 * {@link MultiPartProcessor} need not control it.
 * 
 * @author "Javier Cano"
 * 
 */
public interface ProductionThrottled {

	/**
	 * The {@link ParallelCoordinationContext} to throttle the products taken.
	 * 
	 * @return
	 */
	ParallelCoordinationContext getProductsCoordinationContext();

	void setProductsCoordinationContext(ParallelCoordinationContext context);

	/**
	 * The {@link ParallelCoordinationContext} to throttle the results produced.
	 * 
	 * @return
	 */
	ParallelCoordinationContext getResultsCoordinationContext();

	void setResultsCoordinationContext(ParallelCoordinationContext context);

}
