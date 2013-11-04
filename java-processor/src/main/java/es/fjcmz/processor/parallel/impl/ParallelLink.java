package es.fjcmz.processor.parallel.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import es.fjcmz.processor.Processor;
import es.fjcmz.processor.ProcessorFactory;
import es.fjcmz.processor.UnhandledException;

/**
 * A link to be used internally in a {@link ParallelRunner} to separate
 * different types of {@link Processor}s groups in a parallel execution flow. <br>
 * Each link is related to a {@link ProcessorFactory} that will provide the
 * {@link Processor}s that execute on products and provide results. <br>
 * 
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public interface ParallelLink<P, R> {

	/**
	 * Setter for the {@link ProcessorFactory} that this link will use to create
	 * {@link Processor}s to process on received products.
	 * 
	 * @param processorFactory
	 */
	void setProcessorFactory(ProcessorFactory<P, R> processorFactory);

	/**
	 * Setter for the context that will control the production throttling on the
	 * maximum products that this link accepts.
	 * 
	 * @param context
	 */
	void setProductsCoordinationContext(ParallelCoordinationContext context);

	/**
	 * Setter for the context that will control the production throttling on the
	 * results that this link produces.
	 * 
	 * @param context
	 */
	void setResultsCoordinationContext(ParallelCoordinationContext context);

	/**
	 * Called before {@link #start()}.
	 * 
	 * @throws UnexpectedException
	 */
	void prepare() throws UnhandledException;

	/**
	 * Called after {@link #stop()}.
	 * 
	 * @throws UnexpectedException
	 */
	void dispose() throws UnhandledException;

	/**
	 * Starts the execution of this link so that it starts taking products and
	 * processes them via {@link Processor}s in concurrent threads.
	 * 
	 * @throws UnexpectedException
	 */
	void start() throws UnhandledException;

	/**
	 * Requests the stop of this link.
	 * 
	 * @throws UnexpectedException
	 */
	void stop() throws UnhandledException;

	/**
	 * Setter for the {@link BlockingQueue} that provides products to this link.
	 * 
	 * @param productsQueue
	 */
	void setProductsQueue(BlockingQueue<P> productsQueue);

	/**
	 * Setter for the {@link BlockingQueue} where the results created by this
	 * link are put.
	 * 
	 * @param resultsQueue
	 */
	void setResultsQueue(BlockingQueue<R> resultsQueue);

	/**
	 * Setter for the {@link BlockingQueue} queue used to notify the
	 * {@link ParallelRunner} of the end of production of this link.
	 * 
	 * @param notificationsQueue
	 */
	void setNotificationsQueue(BlockingQueue<String> notificationsQueue);

	/**
	 * Gets the {@link ProcessorFactory} that this {@link ParallelLink} uses.
	 * 
	 * @return
	 */
	ProcessorFactory<P, R> getProcessorFactory();

	/**
	 * Getter for the {@link ThreadPoolExecutor} that will process the multiple
	 * threads of this parallel link.
	 * 
	 * @return
	 */
	ThreadPoolExecutor getThreadPoolExecutor();

	/**
	 * Set the {@link ParallelRunnerInfo} related to the {@link ParallelRunner}
	 * where this {@link ParallelLink} is running.
	 * 
	 * @param info
	 */
	void setParallelRunnerInfo(ParallelRunnerInfo info);

}
