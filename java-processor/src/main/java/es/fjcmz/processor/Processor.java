package es.fjcmz.processor;

import es.fjcmz.processor.parallel.impl.ParallelRunner;


/**
 * Representation of a basic unit of processing for a parallel processing flow. <br>
 * It takes objects of type P and produces results of type R. <br>
 * Uses the concept of a NULL instance object to represent the null value where
 * an explicit null is not allowed. <br>
 * {@link Processor}s are created by {@link ProcessorFactory}s during the
 * execution of a {@link ParallelRunner} to process each product that flows
 * through the execution. One new {@link Processor} is created for each product.
 * {@link Processor}s are not reused. <br>
 * The lifecycle for a {@link Processor} is:
 * <ol>
 * <li>instantiation</li>
 * <li>{@link #prepare()}</li>
 * <li>{@link #process(Object)}</li>
 * <li>{@link #dispose()}</li>
 * </ol>
 * {@link BaseProcessor} provides a base implementation that clients should use.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 * 
 * @see ProcessorFactory
 * @see ParallelRunner
 * @see BaseProcessor
 * @see MultiPartProcessor
 * @see BaseMultipartProcessor
 */
public interface Processor<P, R> {

	/**
	 * Main method that must process the given product and provide a result. <br>
	 * 
	 * @param product
	 * @return
	 * @throws UnexpectedException
	 */
	ProcessorResult<P, R> process(P product) throws UnhandledException;

	/**
	 * Called immediately before {@link #process(Object)}, any initialization to
	 * make the processing happen should be done here.
	 * 
	 * @throws UnexpectedException
	 */
	void prepare() throws UnhandledException;

	/**
	 * Called immediately after {@link #process(Object)}, any clean can take
	 * place here.
	 * 
	 * @throws UnexpectedException
	 */
	void dispose() throws UnhandledException;

	/**
	 * Name, hopefully unique, for this {@link Processor}.
	 * 
	 * @return
	 */
	String getName();

}
