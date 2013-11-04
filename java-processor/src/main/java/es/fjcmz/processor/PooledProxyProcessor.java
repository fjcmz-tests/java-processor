package es.fjcmz.processor;


/**
 * Helper class to wrap a {@link Processor} and manage it in a
 * {@link BasePooledProcessorsFactory}. <br>
 * The {@link Processor}s that this class wraps will be reused, but are still
 * subject to the normal flow of a {@link Processor}.
 * <ol>
 * <li>{@link Processor#prepare()}</li>
 * <li>{@link Processor#process(Object)}</li>
 * <li>{@link Processor#dispose()}</li>
 * </ol>
 * All operations from the {@link Processor} interface are delegated to the
 * proxied {@link Processor}. <br>
 * On {@link Processor#dispose()} the wrapped instance is returned to the pool
 * of idle processors from the factory.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public class PooledProxyProcessor<P, R> implements Processor<P, R> {

	protected String name = null;
	protected Processor<P, R> wrappedProcessor = null;
	protected BasePooledProcessorsFactory<P, R> poolingProcessorFactory = null;

	public PooledProxyProcessor(Processor<P, R> wrappedProcessor,
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
	public ProcessorResult<P, R> process(P product) throws UnhandledException {
		return wrappedProcessor.process(product);
	}

	@Override
	public void prepare() throws UnhandledException {
		wrappedProcessor.prepare();
	}

	@Override
	public void dispose() throws UnhandledException {
		wrappedProcessor.dispose();
		poolingProcessorFactory.instanceIdle(wrappedProcessor);
	}

}
