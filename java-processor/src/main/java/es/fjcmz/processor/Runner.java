package es.fjcmz.processor;

import java.util.Collection;

/**
 * Allows coordinating the running of processing flows with several steps connected with the actual processing
 * happening in {@link Processor}s from the chained {@link ProcessorFactory}s. <br>
 * 
 * @author "Javier Cano"
 *
 */
public interface Runner {

    /**
     * Execution of the whole processing flow until all products are consumed. <br>
     * 
     * @param products
     * @throws UnexpectedException
     */
    void executeOn(Collection<?> products, Object nullProduct) throws UnhandledException;

    // //
    
    /**
     * Gets the name of this {@link Runner} instance.
     * 
     * @return
     */
    String getName();
    
    /**
     * Gets the {@link RunnerStatus} of this {@link Runner}, according to the stage of processing that is in. <br>
     * 
     * @return
     */
    RunnerStatus getStatus();
    
}
