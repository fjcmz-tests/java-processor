package es.fjcmz.processor.parallel.impl;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;

import es.fjcmz.processor.BaseMultipartProcessor;
import es.fjcmz.processor.BaseProcessor;
import es.fjcmz.processor.Processor;
import es.fjcmz.processor.ProcessorFactory;
import es.fjcmz.processor.Runner;
import es.fjcmz.processor.UnhandledException;

/**
 * A parallel executor that can execute a flow of parallel processing based on a
 * producer-consumer pattern between several stages of processors chained
 * together so that the results from one stage are the products for the next. <br>
 * Processing stages are represented by a {@link ProcessorFactory} that can
 * provide {@link Processor}s as needed to execute the processing on arriving
 * products. <br>
 * Stages are added via the {@link #chain(ProcessorFactory, int, int, double)} method to
 * connect the given processor factory with the last one. Checks take place to
 * ensure that types produced and taken by the stages being connected are
 * compatible. <b> <br>
 * Production throttling can be specified when chaining a new stage with the
 * {@link #chain(ProcessorFactory, int, int, Long)} method, to specify the
 * maximum number of concurrent products awaiting to be processed that are
 * allowed before production is stopped until it can be resumed again. <br>
 * The default implementation is {@link ParallelRunnerImpl}. <br>
 * 
 * @author "Javier Cano"
 * 
 * @see ParallelRunnerImpl
 * @see ProcessorFactory
 * @see Processor
 * @see BaseProcessor
 * @see BaseMultipartProcessor
 * 
 */
public interface ParallelRunner extends Runner {

	/**
	 * Gets the name for this {@link ParallelRunner}. Unique names are not
	 * enforced, but it makes monitoring and identifying ParallelRunners much
	 * more easy. <br>
	 * 
	 * @return
	 */
	@Override
    String getName();

	/**
	 * Adds a new stage as {@link #chain(ProcessorFactory, int, int, double)}, but adds
	 * production throttling. <br>
	 * If the number of concurrent products awaiting processing between the
	 * previous stage and the new one goes higher than 'maxconcurrentProducts',
	 * production on the previous stage will be halted until enough slots become
	 * available again.
	 * 
	 * @param processorFactory
	 * @param instances
	 * @param queueCapacity
	 * @param maxConcurrentProducts
	 * @return
	 */
	<P, R> ParallelRunner chain(ProcessorFactory<P, R> processorFactory, int instances, int queueCapacity,
			Long maxConcurrentProducts);

	/**
     * Executes this full parallel flow on the given initial products. <br>
     * The products will be processed by each stage, via the {@link Processor}s
     * in each {@link ProcessorFactory} in each stage. <br>
     * This method blocks until all processing is finished. <br>
     * Production throttling will automatically take place as specified when
     * building the {@link ParallelRunner} via {@link #chain(ProcessorFactory, int, int, double)} methods.
     * 
     * @param initialProducts
     * @throws UnexpectedException
     */
    @Override
    void executeOn(Collection<?> initialProducts, Object nullProduct) throws UnhandledException;
	
	/**
	 * Gets the type of the input elements.
	 * 
	 * @return
	 */
	Class<?> getInputType();

	/**
	 * Gets the type of the last produced elements.
	 * 
	 * @return
	 */
	Class<?> getOutputType();

	/**
	 * Gets the initial input queue, the one that feeds the initial stage.
	 * 
	 * @return
	 */
	BlockingQueue<?> getInputQueue();

	// //
	//
	// //

	String END_OF_PRODUCTION_MESSAGE = "END_OF_PRODUCTION";

}
