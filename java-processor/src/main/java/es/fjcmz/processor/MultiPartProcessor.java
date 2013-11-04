package es.fjcmz.processor;

import es.fjcmz.processor.parallel.impl.ParallelRunner;


/**
 * A {@link Processor} that can have its processing divided in several steps.
 * The execution of this processing happens in several steps instead on in one
 * single block. <br>
 * This procesor lifecycle is different:
 * <ul>
 * <li>instantiation</li>
 * <li>{@link #prepareFor(Object)}</li>
 * <li>while(!{@link #isDone()}) { {@link #processStep()}</li>
 * <li>{@link #dispose()}</li>
 * </ul>
 * {@link BaseMultipartProcessor} provides a base implementation that clients
 * should use.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 * 
 * @see Processor
 * @see BaseMultipartProcessor
 * @see ProcessorFactory
 * @see ParallelRunner
 */
public interface MultiPartProcessor<P, R> extends Processor<P, R> {

	/**
	 * True when this {@link MultiPartProcessor} has had the
	 * {@link #prepareFor(Object)} method successfully called.
	 * 
	 * @return
	 */
	boolean isReady();

	/**
	 * True when this {@link MultiPartProcessor} has finished producing and
	 * subsequent calls to {@link #processStep()} will provide no more results.
	 * 
	 * @return
	 */
	boolean isDone();
	
	/**
	 * Set this {@link MultiPartProcessor} as not ready and not done.
	 * @return
	 */
	void reset();

	/**
	 * Called once before starting any processing. <br>
	 * Subclasses need to execute any preparation for the following processing
	 * here.
	 * 
	 * @param product
	 * @throws UnhandledException
	 */
	void prepareFor(P product) throws UnhandledException;

	/**
	 * Provides the product on which this {@link MultiPartProcessor} is working,
	 * if it is already set.
	 * 
	 * @return
	 */
	P getProduct();

	/**
	 * Called as long as {@link #isDone()} is false to get all the available
	 * results from this {@link MultiPartProcessor}.
	 * 
	 * @return
	 * @throws UnhandledException
	 */
	MultiPartResult<P, R> processStep() throws UnhandledException;

}
