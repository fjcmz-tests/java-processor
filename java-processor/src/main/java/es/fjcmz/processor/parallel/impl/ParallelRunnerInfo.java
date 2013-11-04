package es.fjcmz.processor.parallel.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import es.fjcmz.processor.Processor;
import es.fjcmz.processor.ProcessorFactory;

/**
 * Has base monitoring information about the {@link ParallelRunner} related to
 * it. <br>
 * Is collected by {@link ParallelRunnerStats} as ParallelRunners are created
 * and disposed. <br>
 * 
 * @author "Javier Cano"
 * 
 */
public class ParallelRunnerInfo {

	protected ParallelRunner parallelRunner = null;
	protected List<ProcessorFactory<?, ?>> processorFactories = new ArrayList<>();
	protected Map<ProcessorFactory<?, ?>, ThreadPoolExecutor> mapFactory2ThreadPool = new HashMap<>();
	protected Map<ProcessorFactory<?, ?>, String> mapFactory2LastUpdated = new HashMap<>();
	protected long started = 0;
	protected long ended = 0;

	// //

	/**
	 * Get the {@link ParallelRunner} related to this info.
	 * 
	 * @return
	 */
	public ParallelRunner getParallelRunner() {
		return parallelRunner;
	}

	public void setParallelRunner(ParallelRunner parallelRunner) {
		this.parallelRunner = parallelRunner;
	}

	/**
	 * Get the list of {@link ProcessorFactory}s (in order) used by the
	 * associated {@link ParallelRunner}.
	 * 
	 * @return
	 */
	public List<ProcessorFactory<?, ?>> getProcessorFactories() {
		return processorFactories;
	}

	public void setProcessorFactories(List<ProcessorFactory<?, ?>> processorFactories) {
		this.processorFactories = processorFactories;
	}

	/**
	 * Gets a mapping from each {@link ProcessorFactory} to each
	 * {@link ThreadPoolExecutor} that managed the threads where
	 * {@link Processor}s run for each stage of the related
	 * {@link ParallelRunner}.
	 * 
	 * @return
	 */
	public Map<ProcessorFactory<?, ?>, ThreadPoolExecutor> getMapFactory2ThreadPool() {
		return mapFactory2ThreadPool;
	}

	public void setMapFactory2ThreadPool(Map<ProcessorFactory<?, ?>, ThreadPoolExecutor> mapFactory2ThreadPool) {
		this.mapFactory2ThreadPool = mapFactory2ThreadPool;
	}

	/**
	 * Gets a mapping from {@link ProcessorFactory} to the last time some
	 * activity was seen in that state.
	 * 
	 * @return
	 */
	public Map<ProcessorFactory<?, ?>, String> getMapFactory2LastUpdated() {
		return mapFactory2LastUpdated;
	}

	public void setMapFactory2LastUpdated(Map<ProcessorFactory<?, ?>, String> mapFactory2LastUpdated) {
		this.mapFactory2LastUpdated = mapFactory2LastUpdated;
	}

	/**
	 * Gets the time when the related {@link ParallelRunner} was started.
	 * 
	 * @return
	 */
	public long getStarted() {
		return started;
	}

	public void setStarted(long started) {
		this.started = started;
	}

	/**
	 * Gets the time when the related {@link ParallelRunner} was stopped.
	 * 
	 * @return
	 */
	public long getEnded() {
		return ended;
	}

	public void setEnded(long ended) {
		this.ended = ended;
	}

}
