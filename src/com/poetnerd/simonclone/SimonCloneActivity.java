package com.poetnerd.simonclone;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;

import android.os.Bundle;

import android.util.Log;

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
import android.widget.Toast;

import android.content.DialogInterface;

import android.media.AudioManager;

public class SimonCloneActivity extends Activity {
	@SuppressWarnings("unused")
	private static final String TAG = SimonCloneActivity.class.getSimpleName();
	
	private static final int LEVEL_DIALOG = 1;
	private static final int GAME_DIALOG = 2;
	private static final int ABOUT_DIALOG = 3;
	
	private SimonClone model;
	private Menu mMenu;
	private AlertDialog levelDialog;
	private AlertDialog gameDialog;
	private AlertDialog aboutDialog;
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
        
        // After all initialization, we set up our save/restore InstanceState Bundle.
        if (savedInstanceState == null) {		// Just launched.  Set initial state.
        	Log.d(TAG, "Initializing");
        	SharedPreferences settings = getPreferences (0); // Private mode by default.
        	model.setLevel(settings.getInt(SimonClone.KEY_GAME_LEVEL, 1));	// Game Level
        	model.setGame(settings.getInt(SimonClone.KEY_THE_GAME, 1)); 	// The Game
        	model.setLongest(settings.getString(SimonClone.KEY_LONGEST_SEQUENCE, "")); 	// String Rep of Longest
        	levelDisplay.setText(String.valueOf(model.getLevel()));
        	gameDisplay.setText(String.valueOf(model.getGame()));
        } else {
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
        case R.id.clear_longest:
        	model.setLongest("");
        }
        return false;
    }

}