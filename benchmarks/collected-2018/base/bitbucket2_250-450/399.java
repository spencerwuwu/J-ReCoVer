// https://searchcode.com/api/result/54317656/

/**
 * 
 */
package org.javarosa.formmanager.api;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

import org.javarosa.core.api.State;
import org.javarosa.core.data.IDataPointer;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.UnavailableServiceException;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.formmanager.api.transitions.FormEntryTransitions;
import org.javarosa.formmanager.properties.FormManagerProperties;
import org.javarosa.formmanager.view.IFormEntryView;
import org.javarosa.j2me.services.DataCaptureServiceRegistry;
import org.javarosa.j2me.services.LocationCaptureService;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.utilities.media.MediaUtils;

/**
 * Extension of {@link FormEntryController} for J2ME.
 * 
 * @author ctsims
 *
 */
public class JrFormEntryController extends FormEntryController implements FormMultimediaController {
	
	FormEntryTransitions transitions;
	IFormEntryView view;
	boolean quickEntry = true;
	boolean isMinimal = false;
	
	private static Reference curAudRef = null;
	private static String curAudioURI;
	
	protected static boolean playAudioIfAvailable = true;
	
	private static int POUND_KEYCODE = Canvas.KEY_POUND;
	
	
	/** Causes audio player to throw runtime exceptions if there are problems instead of failing silently **/
	private boolean audioFailFast = true;
	
	String extraKeyMode;
	
	public JrFormEntryController(JrFormEntryModel model) {
		this(model, FormManagerProperties.EXTRA_KEY_LANGUAGE_CYCLE, false, true);
	}
	
	public JrFormEntryController(JrFormEntryModel model, String extraKeyMode, boolean audioFailFast, boolean quickEntry){
		this(model, extraKeyMode, audioFailFast, quickEntry, false);
	}
	
	public JrFormEntryController(JrFormEntryModel model, String extraKeyMode, boolean audioFailFast, boolean quickEntry, boolean isMinimal) {
		super(model);
		tryToInitDefaultLanguage(model);
		this.extraKeyMode = extraKeyMode;
		this.audioFailFast = audioFailFast;
		this.quickEntry = quickEntry;
		this.isMinimal = isMinimal;
		
    	//#if device.identifier == Sony-Ericsson/K610i
    	POUND_KEYCODE = Canvas.KEY_STAR;
    	//#endif
	}

	private void tryToInitDefaultLanguage(JrFormEntryModel model) {
		//Try to set the current form locale based on the current app locale
		String[] languages = model.getLanguages();
		if(languages != null) {
			String locale = Localization.getGlobalLocalizerAdvanced().getLocale();
			if(locale != null) {
				for(String language : languages) {
					if(locale.equals(language)) {
						model.getForm().getLocalizer().setLocale(locale);
						break;
					}
				}
			}
		}
	}

	public JrFormEntryModel getModel () {
		return (JrFormEntryModel)super.getModel();
	}
	
	public void setView(IFormEntryView view) {
		this.view = view;
		
		view.attachFormMediaController(this);
	}
	public IFormEntryView getView(){
		return this.view;
	}
	public void setTransitions(FormEntryTransitions transitions) {
		this.transitions = transitions;
	}
	
