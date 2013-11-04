package es.fjcmz.processor;

import java.rmi.UnexpectedException;

/**
 * A {@link ProcessorFactory} with {@link #prepare()} and {@link #dispose()}
 * methods for better factory initializaton and disposal.
 * 
 * @author "Javier Cano"
 * 
 * @param <P>
 * @param <R>
 */
public interface PreparableProcessorFactory<P, R> extends ProcessorFactory<P, R> {

	/**
	 * Called before any execution takes place and before the
	 * {@link #createProcessor()} is called.
	 * 
	 * @throws UnexpectedException
	 */
	void prepare() throws UnhandledException;

	/**
	 * Called after all execution has taken place. {@link #createProcessor()}
	 * will not be called again after this call.
	 * 
	 * @throws UnexpectedException
	 */
	void dispose() throws UnhandledException;

}
