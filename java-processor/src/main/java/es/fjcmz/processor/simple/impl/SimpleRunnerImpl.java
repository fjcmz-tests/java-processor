package es.fjcmz.processor.simple.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import es.fjcmz.processor.BaseRunnerImpl;
import es.fjcmz.processor.MultiPartProcessor;
import es.fjcmz.processor.MultiPartResult;
import es.fjcmz.processor.PreparableProcessorFactory;
import es.fjcmz.processor.Processor;
import es.fjcmz.processor.ProcessorFactory;
import es.fjcmz.processor.ProcessorResult;
import es.fjcmz.processor.RunnerStatus;
import es.fjcmz.processor.UnhandledException;

/**
 * Trivial implementation of a {@link SimpleRunner}. <br>
 * Will execute the whole processing flow based on products processed by chains of {@link Processor}s. <br>
 * To create the processing chain use {@link #chain(ProcessorFactory)} or {@link #chain(ProcessorFactory, double)} to specify
 * the load weight of the factory. <br>
 * To execute the flow on a set of products execute {@link #executeOn(Collection, Object)} or 
 * {@link #executeOn(Collection, Object, ProgressInformer)} if you want to provide a {@link ProgressInformer} that will
 * keep track of progress and that can provide that a stop has been requested. <br>
 * 
 * @author "Javier Cano"
 *
 */
