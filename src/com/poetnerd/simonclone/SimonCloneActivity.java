package com.poetnerd.simonclone;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SimonCloneActivity extends Activity {
	@SuppressWarnings("unused")
	private static final String TAG = SimonCloneActivity.class.getSimpleName();
	
	private SimonClone model;
	

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        model = new SimonClone(this);
                
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ButtonGridView grid = (ButtonGridView) this.findViewById(R.id.button_grid);
        grid.setSimonCloneModel(model);

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
}