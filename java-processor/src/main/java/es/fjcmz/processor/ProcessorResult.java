package es.fjcmz.processor;


/**
 * Encapsulation of the result from a {@link Processor} with the product that
 * produced it.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public class ProcessorResult<P, R> {

	protected P product = null;
	protected R result = null;

	/**
	 * Use {@link #ProcessorResult(Object, Object)} <br>
	 * 
	 * @deprecated
	 */
	@Deprecated
	public ProcessorResult() {
	}

	/**
	 * Constructor with the product and the result.
	 * @param product
	 * @param result
	 */
	public ProcessorResult(P product, R result) {
		this.product = product;
		this.result = result;
	}

	/**
	 * The product that produced this result.
	 * 
	 * @return
	 */
	public P getProduct() {
		return product;
	}

	public void setProduct(P product) {
		this.product = product;
	}

	/**
	 * The result value.
	 * 
	 * @return
	 */
	public R getResult() {
		return result;
	}

	public void setResult(R result) {
		this.result = result;
	}

	// //

	public static final ProcessorResult<?, ?> NULL_PROCESSOR_RESULT = new ProcessorResult<>(null, null);

}
