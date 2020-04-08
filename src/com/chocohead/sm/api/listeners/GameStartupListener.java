package com.chocohead.sm.api.listeners;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;

/**
 * Listener for performing tasks as the game first starts up.
 *
 * @see ModInitializer Fabric's equivalent
 *
 * @author Chocohead
 * @implNote
 * 	Technically {@link ClientModInitializer} and {@link DedicatedServerModInitializer} are closer equivalents,
 * 	with both being used if the client <i>and</i> server are listening
 */
public interface GameStartupListener {
	/** Called once when the game is starting up */
	void onGameStart();

	/**
	 * Asynchronous version of {@link GameStartupListener} which will run off the main game thread
	 *
	 * @author Chocohead
	 */
	interface GameStartupAsyncListener extends AsyncListener {
		/**
		 * {@inheritDoc}
		 *
		 * @apiNote
		 * 	Run from a different thread to the game so shouldn't interact with anything non-thread safe.
		 * 	<br>
		 * 	Tasks which need to be on the main thread can be scheduled via {@link #scheduleTask(Runnable)}
		 */
		void onGameStart(Synchroniser syncer);
	}
}