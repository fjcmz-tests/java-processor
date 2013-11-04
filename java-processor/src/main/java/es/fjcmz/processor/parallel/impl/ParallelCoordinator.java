package es.fjcmz.processor.parallel.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.fjcmz.processor.BaseMultipartProcessor;
import es.fjcmz.processor.BaseProcessor;
import es.fjcmz.processor.Processor;

/**
 * A coordinator of production throttling that runs in a separate thread in a
 * {@link ParallelRunner}. <br>
 * Provides {@link ParallelCoordinationContext}s to those {@link ParallelLink}s
 * that can have the production throttled. <br>
 * Receives messages as the {@link Processor}s produce and consume items and
 * manages the locks that directly control the production throttling. <br>
 * This class is managed internally by the {@link ParallelRunnerImpl} and
 * clients of {@link BaseProcessor} or {@link BaseMultipartProcessor} need not
 * worry about it.
 * 
 * @author "Javier Cano"
 * 
 */
public class ParallelCoordinator implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ParallelCoordinator.class);
	public static final Object NULL_OBJECT = new Object();

	// //

	protected BlockingQueue<Object> coordinatorQueue = null;

	protected int contextsCounter = 0;
	protected Map<Object, ParallelCoordinationContext> mapProduce2Context = new HashMap<>();
	protected Map<Object, ParallelCoordinationContext> mapConsume2Context = new HashMap<>();

	protected Exception exception = null;

	// //

	/**
	 * Produces a new {@link ParallelCoordinationContext} that stops production
	 * when 'maxConcurrentProducts' is reached and resumes production when the
	 * number of concurrent products for this context drops below
	 * 'maxConcurrentProducts/2'. <br>
	 * 
	 * @param maxConcurrentProducts
	 * @return
	 */
	public ParallelCoordinationContext newContext(long maxConcurrentProducts) {
		Object produceObject = new Object();
		Object consumeObject = new Object();
		Lock lock = new ReentrantLock();
		Long productsCounter = 0L;
		// Create a new ParallelCoordinationContext.
		ParallelCoordinationContext context = new ParallelCoordinationContext(contextsCounter++, produceObject,
				consumeObject, lock, null, maxConcurrentProducts, productsCounter);
		// Keep its reference in two inverted indexes to find it quickly when
		// receiving messages
		// One index from produced objects to context
		mapProduce2Context.put(produceObject, context);
		// Another index from consumed objects to context
		mapConsume2Context.put(consumeObject, context);
		return context;
	}

	public BlockingQueue<Object> getCoordinatorQueue() {
		return coordinatorQueue;
	}

	/**
	 * Called to prepare the coordinator queue with the specified size. <br>
	 * 
	 * @param size
	 */
	public void prepareCoordinatorQueue(int size) {
		this.coordinatorQueue = new ArrayBlockingQueue<>(size);
		for (ParallelCoordinationContext context : mapProduce2Context.values()) {
			context.setCoordinatorQueue(coordinatorQueue);
		}
	}

	// //

	/**
	 * Iterates taking messages from the coordinatorQueue from
	 * {@link ParallelCoordinationContext}es. <br>
	 * On those messages this coordinator controls production throttling for the
	 * different {@link ParallelLink}s and {@link Processor}s. <br>
	 * Will stop when the {@link #NULL_OBJECT} is received from the
	 * {@link ParallelRunner} where this runs.
	 * 
	 */
	@Override
	public void run() {
		try {
			LOG.info("Starting Parallel Coordinator.");
			Object o = null;
			ParallelCoordinationContext context = null;
			while (true) {
				// Iterate taking messages form the coordinator queue.
				// ParallelCoordinatorContextses are the ones that put messages
				// in this queue.
				o = getCoordinatorQueue().take();
				if (NULL_OBJECT == o) {
					// This message indicates the end of processing, so break
					// and stop.
					LOG.info("Parallel Coordinator received null object.");
					break;
				}
				// Each message is meant to be of produce or consume types.
				// Try the production map
				context = mapProduce2Context.get(o);
				if (context != null) {
					// Control production throttling because of an item produced
					// for this context.
					producedInContext(context);
				} else {
					// Try the consumption map
					context = mapConsume2Context.get(o);
					if (context != null) {
						// Control production throttling because of an item
						// consumed
						// for this context.
						consumedInContext(context);
					} else {
						// Unknown message, ignore
						LOG.warn("Unknown object.");
					}
				}
			}
			LOG.info("Stopping Parallel Coordinator.");
		} catch (Exception ex) {
			// On an exception, stop and store the exception for analysis
			LOG.error("Exception running coordinator.", ex);
			this.exception = ex;
		}
	}

	/**
	 * Helper to control production throttling when an item has been produced in
	 * the given context.
	 * 
	 * @param context
	 */
	protected void producedInContext(ParallelCoordinationContext context) {
		Long counter = context.getProductsCounter();
		synchronized (context) {
			counter++;
			context.setProductsCounter(counter);
			// Check number of concurrent items
			if (counter >= context.getMaxConcurrentProducts()) {
				// and lock if we have reached the maximums
				context.lock();
			}
		}
	}

	/**
	 * Helper to control production throttling when an item has been consumed in
	 * the given context.
	 * 
	 * @param context
	 */
	protected void consumedInContext(ParallelCoordinationContext context) {
		Long counter = context.getProductsCounter();
		synchronized (context) {
			counter--;
			context.setProductsCounter(counter);
			// Check number of concurrent items
			if (counter <= context.getResumeProductionConcurrentProducts()) {
				// and unlock if there is free room available.
				context.unlock();
			}
		}
	}

}
