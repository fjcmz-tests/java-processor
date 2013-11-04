package es.fjcmz.processor;

/**
 * Possible staus of a {@link Runner}. <br>
 * 
 * @author "Javier Cano"
 *
 */
public enum RunnerStatus {

    /**
     * Just created, not prepared neither executed.
     */
    CREATED,
    /**
     * Prepared, but not executed.
     */
    PREPARED,
    /**
     * In execution of all the products via all the {@link Processor}s.
     */
    RUNNING,
    /**
     * Execution paused, waiting to be resumed.
     */
    PAUSED,
    /**
     * Execution finished with no errors.
     */
    SUCCESS,
    /**
     * Execution finished abruptly because of stop request.
     */
    ABORTED,
    /**
     * Execution finished abruptly because of an error.
     */
    ERROR
    
}
