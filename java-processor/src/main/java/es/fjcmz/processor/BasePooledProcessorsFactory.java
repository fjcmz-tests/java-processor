package es.fjcmz.processor;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A {@link ProcessorFactory} that pools {@link Processor}s to be reused. <br>
 * {@link Processor}s provided by this {@link ProcessorFactory} will be reused
 * but are still subject to the normal flow of a Processor :
 * <ol>
 * <li>{@link Processor#prepare()}</li>
 * <li>{@link Processor#process(Object)}</li>
 * <li>{@link Processor#dispose()}</li>
 * </ol>
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public abstract class BasePooledProcessorsFactory<P, R> implements PreparableProcessorFactory<P, R> {

	protected P nullProduct = null;
	protected R nullResult = null;

	private Deque<Processor<P, R>> processorInstances;

	public BasePooledProcessorsFactory(R nullResult) {
	    this.nullResult = nullResult;
	    this.processorInstances = new ConcurrentLinkedDeque<>();
	}

	/**
	 * Will try to get an idle {@link Processor} from the pool, and if none is
	 * available, a new instance of the {@link Processor} will be created.
	 */
	@Override
	public final Processor<P, R> createProcessor() {
		Processor<P, R> realProcessor = getAProcessor();
		if (realProcessor instanceof MultiPartProcessor) {
			return new PooledProxyMultiPartProcessor<>((MultiPartProcessor<P, R>) realProcessor, this);
		} else {
			return new PooledProxyProcessor<>(realProcessor, this);
		}
	}

	@Override
	public P getNullProduct() {
		return this.nullProduct;
	}

	@Override
	public void setNullProduct(P nullProduct) {
		this.nullProduct = nullProduct;
	}

	@Override
	public R getNullResult() {
		return this.nullResult;
	}
	
    @SuppressWarnings("unchecked")
	@Override
	public Class<P> getProductType() {
		return (Class<P>) getNullProduct().getClass();
	}

    @SuppressWarnings("unchecked")
	@Override
	public Class<R> getResultType() {
		return (Class<R>) getNullResult().getClass();
	}

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Processor<P, R>> getProcessorType() {
        return (Class<? extends Processor<P, R>>) createProcessorInstance().getClass();
    }

	// //
	// Processors Pooling.
	// //

	protected Processor<P, R> getAProcessor() {
		Processor<P, R> processor = getProcessorFromPool();
		if (processor != null) {
			return processor;
		} else {
			return createProcessorInstance();
		}
	}
	
	protected Processor<P, R> getProcessorFromPool() {
		Processor<P, R> processor = processorInstances.pollFirst();
		return processor;
	}

	protected void instanceIdle(Processor<P, R> processor) {
		this.processorInstances.add(processor);
	}

	/**
	 * Subclasses need to implement this to create new instances of the specific
	 * {@link Processor} when the pool is empty. <br>
	 * Both normal {@link Processor}s and {@link MultiPartProcessor}s are
	 * supported.
	 * 
	 * @return
	 */
	protected abstract Processor<P, R> createProcessorInstance();
	
	// //
	
	/**
	 * Clients need to override to apply specific prepare actions.
	 */
	@Override
	public void prepare() throws UnhandledException {
		
	}
	
	/**
	 * Clients need to override to apply specific dispose actions.
	 */
	@Override
	public void dispose() throws UnhandledException {
		
	}

}
