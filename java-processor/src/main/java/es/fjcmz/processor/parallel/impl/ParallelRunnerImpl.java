package es.fjcmz.processor.parallel.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import es.fjcmz.processor.BaseRunnerImpl;
import es.fjcmz.processor.Processor;
import es.fjcmz.processor.ProcessorFactory;
import es.fjcmz.processor.RunnerStatus;
import es.fjcmz.processor.UnhandledException;

/**
 * Implementation of {@link ParallelRunner}.
 * 
 * @author "Javier Cano"
 * 
 * @see Processor
 * @see ProcessorFactory
 */
public class ParallelRunnerImpl extends BaseRunnerImpl implements ParallelRunner {

	private static final Logger LOG = LoggerFactory.getLogger(ParallelRunnerImpl.class);

	// //
	// Fields and constants
	// //

	protected ParallelRunnerInfo info = null;
	
	protected List<ParallelLink<?, ?>> allLinks = new ArrayList<>();
	protected List<ProcessorFactory<?, ?>> allProcessorFactories = new ArrayList<>();
	protected List<BlockingQueue<?>> allQueues = new ArrayList<>();
	protected List<ParallelCoordinationContext> allContexts = new ArrayList<>();
	protected List<BlockingQueue<String>> notificationQueues = new ArrayList<>();

	protected ParallelCoordinator coordinator = null;
	protected int coordinatorQueueSize = 0;
	
	protected int linksCounter = 1;

	// //
	// 
	// //

	/**
	 * Instantiation requires an initial stage, represented by a
	 * {@link ProcessorFactory} and information about concurrent processors
	 * desired.
	 * 
	 * @param processorFactory
	 * @param instances
	 * @param queuesCapacity
	 */
	public <P, R> ParallelRunnerImpl(ProcessorFactory<P, R> processorFactory, int instances, int queuesCapacity) {
		this(null,processorFactory, instances, queuesCapacity);
	}
	
	/**
	 * 
	 * Instantiation requires an initial stage, represented by a
	 * {@link ProcessorFactory} and information about concurrent processors
	 * desired.
	 * 
	 * @param name
	 * @param processorFactory
	 * @param instances
	 * @param queuesCapacity
	 */
	public <P, R> ParallelRunnerImpl(String name, ProcessorFactory<P, R> processorFactory, int instances, int queuesCapacity) {
		Preconditions.checkArgument(queuesCapacity > 0, "Must provide a positive capacity.");
		if (name == null) {
			name = ParallelRunnerStats.getInstance().createUniqueName(this);
		}
		this.name = name;
		// Prepare the first link of the parallel chain.
		BlockingQueue<P> productsQueue = null;
		BlockingQueue<String> notificationsQueue = new ArrayBlockingQueue<>(10);
		ParallelLink<P, R> parallelLink = null;
		productsQueue = new ArrayBlockingQueue<>(queuesCapacity);
		allQueues.add(productsQueue);
		notificationQueues.add(notificationsQueue);
		parallelLink = createParallelLink(processorFactory, instances, productsQueue, null, notificationsQueue, queuesCapacity);
		allLinks.add(parallelLink);
		allProcessorFactories.add(processorFactory);
		coordinatorQueueSize = queuesCapacity;
		//
		info = new ParallelRunnerInfo();
		info.setParallelRunner(this);
		info.getProcessorFactories().add(processorFactory);
	}

	// //
	//
	// //

	@Override
	public <P, R> ParallelRunner chain(ProcessorFactory<P, R> processorFactory, int instances, int queueCapacity,
			Long maxConcurrentProducts) {
		// Set null product of given factory as the null result from previous factory
		Object nullInstance = allProcessorFactories.get(allProcessorFactories.size() - 1).getNullResult();
		processorFactory.setNullProduct((P)nullInstance);
		BlockingQueue<P> newPipeQueue = null;
		BlockingQueue<String> notificationsQueue = new ArrayBlockingQueue<>(10);
		ParallelLink<P, R> parallelLink = null;
		newPipeQueue = new ArrayBlockingQueue<>(queueCapacity);
		allQueues.add(newPipeQueue);
		notificationQueues.add(notificationsQueue);
		ParallelLink<?, P> previousLink = (ParallelLink<?, P>) allLinks.get(allLinks.size() - 1);
		previousLink.setResultsQueue(newPipeQueue);
		// Create the ParallelLink for this stage
		parallelLink = createParallelLink(processorFactory, instances, newPipeQueue, null, notificationsQueue, queueCapacity);
		if (maxConcurrentProducts != null) {
			// If some production throttling has been specified, set it up, with
			// a new ParallelCoordinatorContext that connects both stages
			ParallelCoordinator coordinator = getCoordinator();
			ParallelCoordinationContext context = coordinator.newContext(maxConcurrentProducts);
			parallelLink.setProductsCoordinationContext(context);
			allLinks.get(allLinks.size() - 1).setResultsCoordinationContext(context);
			allContexts.add(context);
		}
		allLinks.add(parallelLink);
		allProcessorFactories.add(processorFactory);
		if (queueCapacity > coordinatorQueueSize) {
			// Update the size of the coordinator queue to keep the biggest one.
			coordinatorQueueSize = queueCapacity;
		}
		// allow chaining.
		info.getProcessorFactories().add(processorFactory);
		return this;
	}

	public ParallelCoordinator getCoordinator() {
		if (coordinator == null) {
			// This is only requested when needed.
			// Lazy initialization here
			coordinator = new ParallelCoordinator();
		}
		return coordinator;
	}

