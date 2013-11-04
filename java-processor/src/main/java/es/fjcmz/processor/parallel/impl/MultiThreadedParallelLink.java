package es.fjcmz.processor.parallel.impl;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import es.fjcmz.processor.MultiPartProcessor;
import es.fjcmz.processor.MultiPartResult;
import es.fjcmz.processor.PreparableProcessorFactory;
import es.fjcmz.processor.Processor;
import es.fjcmz.processor.ProcessorFactory;
import es.fjcmz.processor.ProcessorResult;
import es.fjcmz.processor.UnhandledException;

/**
 * A {@link ParallelLink} that executes concurrent {@link Processor}s inside a
 * {@link ThreadPoolExecutor}. <br>
 * Will start processing products in the thread pool when {@link #start()} is
 * called and automatically stop when all processing is done. <br>
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public class MultiThreadedParallelLink<P, R> implements ParallelLink<P, R> {

	private static final Logger LOG = LoggerFactory.getLogger(MultiThreadedParallelLink.class);

	// //
	//
	// //

    private static final int WAIT_AFTER_REJECTED_MS = 200;

	// //

    protected String name = null;
    
	protected ProcessorFactory<P, R> processorFactory = null;
	protected BlockingQueue<P> productsQueue = null;
	protected BlockingQueue<R> resultsQueue = null;
	protected BlockingQueue<String> notificationsQueue = null;

	protected ThreadPoolExecutor threadPoolExecutor = null;
	protected ResubmitTaskRejectedExecutorHandler rejectedExecutionHandler = null;
	protected int minThreads = -1;
	protected int maxThreads = -1;

	protected BlockingQueue<Runnable> worksQueue = null;
	protected int worksQueueCapacity = -1;

	protected Thread thisThread = null;
	protected Runnable thisRunner = null;

	protected AtomicLong allProcessorsRunning = new AtomicLong(0);
	protected volatile boolean stopping = false;
	protected volatile boolean stopped = false;

	protected ParallelCoordinationContext productsCoordinationContext = null;
	protected ParallelCoordinationContext resultsCoordinationContext = null;
	protected ParallelRunnerInfo parallelRunnerInfo = null;

	// //
	//
	// //

	/**
	 * Constructor with all required arguments.
	 * 
	 * @param processorFactory
	 * @param productsQueue
	 * @param resultsQueue
	 * @param notificaBlockingQueue
	 * @param minThreads
	 * @param maxThreads
	 * @param worksQueueCapacity
	 */
	public MultiThreadedParallelLink(String name, ProcessorFactory<P, R> processorFactory, BlockingQueue<P> productsQueue,
			BlockingQueue<R> resultsQueue, BlockingQueue<String> notificaBlockingQueue, int minThreads, int maxThreads,
			int worksQueueCapacity) {
	    this.name = name;
		Preconditions.checkNotNull(processorFactory, "Must provide a Processor Factory.");
		Preconditions.checkNotNull(productsQueue, "Must provide a products queue.");
		this.processorFactory = processorFactory;
		this.productsQueue = productsQueue;
		this.resultsQueue = resultsQueue;
		this.notificationsQueue = notificaBlockingQueue;
		Preconditions.checkArgument(minThreads > 0, "Minimum number of threads needs to be > 0");
		this.minThreads = minThreads;
		Preconditions.checkArgument(maxThreads >= minThreads,
				"Maximum number of threads needs to be >= minimum number of threads.");
		this.maxThreads = maxThreads;
		this.worksQueueCapacity = worksQueueCapacity;
	}
	
	// //

	public ParallelRunnerInfo getParallelRunnerInfo() {
		return parallelRunnerInfo;
	}
	
	@Override
	public void setParallelRunnerInfo(ParallelRunnerInfo info) {
		this.parallelRunnerInfo = info;
	}
	
	// //
	//
	// //

	@Override
	public ProcessorFactory<P,R> getProcessorFactory() {
		return this.processorFactory;
	}
	
	@Override
	public ThreadPoolExecutor getThreadPoolExecutor() {
		return this.threadPoolExecutor;
	}
	
	@Override
	public void setProcessorFactory(ProcessorFactory<P, R> processorFactory) {
		this.processorFactory = processorFactory;
	}

	@Override
	public void setProductsCoordinationContext(ParallelCoordinationContext context) {
		this.productsCoordinationContext = context;
	}

	@Override
	public void setResultsCoordinationContext(ParallelCoordinationContext context) {
		this.resultsCoordinationContext = context;
	}

	/**
	 * Prepares the {@link ThreadPoolExecutor} to use for concurrent
	 * {@link Processor}s execution.
	 * 
	 * @throws UnhandledException
	 */
	@Override
	public void prepare() throws UnhandledException {
		if (processorFactory instanceof PreparableProcessorFactory) {
			((PreparableProcessorFactory<?, ?>) processorFactory).prepare();
		}
		// Prepare thread pool executor
		rejectedExecutionHandler = new ResubmitTaskRejectedExecutorHandler();
		worksQueue = new ArrayBlockingQueue<>(worksQueueCapacity);
		threadPoolExecutor = new ProcessorPoolExecutor(minThreads, maxThreads, worksQueue);
		threadPoolExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);
		threadPoolExecutor.allowCoreThreadTimeOut(true);
		NamedThreadFactory namedThreadFactory = new NamedThreadFactory(processorFactory.getClass().getSimpleName());
		threadPoolExecutor.setThreadFactory(namedThreadFactory);
		//
		thisRunner = new ThreadedLinkRunner();
		thisThread = new Thread(thisRunner, "PL_for_" + processorFactory.getProductType().getSimpleName());
	}

	@Override
	public void dispose() throws UnhandledException {
		if (processorFactory instanceof PreparableProcessorFactory) {
			((PreparableProcessorFactory<?, ?>) processorFactory).dispose();
		}
	}

	/**
	 * Starts the concurrent processing of products that come from the input
	 * products queue.
	 * 
	 * @throws UnhandledException
	 */
	@Override
	public void start() throws UnhandledException {
		this.thisThread.start();
	}

	/**
	 * Called automatically when the processing on all products is done.
	 * 
	 * @throws UnhandledException
	 */
	@Override
	public void stop() throws UnhandledException {
		if (stopped) {
			return;
		}
		stopped = true;
		try {
			if (resultsQueue != null) {
				resultsQueue.put(processorFactory.getNullResult());
			}
		} catch (InterruptedException ex) {
			LOG.error("Exception stopping threaded executor.", ex);
		}
		notifyEndOfProduction();
		if (thisThread != null && thisThread != Thread.currentThread()) {
			try {
				// Collect the thread that waits for input products.
				thisThread.join();
			} catch (InterruptedException ex) {
				LOG.error("Exception waiting for thread to join.", ex);
			}
		}
		if (threadPoolExecutor != null) {
			// Stop the thread pool
			threadPoolExecutor.shutdown();
			threadPoolExecutor = null;
		}
	}
	
	@Override
	public void setProductsQueue(BlockingQueue<P> productsQueue) {
		this.productsQueue = productsQueue;
	}

	@Override
	public void setResultsQueue(BlockingQueue<R> resultsQueue) {
		this.resultsQueue = resultsQueue;
	}

	@Override
	public void setNotificationsQueue(java.util.concurrent.BlockingQueue<String> notificationsQueue) {
		this.notificationsQueue = notificationsQueue;
	}

	/**
	 * Helper to notify the controller thread that this link is done, used for a
	 * controlled stopping and cleanup.
	 */
	protected void notifyEndOfProduction() {
		if (notificationsQueue != null) {
			try {
				notificationsQueue.put(ParallelRunner.END_OF_PRODUCTION_MESSAGE);
				LOG.info("Sent {}", ParallelRunner.END_OF_PRODUCTION_MESSAGE);
			} catch (InterruptedException ex) {
				LOG.error("Exception while notifying end of production.");
			}
		}
	}

	// //

	/**
	 * Helper to make sure that the given Callable is submitted to the thread
	 * pool executor, waiting for free space if necessary. <br>
	 * This will retry on rejections of a job until it has been submitted
	 * correctly, waiting {@value #WAIT_AFTER_REJECTED_MS} ms for each retry to
	 * make some free space in the thread pool queue. If the processing is
	 * delayed too much by too frequent rejections, it is better to provide
	 * bigger queues and/or production throttling options on the
	 * {@link ParallelRunner#chain(ProcessorFactory, int, int, Long)} method.
	 * 
	 * @param callable
	 */
	protected void submitToThreadPoolExecutor(Callable<ProcessorResult<P, R>> callable) {
		boolean submitted = false;
		while (!submitted) {
			Future<ProcessorResult<P, R>> submittedFuture = threadPoolExecutor.submit(callable);
			if (rejectedExecutionHandler.checkAndCleanRejected(submittedFuture)) {
				// This means the job was not submitted correctly.
				try {
					Thread.sleep(WAIT_AFTER_REJECTED_MS);
				} catch (InterruptedException ex) {
					LOG.debug("Exception when waiting for jobs queue to have free space.");
					continue;
				}
			} else {
				submitted = true;
			}
		}
	}

	// //
	// Auxiliary classes to support the multithreaded approach to processing.
	// //

	/**
	 * A {@link ThreadPoolExecutor} that in the
	 * {@link #afterExecute(Runnable, Throwable)} methods executes additional
	 * logic to keep track of finished {@link Processor}s to forward the results
	 * to the next stage in the {@link ParallelRunner} flow, as well as to keep
	 * track of running processors and know when to stop this link because of
	 * all processing having had taken place.
	 * 
	 * @author "Javier Cano"
	 * 
	 */
	private class ProcessorPoolExecutor extends ThreadPoolExecutor {

		public ProcessorPoolExecutor(int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue) {
			super(corePoolSize, maximumPoolSize, 30, TimeUnit.SECONDS, workQueue);
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			if (t != null) {
				LOG.error("Exception processing product.", t);
				return;
			}
			ProcessorResult<P, R> processorResult = null;
			try {
				processorResult = getResult(r);
				if (processorResult != null) {
					processResult(r, processorResult);
				}
			} catch (UnhandledException | ExecutionException | InterruptedException ex) {
				LOG.error("Exception handling result from processor.", ex);
				allProcessorsRunning.decrementAndGet();
			}
			checkStopping();
		}

		/**
		 * Gets the {@link ProcessorResult} from a submitted task. We expect it
		 * to always be a {@link ProcessorResult} as no one else should be
		 * submitting tasks to this thread pool.
		 * 
		 * @param r
		 * @return
		 * @throws ExecutionException
		 * @throws InterruptedException
		 */
		protected ProcessorResult<P, R> getResult(Runnable r) throws ExecutionException, InterruptedException {
			if (r instanceof FutureTask) {
				FutureTask<ProcessorResult<P, R>> futureTask = (FutureTask<ProcessorResult<P, R>>) r;
				return futureTask.get();
			} else {
				LOG.warn("Unknown Runnable after execute. Ignoring.");
				return null;
			}
		}

		/**
		 * Processes the result so that it is forwarded to the next stage in the
		 * processing flow via the resultsQueue and in case the
		 * {@link Processor} that finished a {@link MultiPartProcessor} and is
		 * not yet done, resubmits it into the thread pool to continue its
		 * processing.
		 * 
		 * @param processorResult
		 * @throws UnhandledException
		 * @throws InterruptedException
		 */
		protected void processResult(Runnable r, ProcessorResult<P, R> processorResult) throws UnhandledException,
				InterruptedException {
			MultiPartResult<P, R> multiPartResult = null;
			R result = null;
			if (processorResult instanceof MultiPartResult) {
				// This is a result from a MultiPartProcessor, additional
				// actions need to take place
				multiPartResult = (MultiPartResult<P, R>) processorResult;
				result = multiPartResult.getResult();
				if (multiPartResult.isLast()) {
					multiPartResult.getProcessor().dispose();
					allProcessorsRunning.decrementAndGet();
				} else {
					// Put it again in the thread pool to continue execution, as
					// it is not yet done.
					MultiPartProcessor<P, R> mulitPartProcessor = multiPartResult.getProcessor();
					Callable<ProcessorResult<P, R>> callable = new SimpleProcessorCallable<P, R>(mulitPartProcessor,
							mulitPartProcessor.getProduct());
					submitToThreadPoolExecutor(callable);
				}
			} else {
				// This is a normal result from a normal Processor
				allProcessorsRunning.decrementAndGet();
				result = processorResult.getResult();
			}
			// We may not have a result queue because we are the last stage of
			// the parallel flow.
			if (result != null && resultsQueue != null) {
				resultsQueue.put(result);
			}
		}

		protected void checkStopping() {
			if (stopping && allProcessorsRunning.get() <= 0) {
				try {
					stop();
				} catch (UnhandledException ex) {
					LOG.error("Exception stopping threaded executor.", ex);
				}
			}
		}
	}

	/**
	 * Keeps a set of the {@link Runnable}s that are rejected when submitted to
	 * the thread pool executor, to check and resubmit if necessary.
	 * 
	 * @author "Javier Cano"
	 * 
	 */
	private class ResubmitTaskRejectedExecutorHandler implements RejectedExecutionHandler {
		protected Map<Object, Object> rejectedRunnables = new ConcurrentHashMap<>();

		public boolean checkAndCleanRejected(Object o) {
			boolean rejected = rejectedRunnables.containsKey(o);
			if (rejected) {
				rejectedRunnables.remove(o);
			}
			return rejected;
		}

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			this.rejectedRunnables.put(r, DUMMY_OBJECT);
		}
	}

	/**
	 * A {@link Runnable} that takes care of grabbing products from this link's
	 * input queue and creating and submitting {@link Processor}s for them into
	 * the thread pool; until the null product that marks the end of products
	 * stream arrives.
	 * 
	 * @author "Javier Cano"
	 * 
	 */
	private class ThreadedLinkRunner implements Runnable {
		@Override
		public void run() {
			try {
				P nullProduct = processorFactory.getNullProduct();
				P product = null;
				while (!stopping) {
					// take product from queue
					product = productsQueue.take();
					while (!stopping && nullProduct == product) {
						// check whether this is really the end
						if (productsQueue.size() <= 0) {
							stopping = true;
							LOG.info("Received NullProduct {}", nullProduct);
							break;
						} else {
							product = productsQueue.take();
						}
					}
					if (product != nullProduct) {
						submitNewProcessor(product);
					}
				}
				if (stopping && allProcessorsRunning.get() <= 0) {
					stop();
				}
			} catch (Exception ex) {
				LOG.error("Error processing product.", ex);
			}
		}

		/**
		 * Submit a new {@link Processor} into the thread pool for the given
		 * product with special care for production throttling if needed.
		 * 
		 * @param product
		 */
		protected void submitNewProcessor(P product) {
			Processor<P, R> processor = null;
			SimpleProcessorCallable<P, R> callable = null;
			processor = processorFactory.createProcessor();
			if (processor instanceof ProductionThrottled) {
				ProductionThrottled coordinated = ((ProductionThrottled) processor);
				coordinated.setProductsCoordinationContext(productsCoordinationContext);
				coordinated.setResultsCoordinationContext(resultsCoordinationContext);
			}
			callable = new SimpleProcessorCallable<P, R>(processor, product);
			allProcessorsRunning.incrementAndGet();
			// Try to submit, waiting if necessary because of a full queue
			submitToThreadPoolExecutor(callable);
		}
	}

	/**
	 * A {@link Callable} that wraps the execution of a {@link Processor} inside
	 * the {@link ThreadPoolExecutor}.
	 * 
	 * @author "Javier Cano"
	 * 
	 * @param <PP>
	 * @param <RR>
	 */
	private class SimpleProcessorCallable<PP, RR> implements Callable<ProcessorResult<PP, RR>> {
		private PP product = null;
		protected ProcessorResult<PP, RR> processorResult = null;
		private Processor<PP, RR> processor = null;
		private MultiPartProcessor<PP, RR> multiPartProcessor = null;

		public SimpleProcessorCallable(Processor<PP, RR> processor, PP product) {
			this.processor = processor;
			if (processor instanceof MultiPartProcessor) {
				this.multiPartProcessor = (MultiPartProcessor<PP, RR>) processor;
			}
			this.product = product;
		}

		/**
		 * Executes the {@link Processor} and collects the result, making it
		 * available via {@link #getProcessorResult()}.
		 */
		@Override
		public ProcessorResult<PP, RR> call() throws UnhandledException {
			if (multiPartProcessor != null) {
				// A MultiPartProcessor has a different flow than a normal
				// Processor.
				if (!multiPartProcessor.isReady()) {
					multiPartProcessor.prepareFor(product);
				}
				processorResult = multiPartProcessor.processStep();
			} else {
				// A normal processor requires a prepare, process and dispose
				// flow.
				processor.prepare();
				processorResult = processor.process(product);
				processor.dispose();
			}
			return processorResult;
		}

	}
	
	/**
	 * Thread Factory to set a meaningful name to threads in the thread pool.
	 * 
	 * @author "Javier Cano"
	 *
	 */
	private class NamedThreadFactory implements ThreadFactory {
		protected String baseName = null;
		protected int counter = 0;
		public NamedThreadFactory(String baseName) {
			if (baseName == null || baseName.isEmpty()) {
				throw new IllegalArgumentException("Must provide a base name.");
			}
			this.baseName = baseName;
		}
		@Override
		public Thread newThread(Runnable r) {
			String name = baseName + counter;
			counter++;
			return new Thread(r, name);
		}
	}

	// //

	private static final Object DUMMY_OBJECT = new Object();

}
