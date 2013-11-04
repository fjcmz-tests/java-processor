package es.fjcmz.processor;

import es.fjcmz.processor.parallel.impl.ParallelCoordinationContext;
import es.fjcmz.processor.parallel.impl.ProductionThrottled;

/**
 * Base class for a {@link Processor}. <br>
 * Subclasses need only worry to implement the {@link #doProcess(Object)} to
 * execute the desired processing on the given product and provide a result for
 * it. <br>
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public abstract class BaseProcessor<P, R> implements Processor<P, R>, ProductionThrottled {

	protected String name = null;

	protected ParallelCoordinationContext productsCoordinationContext = null;
	protected ParallelCoordinationContext resultsCoordinationContext = null;

	// //

    public BaseProcessor(String name) {
        this.name = name;
    }
	
	// //

	@Override
	public String getName() {
		return this.name;
	}
	
	// //

	/**
	 * Made final with specific flow control for exception handling and
	 * production throttling. <br>
	 * Subclasses must implement {@link #doProcess(Object)};
	 */
	@Override
	public final ProcessorResult<P, R> process(P product) throws UnhandledException {
		R result = null;
		try {
			result = doProcess(product);
		} finally {
			controlProductThrottling(product);
			controlResultThrottling(result);
		}
		return new ProcessorResult<P, R>(product, result);
	}

	/**
	 * Control of production throttling, will make the previous producer stop on
	 * producing if too many products are waiting processing or resume if if it
	 * was stopped and can start again.
	 * 
	 * @param product
	 */
	protected final void controlProductThrottling(P product) {
		if (product != null) {
			// If there is any throttling control for the products we can
			// accept, control it here
			if (productsCoordinationContext != null) {
				// Inform we have consumed one product.
				productsCoordinationContext.consumed();
			}
		}
	}

	/**
	 * Control of production throttling, will make this producer stop on
	 * producing if too many products are waiting processing or resume if if it
	 * was stopped and can start again.
	 * 
	 * @param result
	 */
	protected final void controlResultThrottling(R result) {
		if (result != null) {
			// If there is any throttling control for the results we can
			// produce, control it here
			if (resultsCoordinationContext != null) {
				// Try the lock that controls results from being produced.
				// Others can have taken this lock to prevent further production
				resultsCoordinationContext.tryLock();
			}
			if (resultsCoordinationContext != null) {
				// Inform we have produced one result
				resultsCoordinationContext.produced();
			}
		}
	}
	
    // //
    // Main cycle
    // //

	/**
	 * Subclasses must implement the actual processing here. <br>
	 * {@link #prepare()} is called before {@link #process(Object)}. <br>
	 * {@link #dispose()} is called once {@link #process(Object)} has finished.
	 */
	protected abstract R doProcess(P product) throws UnhandledException;

	/**
	 * Subclasses must implement the desired actions before starting processing
	 * here. <br>
	 * This is called only once for {@link Processor} before calling
	 * {@link #process(Object)}.
	 */
	@Override
	public void prepare() throws UnhandledException {
		// for subclasses to implement
	}

	/**
	 * Subclasses must implement the desired actions after all processing has
	 * taken place here. <br>
	 * This is called only once for {@link Processor} after all processing in
	 * {@link #process(Object)} has taken place.
	 */
	@Override
	public void dispose() throws UnhandledException {
		// for subclasses to implement
	}

	// //

	@Override
	public ParallelCoordinationContext getProductsCoordinationContext() {
		return productsCoordinationContext;
	}

	@Override
	public void setProductsCoordinationContext(ParallelCoordinationContext context) {
		this.productsCoordinationContext = context;
	}

	@Override
	public ParallelCoordinationContext getResultsCoordinationContext() {
		return this.resultsCoordinationContext;
	}

	@Override
	public void setResultsCoordinationContext(ParallelCoordinationContext context) {
		this.resultsCoordinationContext = context;
	}

}
