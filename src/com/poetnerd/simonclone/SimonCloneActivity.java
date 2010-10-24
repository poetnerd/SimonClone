/*
 * By William D. Cattey
 * 
 * This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ 
 * or send a letter to Creative Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
 * 
 * Summary of terms: you may use this code for non-commercial purposes, and you may make changes, but you must 
 * attribute the source, and you must share the source under these same terms.
 */

package com.poetnerd.simonclone;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;

import android.os.Bundle;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.TextView;

import android.content.DialogInterface;

import android.media.AudioManager;

public class SimonCloneActivity extends Activity {
	@SuppressWarnings("unused")
	private static final String TAG = SimonCloneActivity.class.getSimpleName();
	
	private static final int LEVEL_DIALOG = 1;
	private static final int GAME_DIALOG = 2;
	private static final int ABOUT_DIALOG = 3;
	private static final int HELP_DIALOG = 4;
	
	private SimonClone model;
	private Menu mMenu;
	private AlertDialog levelDialog;
	private AlertDialog gameDialog;
	private AlertDialog aboutDialog;
	private AlertDialog helpDialog;
	private TextView levelDisplay;
	private TextView gameDisplay;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        model = new SimonClone(this);
                
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ButtonGridView grid = (ButtonGridView) this.findViewById(R.id.button_grid);
        grid.setSimonCloneModel(model);
        
        gameDisplay = (TextView)findViewById(R.id.game);
        
        levelDisplay = (TextView)findViewById(R.id.level);        

        Button lastButton = (Button)findViewById(R.id.last);
        lastButton.setOnClickListener(new OnClickListener() {        	
        	public void onClick(View v) {
        		model.playLast();
        	}
        });

        Button longestButton = (Button)findViewById(R.id.longest);
        longestButton.setOnClickListener(new OnClickListener() {        	
        	public void onClick(View v) {
        		model.playLongest();
        	}
        });
        
        Button startButton = (Button)findViewById(R.id.start);
        startButton.setOnClickListener(new OnClickListener() {        	
        	public void onClick(View v) {
        		model.gameStart();
        	}
        });
        
        /* Change the default vol control of app to what is SHOULD be. */
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);  
        
        /* After all initialization, we set up our save/restore InstanceState Bundle. */
        if (savedInstanceState == null) {		// Just launched.  Set initial state.
        	SharedPreferences settings = getPreferences (0); // Private mode by default.
        	model.setLevel(settings.getInt(SimonClone.KEY_GAME_LEVEL, 1));	// Game Level
        	model.setGame(settings.getInt(SimonClone.KEY_THE_GAME, 1)); 	// The Game
        	model.setLongest(settings.getString(SimonClone.KEY_LONGEST_SEQUENCE, "")); 	// String Rep of Longest
        	levelDisplay.setText(String.valueOf(model.getLevel()));
        	gameDisplay.setText(String.valueOf(model.getGame()));
        } else {
        	/* If I understand the activity cycle, I can put this here and not override
        	 * onRestoreInstanceState */
        	model.restoreState(savedInstanceState);
        }
    }
        
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	model.saveState(outState);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)  {
    	mMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    protected void onPause () {
    	super.onPause();
    	SharedPreferences settings = getPreferences (0); // Private mode by default.
    	SharedPreferences.Editor editor = settings.edit();
    	
    	editor.putInt(SimonClone.KEY_GAME_LEVEL, model.getLevel());	// Game Level
    	editor.putInt(SimonClone.KEY_THE_GAME, model.getGame());	// The Game
    	editor.putString(SimonClone.KEY_LONGEST_SEQUENCE, model.getLongest());	// Longest match

    	editor.commit();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	AlertDialog.Builder builder;
    	switch (id) {
    	case LEVEL_DIALOG:
    		builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.set_level);
            builder.setSingleChoiceItems(R.array.level_choices, model.getLevel() - 1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	model.setLevel(whichButton + 1);
                	levelDisplay.setText(String.valueOf(whichButton + 1));
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	levelDialog.dismiss();
                }
            });
            levelDialog = builder.create();
            return levelDialog;
    	case GAME_DIALOG:
    		builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.set_game);
            builder.setSingleChoiceItems(R.array.game_choices, model.getGame() - 1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	model.setGame(whichButton + 1);
                	gameDisplay.setText(String.valueOf(whichButton + 1));
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	gameDialog.dismiss();
                }
            });
            gameDialog = builder.create();
            return gameDialog;
    	case ABOUT_DIALOG:
    		builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.about);
    		builder.setMessage(R.string.long_about);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	aboutDialog.dismiss();
                }
            });
            aboutDialog = builder.create();
            return aboutDialog;
    	case HELP_DIALOG:
    		builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.help);
    		builder.setMessage(R.string.long_help);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	helpDialog.dismiss();
                }
            });
            helpDialog = builder.create();
            return helpDialog;
    	default: return null;
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.set_level:
        	showDialog(LEVEL_DIALOG);
        	return true;
        case R.id.set_game:
        	showDialog(GAME_DIALOG);
        	return true;
        case R.id.about:
        	showDialog(ABOUT_DIALOG);
        	return true;
        case R.id.help:
        	showDialog(HELP_DIALOG);
        	return true;
        case R.id.clear_longest:
        	model.setLongest("");
        	return true;
        }
        return false;
    }

}