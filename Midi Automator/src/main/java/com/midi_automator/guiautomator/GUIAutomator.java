package com.midi_automator.guiautomator;

import org.apache.log4j.Logger;
import org.sikuli.basics.Settings;
import org.sikuli.script.Match;
import org.sikuli.script.ObserveEvent;
import org.sikuli.script.ObserverCallBack;
import org.sikuli.script.Region;

import com.midi_automator.model.MidiAutomatorProperties;
import com.midi_automator.presenter.IDeActivateable;
import com.midi_automator.utils.SystemUtils;

/**
 * 
 * @author aguelle
 * 
 *         This class automates GUI interactions.
 */
public class GUIAutomator extends Thread implements IDeActivateable {

	static Logger log = Logger.getLogger(GUIAutomator.class.getName());

	private final float MOVE_MOUSE_DELAY = 0;
	private final boolean CHECK_LAST_SEEN = true;
	private final double SIKULIX_TIMEOUT = 10;

	private volatile boolean running = true;
	private boolean active = true;

	private GUIAutomation guiAutomation;
	private Region searchRegion = new MinSimColoredScreen();
	private Match match;
	private ObserverCallBack observer = new AppearingMatchObserver();
	private boolean fixedSearchRegion = false;

	/**
	 * Constructor
	 * 
	 * @param minSimilarity
	 *            The minimum similarity for the automation
	 */
	public GUIAutomator(float minSimilarity) {
		Settings.MinSimilarity = minSimilarity;
		Settings.CheckLastSeenSimilar = minSimilarity;
		Settings.MoveMouseDelay = MOVE_MOUSE_DELAY;
		Settings.CheckLastSeen = CHECK_LAST_SEEN;
	}

	/**
	 * Constructor
	 * 
	 * @param guiAutomation
	 *            The gui automation to run
	 * @param minSimilarity
	 *            The minimum similarity for the automation
	 */
	public GUIAutomator(GUIAutomation guiAutomation, float minSimilarity) {
		this(minSimilarity);
		setGUIAutomation(guiAutomation);
	}

	@Override
	public void run() {

		if (guiAutomation.getTimeout() > 0) {
			new TimeOutWatcher().start();
		}

		// prepare search parameters

		searchRegion.setObserveScanRate(guiAutomation.getScanRate());
		searchRegion.onAppear(SystemUtils.replaceSystemVariables(guiAutomation
				.getImagePath()), observer);

		while (running) {
			try {
				Thread.sleep(100);
				if (isActive()) {
					triggerAutomation();
				}
			} catch (InterruptedException e) {
				log.error("Failure in GUIAutomator Thread sleep", e);
			}
		}
	}

	/**
	 * Terminate the automator thread
	 */
	public void terminate() {
		if (searchRegion != null) {
			searchRegion.stopObserver();
		}
		running = false;
	}

	/**
	 * Sets the GUI automation that shall be run.
	 * 
	 * @param guiAutomation
	 *            A GUI automation
	 */
	public void setGUIAutomation(GUIAutomation guiAutomation) {
		this.guiAutomation = guiAutomation;

		log.info(("(" + getName() + "): Activated Automation: " + guiAutomation
				.toString()));
	}

	/**
	 * Runs a configured GUI automation.
	 */
	private void triggerAutomation() {

		// always
		if (guiAutomation.getTrigger()
				.equals(GUIAutomation.CLICKTRIGGER_ALWAYS)) {
			if (guiAutomation.isActive()) {
				searchAndRunAutomation();
			}
		}

		// once
		if (guiAutomation.getTrigger().equals(GUIAutomation.CLICKTRIGGER_ONCE)) {
			if (guiAutomation.isActive()) {
				if (searchAndRunAutomation()) {
					guiAutomation.setActive(false);
				}
			}
		}

		// on change
		if (guiAutomation.getTrigger().equals(
				GUIAutomation.CLICKTRIGGER_ONCE_PER_CHANGE)) {
			if (guiAutomation.isActive()) {
				if (searchAndRunAutomation()) {
					guiAutomation.setActive(false);
				}
			}
		}

		// on midi
		if (guiAutomation.getTrigger()
				.contains(GUIAutomation.CLICKTRIGGER_MIDI)) {
			if (guiAutomation.isActive()) {
				if (searchAndRunAutomation()) {
					guiAutomation.setActive(false);
				}
			}
		}
	}

