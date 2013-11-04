package es.fjcmz.processor.parallel.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.fjcmz.processor.BaseProcessor;
import es.fjcmz.processor.MultiPartProcessor;
import es.fjcmz.processor.Processor;

/**
 * A context that holds information for throttling of the production between two
 * {@link ParallelLink}s during the execution of a {@link ParallelRunner}. <br>
 * Used and managed internally, clients of {@link BaseProcessor} and
 * {@link MultiPartProcessor} need not worry about this.
 * 
 * @author "Javier Cano"
 * 
 */
public class ParallelCoordinationContext {

	private static final Logger LOG = LoggerFactory.getLogger(ParallelCoordinationContext.class);

	// //

	protected int id = -1;

	protected Object produceObject = null;
	protected Object consumeObject = null;
	protected Lock lock = null;
	protected BlockingQueue<Object> coordinatorQueue = null;

	protected long maxConcurrentProducts = -1;
	protected long resumeProductionConcurrentProducts = -1;
	protected Long productsCounter = null;

	protected boolean locked = false;

	/**
	 * Use
	 * {@link #ParallelCoordinationContext(int, Object, Object, Lock, BlockingQueue, long, Long)}
	 * <br>
	 * 
	 * @deprecated
	 */
	@Deprecated
	public ParallelCoordinationContext() {
	}

	/**
	 * Constructor with all parameters.
	 * 
	 * @param id
	 * @param produceObject
	 * @param consumeObject
	 * @param lock
	 * @param coordinatorQueue
	 * @param maxConcurrentProducts
	 * @param productsCounter
	 */
	public ParallelCoordinationContext(int id, Object produceObject, Object consumeObject, Lock lock,
			BlockingQueue<Object> coordinatorQueue, long maxConcurrentProducts, Long productsCounter) {
		super();
		this.id = id;
		this.produceObject = produceObject;
		this.consumeObject = consumeObject;
		this.lock = lock;
		this.coordinatorQueue = coordinatorQueue;
		this.maxConcurrentProducts = maxConcurrentProducts;
		this.resumeProductionConcurrentProducts = maxConcurrentProducts / 2;
		this.productsCounter = productsCounter;
	}

	/**
	 * Identifier of this context, unique within the {@link ParallelRunner} in
	 * which it runs.
	 * 
	 * @return
	 */
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * The object that represents a produce message.
	 * 
	 * @return
	 */
	public Object getProduceObject() {
		return produceObject;
	}

	public void setProduceObject(Object produceObject) {
		this.produceObject = produceObject;
	}

	/**
	 * The object that represents a consume message.
	 * 
	 * @return
	 */
	public Object getConsumeObject() {
		return consumeObject;
	}

	public void setConsumeObject(Object consumeObject) {
		this.consumeObject = consumeObject;
	}

	/**
	 * The {@link Lock} used to throttle production.
	 * 
	 * @return
	 */
	public Lock getLock() {
		return lock;
	}

	public void setLock(Lock lock) {
		this.lock = lock;
	}

	/**
	 * The maximum number of concurrent products that can exist before
	 * production is stopped.
	 * 
	 * @return
	 */
	public long getMaxConcurrentProducts() {
		return maxConcurrentProducts;
	}

	public void setMaxConcurrentProducts(long maxConcurrentProducts) {
		this.maxConcurrentProducts = maxConcurrentProducts;
	}

	/**
	 * The threshold value under which the number of concurrent products must go
	 * after stopping production to resume it again.
	 * 
	 * @return
	 */
	public long getResumeProductionConcurrentProducts() {
		return resumeProductionConcurrentProducts;
	}

	public void setResumeProductionConcurrentProducts(long resumeProductionConcurrentProducts) {
		this.resumeProductionConcurrentProducts = resumeProductionConcurrentProducts;
	}

	/**
	 * The counter with the current number of concurrent products.
	 * 
	 * @return
	 */
	public Long getProductsCounter() {
		return productsCounter;
	}

	public void setProductsCounter(Long productsCounter) {
		this.productsCounter = productsCounter;
	}

	/**
	 * The {@link BlockingQueue} that can communicate this
	 * {@link ParallelCoordinationContext} with the {@link ParallelCoordinator}.
	 * 
	 * @return
	 */
	public BlockingQueue<Object> getCoordinatorQueue() {
		return coordinatorQueue;
	}

	public void setCoordinatorQueue(BlockingQueue<Object> coordinatorQueue) {
		this.coordinatorQueue = coordinatorQueue;
	}

	// //

	/**
	 * Locks and unlocks on the {@link Lock} of this context. <br>
	 * Used by {@link Processor}s to control the throttling of results produced.
	 */
	public void tryLock() {
		try {
			lock.lock();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Locks on this context's {@link Lock}.
	 */
	public void lock() {
		synchronized (this) {
			if (!locked) {
				LOG.trace("Locking {}", getId());
				lock.lock();
				LOG.trace("Locked {}", getId());
				locked = true;
			}
		}
	}

	/**
	 * Unlocks on this context's {@link Lock}.
	 */
	public void unlock() {
		synchronized (this) {
			if (locked) {
				LOG.trace("Unlocking {}", getId());
				lock.unlock();
				LOG.trace("Unlocked {}", getId());
				locked = false;
			}
		}
	}

	/**
	 * Called when a product is produced in a {@link Processor}. <br>
	 * Will the notify the {@link ParallelCoordinator} and any necessary
	 * throttling control will take place.
	 */
	public void produced() {
		put(getProduceObject());
	}

	/**
	 * Called when a product is consumed in a {@link Processor}. <br>
	 * Will notify the {@link ParallelCoordinator} and any necessary throttling
	 * control will take place.
	 */
	public void consumed() {
		put(getConsumeObject());
	}

	/**
	 * Helper to send a message though the coordinatorQueue.
	 * @param o
	 */
	private void put(Object o) {
		try {
			coordinatorQueue.put(o);
		} catch (InterruptedException ex) {
			LOG.debug("Exception putting object in coordinator queue.", ex);
		}
	}

	// //

}