	protected void prepare() throws UnhandledException {
		// inform this ParallelRunner is started.
		ParallelRunnerStats.getInstance().parallelRunnerStarted(info);
	}

	protected void dispose() throws UnhandledException {
		// Stop and dispose all links
		for (ParallelLink<?, ?> link : allLinks) {
			link.stop();
			link.dispose();
		}
		// inform this ParallelRUnner is stopped.
		ParallelRunnerStats.getInstance().parallelRunnerDisposed(info);
	}

	@Override
	public void executeOn(Collection<?> initialProducts, Object nullProduct) throws UnhandledException {
	    if (initialProducts == null || initialProducts.isEmpty()) {
	        LOG.debug("Requested ParallelRunner processing on no products.");
	        return;
	    }
	    try {
    		// set the null product for the first factory
    		ProcessorFactory<Object, Object> firstFactory = (ProcessorFactory<Object, Object>) allProcessorFactories.get(0);
    		firstFactory.setNullProduct(nullProduct);
    		// Prepare this ParallelRunner
    		prepare();
    		// Prepare coordinator, the thread will be null if no coordinator is
    		// needed
    		Thread coordinatorThread = prepareCoordinator();
    		// prepare the links
    		prepareLinks();
    		setStatus(RunnerStatus.PREPARED);
    		// put the initial products in the initial queue
    		queueProducts(initialProducts);
    		// Start links, processing of initial products will start and the flow
    		// will be in execution until all stages are done.
    		startLinks();
    		setStatus(RunnerStatus.RUNNING);
    		// Wait for all stages and the coordinator to finish
    		waitToFinish(coordinatorThread);
    		// Dispose, stop everything.
    		dispose();
    		setEndStatus();
	    } catch (Throwable t) {
	        LOG.error("Error running ParallelRunnerImpl " + getName(), t);
	        setStatus(RunnerStatus.ERROR);
	    }
	}
	
	protected Thread prepareCoordinator() {
		if (coordinator != null) {
			Thread coordinatorThread = null;
			coordinator.prepareCoordinatorQueue(coordinatorQueueSize);
			coordinatorThread = new Thread(coordinator, "ParallelCoordinator");
			coordinatorThread.start();
			return coordinatorThread;
		}
		return null;
	}

	protected <P> void queueProducts(Collection<P> initialProducts) {
		BlockingQueue<P> initialQueue = (BlockingQueue<P>) allQueues.get(0);
		for (P product : initialProducts) {
			initialQueue.add(product);
		}
		P nullProduct = (P) allProcessorFactories.get(0).getNullProduct();
		initialQueue.add(nullProduct);
	}

	protected void prepareLinks() throws UnhandledException {
		for (ParallelLink<?, ?> link : allLinks) {
			link.prepare();
			// update ParallelRunnerInfo
			link.setParallelRunnerInfo(info);
			info.getMapFactory2ThreadPool().put(link.getProcessorFactory(), link.getThreadPoolExecutor());
		}
	}

	protected void startLinks() throws UnhandledException {
		ParallelLink<?, ?> link = null;
		// Links are started in reverse order to prevent race conditions from
		// happening while setting up the whole parallel flow.
		for (int i = allLinks.size() - 1; i >= 0; i--) {
			link = allLinks.get(i);
			link.start();
		}
	}

	/**
	 * Helper to wait until all stages and the coordinator are finished
	 * processing. <br>
	 * Blocks until the whole parallel execution flow is done.
	 * 
	 * @param coordinatorThread
	 */
	protected void waitToFinish(Thread coordinatorThread) {
		try {
			String message = null;
			int howMany = 0;
			for (BlockingQueue<String> notificationQ : notificationQueues) {
				message = notificationQ.take();
				howMany++;
				LOG.info("ParallelRunner received {} ({})", message, howMany);
			}
		} catch (InterruptedException ex) {
			LOG.error("Exception waiting for threads to end production.", ex);
		} finally {
			if (coordinatorThread != null) {
				try {
					coordinator.getCoordinatorQueue().put(ParallelCoordinator.NULL_OBJECT);
					coordinatorThread.join();
				} catch (InterruptedException ex) {
					LOG.error("Error stopping parallel coordinator.", ex);
				}
			}
		}
	}

	// //
	//
	// //

	@Override
	public Class<?> getInputType() {
		return allProcessorFactories.get(0).getProductType();
	}

	@Override
	public Class<?> getOutputType() {
		return allProcessorFactories.get(allProcessorFactories.size() - 1).getResultType();
	}

	@Override
	public BlockingQueue<?> getInputQueue() {
		return allQueues.get(0);
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
    public void setName(String name) {
		this.name = name;
	}

	// //
	//
	// //

	/**
	 * Helper to create a new {@link ParallelLink} of correct implementation.
	 * 
	 * @param processorFactory
	 * @param instances
	 * @param productsQueue
	 * @param resultsQueue
	 * @param notificationsQueue
	 * @param queueCapacity
	 * @return
	 */
	protected <P, R> ParallelLink<P, R> createParallelLink(ProcessorFactory<P, R> processorFactory, int instances,
			BlockingQueue<P> productsQueue, BlockingQueue<R> resultsQueue, BlockingQueue<String> notificationsQueue,
			int queueCapacity) {
		ParallelLink<P, R> parallelLink = null;
		String linkName = "Link_for_" + processorFactory.getClass().getName() + "_" + linksCounter;
		parallelLink = new MultiThreadedParallelLink<>(linkName, processorFactory, productsQueue, resultsQueue,
				notificationsQueue, instances, instances, queueCapacity);
		linksCounter++;
		return parallelLink;
	}

}
