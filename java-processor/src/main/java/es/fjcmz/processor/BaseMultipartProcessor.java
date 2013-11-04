package es.fjcmz.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.fjcmz.processor.parallel.impl.ProductionThrottled;

/**
 * Base implementation of a {@link MultiPartProcessor}. <br>
 * Subclasses need only worry to implement {@link #doPrepareFor(Object)} and
 * {@link #doProcessStep()} to process the product and provide results.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public abstract class BaseMultipartProcessor<P, R> extends BaseProcessor<P, R> implements MultiPartProcessor<P, R>,
		ProductionThrottled {

	protected boolean ready = false;
	protected boolean done = false;
	protected P product = null;

	// //

	/**
	 * Constructor with name, null representation of a product object and null
	 * representation of a result object.
	 * 
	 * @param name
	 * @param nullProduct
	 * @param nullResult
	 */
	public BaseMultipartProcessor(String name) {
		super(name);
	}

	@Override
	public boolean isReady() {
		return this.ready;
	}

	@Override
	public boolean isDone() {
		return this.done;
	}
	
	@Override
	public void reset() {
		this.ready = false;
		this.done = false;
	}

	@Override
	public P getProduct() {
		return this.product;
	}

	@Override
	public final void prepareFor(P product) throws UnhandledException {
		try {
			doPrepareFor(product);
			this.product = product;
			this.ready = true;
		} finally {
			// control that the product is consumed
			controlProductThrottling(product);
		}
	}

	/**
	 * Subclasses must implement to prepare the processing on the given product.
	 * 
	 * @param product
	 * @throws UnhandledException
	 */
	protected abstract void doPrepareFor(P product) throws UnhandledException;

	@Override
	public final MultiPartResult<P, R> processStep() throws UnhandledException {
		R result = null;
		try {
			result = doProcessStep();
			if (result != null) {
				return validResult(getProduct(), result);
			} else {
				done = true;
				return nullResult();
			}
		} finally {
			// control the result produced
			if (result != null) {
				controlResultThrottling(result);
			}
		}
	}
	
	protected MultiPartResult<P, R> validResult(P product, R result) {
	    return new MultiPartResult<P, R>(product, result, this, false);
	}
	
	protected MultiPartResult<P, R> nullResult() {
	    return new MultiPartResult<P, R>(null, null, this, true);
	}

	/**
	 * Subclasses must implement to process and provide results on the product
	 * given in {@link #doPrepareFor(Object)}.
	 * 
	 * @return
	 * @throws UnhandledException
	 */
	protected abstract R doProcessStep() throws UnhandledException;

	// //
	//
	// //

	/**
	 * Use {@link BaseMultipartProcessor#processStep()}. <br>
	 * Invalid for a {@link MultiPartProcessor}.
	 * 
	 * @deprecated
	 */
	@Deprecated
	@Override
	protected R doProcess(P product) throws UnhandledException {
		throw new UnhandledException("Use #processStep() for a MultiPartProcessor.");
	}

}
