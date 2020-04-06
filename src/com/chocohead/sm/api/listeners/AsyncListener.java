package com.chocohead.sm.api.listeners;

/**
 * Applied to any listener which is run off the main thread to allow syncing with it
 *
 * @author Chocohead
 */
public interface AsyncListener {
	/**
	 * Object passed to asynchronous listeners to allow them to synchronise back with the main thread
	 *
	 * @author Chocohead
	 */
	interface Synchroniser {
		/**
		 * Schedule the given runnable to run from the context of the main thread
		 *
		 * @param task The task to perform on the main thread
		 */
		void scheduleTask(Runnable task);
	}
}