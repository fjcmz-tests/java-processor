package es.fjcmz.processor.simple.impl;

import java.rmi.UnexpectedException;
import java.util.Collection;

import es.fjcmz.processor.Processor;
import es.fjcmz.processor.ProcessorFactory;
import es.fjcmz.processor.Runner;
import es.fjcmz.processor.UnhandledException;

/**
 * A runner that can chain a processing flow of several {@link ProcessorFactory}
 * s and executes them on a single thread, the calling thread.
 * 
 * @author "Javier Cano"
 * 
 */
public interface SimpleRunner extends Runner {

	/**
	 * Chain method to append a new stage to the processing flow represented by
	 * the {@link Processor}s that the given {@link ProcessorFactory} can
	 * create.
	 * 
	 * @param processorFactory
	 * @return
	 */
	SimpleRunner chain(ProcessorFactory<?, ?> processorFactory);

	/**
     * Execution of the whole processing flow, as built via subsequent calls to
     * the {@link #chain(ProcessorFactory, double)} method, in a sequential way, until
     * all products are consumed.
     * 
     * @param products
     * @throws UnexpectedException
     */
    @Override
    void executeOn(Collection<?> products, Object nullProduct) throws UnhandledException;
	
}
