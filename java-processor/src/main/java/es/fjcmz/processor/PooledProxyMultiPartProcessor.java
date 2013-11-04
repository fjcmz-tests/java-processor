package es.fjcmz.processor;


/**
 * Helper class to wrap a {@link MultiPartProcessor} and manage it in a
 * {@link BasePooledProcessorsFactory}. <br>
 * The {@link MultiPartProcessor}s that this class wraps will be reused, but are
 * still subject to the normal flow of a {@link MultiPartProcessor}.
 * <ol>
 * <li>{@link MultiPartProcessor#prepareFor(Object)}</li>
 * <li>{@link MultiPartProcessor#processStep()}</li>
 * <li>{@link MultiPartProcessor#dispose()}</li>
 * </ol>
 * 
 * All operations from the {@link MultiPartProcessor} interface are delegated to
 * the proxied {@link MultiPartProcessor}. <br>
 * On {@link MultiPartProcessor#dispose()} the wrapped instance is returned to
 * the pool of idle processors from the factory.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public class PooledProxyMultiPartProcessor<P, R> implements MultiPartProcessor<P, R> {

	protected String name = null;
	protected MultiPartProcessor<P, R> wrappedProcessor = null;
	protected BasePooledProcessorsFactory<P, R> poolingProcessorFactory = null;

	public PooledProxyMultiPartProcessor(MultiPartProcessor<P, R> wrappedProcessor,
			BasePooledProcessorsFactory<P, R> poolingProcessorFactory) {
		this.name = "PooledProxy_" + wrappedProcessor.getName();
		this.wrappedProcessor = wrappedProcessor;
		this.poolingProcessorFactory = poolingProcessorFactory;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public P getProduct() {
		return wrappedProcessor.getProduct();
	}

	@Override
	public boolean isDone() {
		return wrappedProcessor.isDone();
	}

	@Override
	public boolean isReady() {
		return wrappedProcessor.isReady();
	}

	@Override
	public void prepareFor(P product) throws UnhandledException {
		wrappedProcessor.prepareFor(product);
	}

	@Override
	public MultiPartResult<P, R> processStep() throws UnhandledException {
		return wrappedProcessor.processStep();
	}

	@Override
	public void prepare() throws UnhandledException {
		wrappedProcessor.prepare();
	}

	@Override
	public void dispose() throws UnhandledException {
		wrappedProcessor.dispose();
		reset();
		poolingProcessorFactory.instanceIdle(wrappedProcessor);
	}
	
	@Override
	public void reset() {
		wrappedProcessor.reset();		
	}

	@Override
	public ProcessorResult<P, R> process(P product) throws UnhandledException {
		return wrappedProcessor.process(product);
	}

}
