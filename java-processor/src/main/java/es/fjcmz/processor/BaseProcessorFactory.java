package es.fjcmz.processor;


/**
 * Base class for {@link ProcessorFactory}s that provides common boilerplate
 * code.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public abstract class BaseProcessorFactory<P, R> implements PreparableProcessorFactory<P, R> {

	protected P nullProduct = null;
	protected R nullResult = null;

	public BaseProcessorFactory(R nullResult) {
        this.nullResult = nullResult;
    }
	
	/**
	 * Must not have an initial value except when set by
	 * {@link #setNullProduct(Object)}.
	 */
	@Override
	public P getNullProduct() {
		return nullProduct;
	}

	@Override
	public void setNullProduct(P nullProduct) {
		this.nullProduct = nullProduct;
	}

	/**
	 * The null instance that represents the end of production from the
	 * {@link Processor}s this factory provides.
	 */
	@Override
	public R getNullResult() {
		return nullResult;
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
        return (Class<? extends Processor<P, R>>) doCreateProcessor().getClass();
    }

    // //

	@Override
	public final Processor<P, R> createProcessor() {
	    Processor<P, R> processor = doCreateProcessor();
	    return processor;
	}
	
	/**
	 * Subclasses must implement any prepare actions here.
	 */
	@Override
	public void prepare() throws UnhandledException {

	}

	/**
	 * Subclasses must implement any dispose actions here.
	 */
	@Override
	public void dispose() throws UnhandledException {

	}
	
	protected abstract Processor<P, R> doCreateProcessor();

}