	/**
	 * Activates all automations that shall be run only once per change.
	 */
	public void activateOncePerChangeAutomations() {

		if (guiAutomation.getTrigger().equals(
				GUIAutomation.CLICKTRIGGER_ONCE_PER_CHANGE)) {
			log.info("Activate automation once per change:" + guiAutomation);
			guiAutomation.setActive(true);
		}
	}

	/**
	 * Activates all automations that shall be run by midi message.
	 * 
	 * @param midiSignature
	 *            The midi signature that shall invoke the automation
	 */
	public void activateMidiAutomations(String midiSignature) {

		String trigger = guiAutomation.getTrigger();
		String signature = guiAutomation.getMidiSignature();

		if (signature != null && midiSignature != null) {
			if ((trigger.contains(GUIAutomation.CLICKTRIGGER_MIDI) && (signature
					.equals(midiSignature)))) {
				log.info("Activated automation " + guiAutomation
						+ " by MIDI message: " + midiSignature);
				guiAutomation.setActive(true);
			}
		}
	}

	/**
	 * Searches for the region and runs the desired action if the automations
	 * are active.
	 * 
	 * @return <TRUE> if the automation was run, <FALSE> if not found or
	 *         automation not active
	 */
	private boolean searchAndRunAutomation() {

		boolean wasRun = false;
		String imagePath = guiAutomation.getImagePath();

		if (!imagePath.equals(MidiAutomatorProperties.VALUE_NULL)) {

			Region lastFound = guiAutomation.getLastFoundRegion();

			// reduce search region
			if (lastFound != null && !guiAutomation.isMovable()
					&& !fixedSearchRegion) {

				searchRegion.x = lastFound.x;
				searchRegion.y = lastFound.y;
				searchRegion.w = lastFound.w;
				searchRegion.h = lastFound.h;

				fixedSearchRegion = true;
			}

			log.debug("("
					+ getName()
					+ "): Search for match of \""
					+ SystemUtils.replaceSystemVariables(guiAutomation
							.getImagePath()) + "\" "
					+ ", minimum smimilarity: " + Settings.MinSimilarity
					+ ", scan rate: " + searchRegion.getObserveScanRate());

			boolean found = searchRegion.observe(SIKULIX_TIMEOUT);

			if (found) {
				// do not run automation if it was deactivated in the meantime
				if (isActive()) {
					runAutomation(match);
					wasRun = true;
				}
			} else {
				log.info("("
						+ getName()
						+ "): Could not find match on screen for \""
						+ SystemUtils.replaceSystemVariables(guiAutomation
								.getImagePath()) + "\"");
			}
		}

		return wasRun;
	}

	/**
	 * Runs automation the automation action on the found region.
	 * 
	 * @param match
	 *            The found region.
	 */
	private void runAutomation(Match match) {
		guiAutomation.setLastFoundRegion(match);

		try {
			Thread.sleep(guiAutomation.getMinDelay());
		} catch (InterruptedException e) {
			log.error("Delay before GUI automation run failed.", e);
		}

		// left click
		if (guiAutomation.getType().equals(GUIAutomation.CLICKTYPE_LEFT)) {
			match.click();
		}

		// right click
		if (guiAutomation.getType().equals(GUIAutomation.CLICKTYPE_RIGHT)) {
			match.rightClick();
		}

		// double click
		if (guiAutomation.getType().equals(GUIAutomation.CLICKTYPE_DOUBLE)) {
			match.doubleClick();
		}
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean active) {
		this.active = active;
	}

	public GUIAutomation getGuiAutomation() {
		return guiAutomation;
	}

	/**
	 * The observer call back stores the found match on appear.
	 * 
	 * @author aguelle
	 *
	 */
	class AppearingMatchObserver extends ObserverCallBack {

		@Override
		public void appeared(ObserveEvent e) {

			match = e.getMatch();

			log.info("("
					+ getName()
					+ "): Found match on screen for \""
					+ SystemUtils.replaceSystemVariables(guiAutomation
							.getImagePath()) + "\"");
		}
	}

	/**
	 * Takes the running time and stops the automation if the timeout exceeded.
	 * 
	 * @author aguelle
	 *
	 */
	class TimeOutWatcher extends Thread {

		private long startingTime;
		private boolean running = true;

		@Override
		public void run() {

			startingTime = System.currentTimeMillis();

			while (running) {
				long runningTime = System.currentTimeMillis() - startingTime;

				if (runningTime > guiAutomation.getTimeout()) {

					running = false;

					log.info("(" + getName()
							+ "): Automation timed out after: " + runningTime);

					terminate();
				}
			}
		}
	}
}