	/**
	 * Handles the given key event, and returns a flag signifying whether 
	 * the interface should continue trying to handle the event.
	 * 
	 * @param key The key that was pressed
	 * @return true if the ui should stop handling this event. False if it
	 * is ok to continue processing it.
	 */
	public boolean handleKeyEvent(int key) {
		if(getModel().getForm().getLocalizer() != null && key == POUND_KEYCODE && !FormManagerProperties.EXTRA_KEY_AUDIO_PLAYBACK.equals(getExtraKeyMode())) {
    		cycleLanguage();
    		return true;
    	} else if(FormManagerProperties.EXTRA_KEY_AUDIO_PLAYBACK.equals(getExtraKeyMode()) && key == POUND_KEYCODE){
    		
    		//For now, we'll assume that video playback basically trumps audio playback. 
    		//TODO: Add a way to play videos when extra-key isn't set to audio
    		if(player != null) {
    			try {
	    			if(player.getState() == Player.STARTED) {
	    				player.stop();
	    			} else {
	    				player.start();
	    			}
				} catch (MediaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			return true;
    		}
    		
    		if(this.getModel().getEvent() != FormEntryController.EVENT_QUESTION) {return false;}
    		//Get prompt
    		FormEntryPrompt fep = this.getModel().getQuestionPrompt();
    		
    		try{
    			if(fep != null && fep.getAudioText() != null) {
    				// log that audio file was (attempted to be) played
    				// TODO: move this to some sort of 'form entry diagnostics' framework
    				// instead of bloating the logs
    				String audio = fep.getAudioText();
    				
    				//extract just the audio filename to reduce log size
    				String audioShort;
    				try {
    					Vector<String> pieces = DateUtils.split(audio, "/", false);
    					String filename = pieces.lastElement();
    					int suffixIx = filename.lastIndexOf('.');
    					audioShort = (suffixIx != -1 ? filename.substring(0, suffixIx) : filename);
    				} catch (Exception e) {
    					audioShort = audio;
    				}	    				
    				Logger.log("audio", audioShort);
    			}
    		} catch(Exception e) {
    			//Nothing
    		}
    		
    		playAudioOnDemand(fep);
    		//We can keep processing this. Audio plays in the background.
    		return false;
    	}
		return false;
	}
	
	public void start() {
		view.show();
	}
	
	/**
	 * Start from a specific index
	 * @param index
	 */
	public void start(FormIndex index){
		view.show(index);
	}
	
	public void abort() {
		view.destroy();
		transitions.abort();
	}
	
	public void saveAndExit(boolean formComplete) {
		if (formComplete){
			this.getModel().getForm().postProcessInstance();
		}
		view.destroy();
		transitions.formEntrySaved(this.getModel().getForm(),this.getModel().getForm().getInstance(),formComplete);
	}
	
	public void suspendActivity(int mediaType) throws UnavailableServiceException {
		view.destroy();
		transitions.suspendForMediaCapture(mediaType);
	}
	
	public void cycleLanguage () {
		setLanguage(getModel().getForm().getLocalizer().getNextLocale());
	}
	
	public String getExtraKeyMode() {
		return extraKeyMode;
	}
	
	
	
	//// New Audio Stuff follows below. I've tried to set it up so that we can split this out into a seperate "view" 
	//// if you will at a later point.
	/**
	 * Checks the boolean playAudioIfAvailable first.
	 * Plays the question audio text
	 */
	public void playAudioOnLoad(FormEntryPrompt fep){
		//If the current session is expecting audio playback w/the extrakey, don't 
		//play it passively, wait for the button to be pressed.
		if(!FormManagerProperties.EXTRA_KEY_AUDIO_PLAYBACK.equals(extraKeyMode)) {
			playAudio(fep,null);
		}
	}
	
	/**
	 * Checks the boolean playAudioIfAvailable first.
	 * Plays the question audio text
	 */
	public void playAudioOnDemand(FormEntryPrompt fep){
		playAudio(fep,null);
	}
	
	public int playAudioOnDemand(FormEntryPrompt fep,SelectChoice select) {
		return playAudio(fep, select);
	}
	
    /**
     * Plays audio for the SelectChoice (if AudioURI is present and media is available)
     * @param fep
     * @param select
     * @return
     */
	public int playAudio(FormEntryPrompt fep,SelectChoice select){
		if (!playAudioIfAvailable) return MediaUtils.AUDIO_DISABLED;
		
		String textID;
		curAudioURI = null;
		String tag = null;
		if (select == null) {
			if (fep.getAudioText() != null) {
				curAudioURI = fep.getAudioText();
				tag = "#";
			} else {
				return MediaUtils.AUDIO_NO_RESOURCE;
			}	
		}else{
			textID = select.getTextID();
			if(textID == null || textID == "") return MediaUtils.AUDIO_NO_RESOURCE;
			
			if (fep.getSpecialFormSelectChoiceText(select, FormEntryCaption.TEXT_FORM_AUDIO) != null) {
				curAudioURI = fep.getSpecialFormSelectChoiceText(select, FormEntryCaption.TEXT_FORM_AUDIO);
				tag = String.valueOf(select.getIndex());
			} else {
				return MediaUtils.AUDIO_NO_RESOURCE;
			}
		}
		
		//No idea why this is a member variable...
		return MediaUtils.playOrPauseAudio(curAudioURI, tag);
	}
	

	public boolean isEntryOptimized() {
		return quickEntry;
	}
	
	//A video player being controlled by this controller
	Player player;

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.formmanager.api.FormMultimediaController#attachVideoPlayer(javax.microedition.media.Player)
	 */
	public void attachVideoPlayer(Player player) {
		//NOTE: Not thread safe
		if(this.player != null) {
			detachVideoPlayer(player);
		}
		this.player = player;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.formmanager.api.FormMultimediaController#detachVideoPlayer(javax.microedition.media.Player)
	 */
	public void detachVideoPlayer(Player player) {
		if(this.player == player) {
			this.player = null;
		}
	}

	public void stopAudio() {
		MediaUtils.stopAudio();
	}
	
	public void setMinimal(boolean isMinimal){
		this.isMinimal = isMinimal;
	}
	
	public boolean isMinimal(){
		return this.isMinimal;
	}

}

