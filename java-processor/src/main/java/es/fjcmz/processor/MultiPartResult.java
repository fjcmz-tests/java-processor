package es.fjcmz.processor;

/**
 * A {@link ProcessorResult} from a {@link MultiPartProcessor}. <br>
 * In addition to the product and result it also provides information about the
 * {@link MultiPartProcessor} that produced it and whether this is the last
 * result.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public class MultiPartResult<P, R> extends ProcessorResult<P, R> {

	protected MultiPartProcessor<P, R> processor = null;
	protected boolean last = false;

	// //

	/**
	 * Constructor with product, result, processor and flag for last result.
	 * 
	 * @param product
	 * @param result
	 * @param processor
	 * @param last
	 */
	public MultiPartResult(P product, R result, MultiPartProcessor<P, R> processor, boolean last) {
		super(product, result);
		this.processor = processor;
		this.last = last;
	}

	// //

	/**
	 * The {@link MultiPartProcessor} that produced this result.
	 * 
	 * @return
	 */
	public MultiPartProcessor<P, R> getProcessor() {
		return processor;
	}

	public void setProcessor(MultiPartProcessor<P, R> processor) {
		this.processor = processor;
	}

	/**
	 * True if this is the last result that the {@link MultiPartProcessor} can
	 * produce.
	 * 
	 * @return
	 */
	public boolean isLast() {
		return last;
	}

	public void setLast(boolean last) {
		this.last = last;
	}

	// //

	public static final MultiPartResult<?, ?> NULL_MULTIPART_RESULT = new MultiPartResult<>(null, null, null, false);

}
