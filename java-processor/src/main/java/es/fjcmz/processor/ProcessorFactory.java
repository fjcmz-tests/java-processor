package es.fjcmz.processor;


/**
 * A factory that can provide new {@link Processor}s on demand.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public interface ProcessorFactory<P, R> {

	/**
	 * The type of the products accepted by the {@link Processor}s created by
	 * this factory.
	 * 
	 * @return
	 */
	Class<P> getProductType();

	/**
	 * The type of the results produced by the {@link Processor}s created by
	 * this factory.
	 * 
	 * @return
	 */
	Class<R> getResultType();

    /**
     * The type of the {@link Processor} created by this factory.
     * @return
     */
	Class<? extends Processor<P, R>> getProcessorType();

	/**
	 * Creates a new instance of a {@link Processor} that takes products of type
	 * P and produces results of type R.
	 * 
	 * @return
	 */
	Processor<P, R> createProcessor();

	/**
	 * Get the null instance for the products type. <br>
	 * Should only have value when set via {@link #setNullProduct(Object)}.
	 * 
	 * @return
	 */
	P getNullProduct();

	/**
	 * Set the null instance that will mark the end of production from the previous stage in the processing flow.
	 * @param nullProduct
	 */
	void setNullProduct(P nullProduct);
	
	/**
	 * Get the null instance for the results type.
	 * 
	 * @return
	 */
	R getNullResult();

}
