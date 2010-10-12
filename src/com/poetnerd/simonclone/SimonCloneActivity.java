package com.poetnerd.simonclone;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface;

public class SimonCloneActivity extends Activity {
	@SuppressWarnings("unused")
	private static final String TAG = SimonCloneActivity.class.getSimpleName();
	
	private static final int LEVEL_DIALOG = 1;
	private static final int GAME_DIALOG = 2;
	
	private SimonClone model;
	private Menu mMenu;
	private AlertDialog levelDialog;
	private AlertDialog gameDialog;
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

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)  {
    	mMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	AlertDialog.Builder builder;
    	switch (id) {
    	case LEVEL_DIALOG:
    		builder = new AlertDialog.Builder(this);
    		builder.setTitle(R.string.set_level);
            builder.setSingleChoiceItems(R.array.level_choices, 0, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	model.setLevel(whichButton);
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
            builder.setSingleChoiceItems(R.array.game_choices, 0, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	model.setGame(whichButton);
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
        }
        return false;
    }

}