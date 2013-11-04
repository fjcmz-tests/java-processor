package es.fjcmz.processor.parallel.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.fjcmz.processor.simple.impl.SimpleRunner;

/**
 * Singleton class that collects execution details about running
 * {@link ParallelRunner}s. <br>
 * Collects all {@link ParallelRunner}s and their {@link ParallelRunnerInfo} in
 * the {@link #getMapRunner2Info()} and {@link #getMapName2Info()} mappings. <br>
 * At any given time, this only has the in-execution {@link ParallelRunner}s. <br>
 * The monitoring only happens on {@link ParallelRunner}s level, not on
 * {@link SimpleRunner}s.
 * 
 * @author "Javier Cano"
 * 
 */
public final class ParallelRunnerStats {

	private static final Logger LOG = LoggerFactory.getLogger(ParallelRunnerStats.class);

	// //

	/**
	 * A correct way to create a singleton without having to worry about
	 * concurrent instantiation is using an internal private static class as the
	 * holder of the instance. <br>
	 * 
	 * @author "Javier Cano"
	 * 
	 */
	private static class Holder {
		private static final ParallelRunnerStats INSTANCE = new ParallelRunnerStats();
	}

	/**
	 * Get the singleton instance of the {@link ParallelRunnerStats}.
	 * 
	 * @return
	 */
	public static ParallelRunnerStats getInstance() {
		return Holder.INSTANCE;
	}

	private ParallelRunnerStats() {
		// private for singleton
	}

	// //

	private Map<ParallelRunner, ParallelRunnerInfo> mapRunner2Info = null;
	private Map<String, ParallelRunnerInfo> mapName2Info = null;

	/**
	 * Get a mapping from name of {@link ParallelRunner} to its
	 * {@link ParallelRunnerInfo}. <br>
	 * Beware that names are not enforced to be unique and two
	 * {@link ParallelRunner}s with the same name will override one another.
	 * 
	 * @return
	 */
	public Map<String, ParallelRunnerInfo> getMapName2Info() {
		return mapName2Info;
	}

	/**
	 * Gets a mapping from {@link ParallelRunner} to its
	 * {@link ParallelRunnerInfo}. <br>
	 * Only contains in-execution {@link ParallelRunner}s.
	 * 
	 * @return
	 */
	public Map<ParallelRunner, ParallelRunnerInfo> getMapRunner2Info() {
		return mapRunner2Info;
	}

	/**
	 * Called to add a newly created {@link ParallelRunnerInfo} that is related
	 * to a {@link ParallelRunner} that has just been created.
	 * 
	 * @param info
	 */
	public void parallelRunnerStarted(ParallelRunnerInfo info) {
		if (mapRunner2Info == null) {
			mapRunner2Info = new HashMap<>();
		}
		if (mapName2Info == null) {
			mapName2Info = new HashMap<>();
		}
		if (info != null && info.getParallelRunner() != null) {
			info.setStarted(System.currentTimeMillis());
			mapRunner2Info.put(info.getParallelRunner(), info);
			String name = info.getParallelRunner().getName();
			if (mapName2Info.containsKey(name)) {
				LOG.warn("Overriding stats of another ParallelRunner with name {}", name);
			}
			mapName2Info.put(name, info);
			LOG.trace("ParallelRunner {} started.", info.getParallelRunner().getName());
		} else {
			LOG.warn("Given null ParallelRunnerInfo.");
		}
	}

	/**
	 * Called when a {@link ParallelRunner} has finished the execution on the
	 * products it was asked to process.
	 * 
	 * @param info
	 */
	public void parallelRunnerDisposed(ParallelRunnerInfo info) {
		if (mapRunner2Info != null && info != null && info.getParallelRunner() != null) {
			info.setEnded(System.currentTimeMillis());
			mapRunner2Info.remove(info.getParallelRunner());
			LOG.trace("ParallelRunenr {} stopped.", info.getParallelRunner().getName());
		}
	}

	// //

	protected long counter = 0;

	/**
	 * Helper to create a unique name for the given {@link ParallelRunner}.
	 * 
	 * @param runner
	 * @return
	 */
	public String createUniqueName(ParallelRunner runner) {
		Class<?> type = null;
		if (runner != null) {
			type = runner.getClass();
		} else {
			type = ParallelRunner.class;
		}
		String name = type.getSimpleName();
		name += "_" + counter;
		counter++;
		return name;
	}

}
