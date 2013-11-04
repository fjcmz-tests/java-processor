package es.fjcmz.processor;


/**
 * Base implementation of a {@link Runner}
 * 
 * @author "Javier Cano"
 *
 */
public abstract class BaseRunnerImpl implements Runner {

    protected String name = null;
    protected RunnerStatus status = RunnerStatus.CREATED;
    
    // //
    //
    // //
    
    public BaseRunnerImpl() {
    }
    
    public BaseRunnerImpl(String name) {
        this.name = name;
    }
    
    // //
    //
    // //
    
    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public RunnerStatus getStatus() {
        return this.status;
    }
    
    public void setStatus(RunnerStatus status) {
        this.status = status;
    }
    
    // //
    
    protected void setEndStatus() {
    	setStatus(RunnerStatus.SUCCESS);
    }

}