public class SimpleRunnerImpl extends BaseRunnerImpl implements SimpleRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleRunnerImpl.class);
    
    // //
    // Fields and constants
    // //

    public static final double DEFAULT_FACTORY_WEIGHT = 1;
    public static final int ACCEPTABLE_PROGRESS_DEVIATION_PERCENT = 10;

	protected List<ProcessorFactory<Object, Object>> processorFactories = new ArrayList<>();
	
	protected Object nullProduct = null;

	// //
	// 
	// //

	public SimpleRunnerImpl(String name, ProcessorFactory<?, ?> processorFactory) {
		Preconditions.checkNotNull(processorFactory, "Must provide a processor factory.");
		this.name = name != null ? name : "Unnamed";
		this.processorFactories.add(getAProcessorFactory(processorFactory));
        this.nullProduct = processorFactory.getNullProduct();
	}

	@Override
	public SimpleRunner chain(ProcessorFactory<?, ?> processorFactory) {
		ProcessorFactory<Object, Object> previousFactory = processorFactories.get(processorFactories.size() - 1);
		processorFactories.add(getAProcessorFactory(processorFactory));
		// Set the null result from previous factory as the null product of the given one
		Object nullInstance = processorFactory.getNullResult();
		previousFactory.setNullProduct(nullInstance);
		return this;
	}

	@Override
	public void executeOn(Collection<?> products, Object nullProduct)
            throws UnhandledException {
	    if (products == null || products.isEmpty()) {
	        LOG.debug("Requested SimpleRunner processing on no products.");
	        return;
	    }
		/**
		 * 0) Put the null product for the first factory. <br>
		 * 1) Prepare all factories. <br>
		 * 2) Wrap the products. <br>
		 * 3) Prepare the progress information. <br>
		 * 4) Execute the chain of processing for all the products. <br>
		 * 5) Dispose all factories. <br>
		 */
	    try {
    		processorFactories.get(0).setNullProduct(nullProduct);
    		prepareProcessorFactories();
    		setStatus(RunnerStatus.RUNNING);
    		executeChainOn((Iterator<Object>) products.iterator(), processorFactories);
    		disposeProcessorFactories();
    		setEndStatus();
	    } catch (Throwable t) {
	        LOG.error("Error executing SimpleRunnerImpl " + getName(), t);
	        setStatus(RunnerStatus.ERROR);
	        throw t;
	    }
	}
	
	/**
	 * Executes the a chain of processing on the given products, keeping track of the initial product that
	 * produces this chain of execution. <br>
	 * 
	 * @param products
	 * @param processorFactories
	 * @throws UnhandledException
	 */
	protected void executeChainOn(Iterator<Object> products,
	        List<ProcessorFactory<Object, Object>> processorFactories)
			throws UnhandledException {
		if (products == null || !products.hasNext()) {
			// No products means we are done with the processing.
			return;
		}
		// The null product _may_ mark the end of production.
		Object nullProduct = getInitialNullProduct();
		Object product = null;
		Object flowingProduct = null;
		Processor<Object, Object> processor = null;
		// Iterate through all given products.
		while (products.hasNext()) {
			product = products.next();
			if (nullProduct == product) {
				// Null product that marks the end of production
				break;
			}
			flowingProduct = product;
			if (processorFactories != null) {
				// We accept having no processing factories, but if there are, we will move each product through
				// the whole chain, the result from one step being the product for the next.
				for (int i = 0; i < processorFactories.size(); i++) {
                    ProcessorFactory<Object, Object> processorFactory = processorFactories.get(i);
					// Create next processor.
                    processor = processorFactory.createProcessor();
                    // Evaluate whether there are more processors (will be needed later)
                    int nextProcessorIndex = i + 1;
                    int processorsSize = processorFactories.size();
                    boolean thereAreMoreProcessors = nextProcessorIndex <= (processorsSize - 1);
                    // and prepare the remaining processor factories
                    List<ProcessorFactory<Object, Object>> remainingProcessorFactories = thereAreMoreProcessors
                        ? processorFactories.subList(nextProcessorIndex, processorsSize)
                        : null;

                    if (processor instanceof MultiPartProcessor) {
						// A multi part processor has a bit different logic, it
						// will act as a products provider while processing, so
						// we prepare an iterator on it to make a recursive call
						// to #executeChainOn(WrappedRunnerProduct, Iterator, List<ProcessorFactory>)
						MultiPartProcessor<Object, Object> multiPartProcessor = ((MultiPartProcessor<Object, Object>) processor);
                        // Prepare the iterator over the multi part processor
                        Iterator<Object> multiPartProcessorIterator = new FromMultiPartProcessorIterable<>(
                                multiPartProcessor, flowingProduct);
                        // and call recursively with the remaining processor factories
                        executeChainOn(multiPartProcessorIterator, remainingProcessorFactories);
                        // After being done recursively, well, we are done on this product.
                        break;
					} else {
						// A normal processor just needs to process this product and prepare the
						// result as the product for the next processor
						ProcessorResult<Object, Object> processorResult = null;
						processor.prepare();
						processorResult = processor.process(flowingProduct);
						processor.dispose();
						if (processorResult != null && processorResult.getResult() != null) {
							// On a successful result, prepare it as product for the next processor
							flowingProduct = processorResult.getResult();
						} else {
                            // No more result
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * Helper to prepare any {@link PreparableProcessorFactory} via {@link PreparableProcessorFactory#prepare()}
	 * @throws UnhandledException
	 */
	protected void prepareProcessorFactories() throws UnhandledException {
		for (ProcessorFactory<?, ?> processorFactory : processorFactories) {
			if (processorFactory instanceof PreparableProcessorFactory) {
				((PreparableProcessorFactory<?, ?>) processorFactory).prepare();
			}
		}
	}

	/**
	 * Helper to dispose any {@link PreparableProcessorFactory} via {@link PreparableProcessorFactory#dispose()}
	 * @throws UnhandledException
	 */
	protected void disposeProcessorFactories() throws UnhandledException {
		for (ProcessorFactory<?, ?> processorFactory : processorFactories) {
			if (processorFactory instanceof PreparableProcessorFactory) {
				((PreparableProcessorFactory<?, ?>) processorFactory).dispose();
			}
		}
	}

	/**
	 * Gets the initial null product taking it from the first {@link ProcessorFactory} that was registered. 
	 * @return
	 */
	protected Object getInitialNullProduct() {
		return nullProduct;
	}

	/**
	 * Helper to work around java generics issues.
	 * @param processorFactory
	 * @return
	 */
	protected ProcessorFactory<Object, Object> getAProcessorFactory(ProcessorFactory<?, ?> processorFactory) {
		return (ProcessorFactory<Object, Object>) processorFactory;
	}

	/**
	 * Helper to work around java generics issues.
	 * @param collection
	 * @return
	 */
	protected Collection<Object> getACollection(Collection<?> collection) {
		return (Collection<Object>) collection;
	}

	// //
	//
	// //

	/**
	 * Custom implementation of an {@link Iterator} over a {@link MultiPartProcessor} that will provide the results
	 * of the different steps that the multi part processor can execute before exhausting the product.
	 * 
	 * @author "Javier Cano"
	 *
	 * @param <P>
	 * @param <R>
	 */
	private static class FromMultiPartProcessorIterable<P, R> implements Iterator<R> {
		protected MultiPartProcessor<P, R> multiPartProcessor = null;
		protected R result = null;

		/**
		 * Constructor needs the {@link MultiPartProcessor} and the initial product. <br>
		 * The processor will be initialized in here if it was not initialized.
		 * @param multiPartProcessor
		 * @param product
		 */
		public FromMultiPartProcessorIterable(MultiPartProcessor<P, R> multiPartProcessor, P product) {
			this.multiPartProcessor = multiPartProcessor;
			try {
				if (!multiPartProcessor.isReady()) {
					multiPartProcessor.prepareFor(product);
				}
				MultiPartResult<P, R> multiPartResult = multiPartProcessor.processStep();
				if (multiPartResult != null && multiPartResult.getResult() != null) {
					result = multiPartResult.getResult();
				}
			} catch (UnhandledException ex) {
				throw new RuntimeException("Error initializing iterator for multi part processor.", ex);
			}
		}

		/**
		 * True if another result can provided from the {@link MultiPartProcessor}. 
		 */
		@Override
		public boolean hasNext() {
			return result != null;
		}

		/**
		 * Provides the next result from the {@link MultiPartProcessor}. <br>
		 * Call only if {@link #hasNext()} is <code>true</code>.
		 */
		@Override
		public R next() {
			R resultToReturn = result;
			result = null;
			try {
				if (multiPartProcessor.isReady() && !multiPartProcessor.isDone()) {
					MultiPartResult<P, R> multiPartResult = multiPartProcessor.processStep();
					if (multiPartResult != null && !multiPartResult.isLast()) {
						result = multiPartResult.getResult();
					}
				}
				if (multiPartProcessor.isDone()) {
                    multiPartProcessor.dispose();
				}
			} catch (UnhandledException ex) {
				throw new RuntimeException("Error getting next result from multi part processor.", ex);
			}
			return resultToReturn;
		}

		/**
		 * DO NOT USE.
		 * 
		 * @throws UnsupportedOperationException
		 * @deprecated
		 */
		@Deprecated
		@Override
		public void remove() {
			throw new UnsupportedOperationException("Operation not supported.");
		}
	}

}
