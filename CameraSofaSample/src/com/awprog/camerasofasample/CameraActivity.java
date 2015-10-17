package com.awprog.camerasofasample;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.awprog.camerasofasample.camera.CameraHelper;
import com.awprog.camerasofasample.camera.CameraHelper.OnImageListener;
import com.fbessou.sofa.GamePadIOClient.ConnectionStateChangedListener;
import com.fbessou.sofa.GamePadIOHelper;
import com.fbessou.sofa.GamePadIOHelper.OnCustomMessageReceivedListener;
import com.fbessou.sofa.GamePadInformation;

public class CameraActivity extends Activity implements OnImageListener, OnCustomMessageReceivedListener, ConnectionStateChangedListener {
	CameraHelper camera;
	SurfaceView cameraSurfaceView;
	GamePadIOHelper easyIO;
	Button buttonCapture;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		
		/** Initialize the camera **/
		cameraSurfaceView = (SurfaceView) findViewById(R.id.surfaceViewCamera);
		camera = new CameraHelper(cameraSurfaceView);
		camera.setOnImageListener(this);
		camera.enableAutoFocusOnClickSurfaceView();
		(buttonCapture = (Button) findViewById(R.id.buttonCapture)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				camera.performCapture();
			}
		});
		buttonCapture.setEnabled(false);
		
		/** SOFA **/
		GamePadInformation info = new GamePadInformation("Camera", UUID.randomUUID());
		easyIO = new GamePadIOHelper(this, info);
		easyIO.start(this);
		easyIO.setOnCustomMessageReceivedListener(this);
	}

    @Override
    protected void onResume() {
        super.onResume();
        camera.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.pause();
    }

    
    private long timeLastSend = 0;
    /** Called when a picture has been taken and saved to jpeg **/
    public void onPhotoTaken(byte[] jpegData) {
    	// Don't send the image is the previous has still not been received by the viewer
    	if(timeLastSend == 0 || SystemClock.elapsedRealtime() - timeLastSend > 5000) {
    		// Encode the image to a string
	        String encodedImage = Base64.encodeToString(jpegData, Base64.DEFAULT);
	        timeLastSend = SystemClock.elapsedRealtime();

			// Send the image
			try {
		        JSONObject json = new JSONObject();
		        
		        json.put("time", timeLastSend);
		        json.put("img64", encodedImage);
		        
		        easyIO.sendCustomMessage(json.toString());
			} catch(JSONException e) {
				timeLastSend = 0;
			}
    	}
    }

	@Override
	public void onCustomMessageReceived(String customMessage) {
		try {
			JSONObject json = new JSONObject(customMessage);
			if(json.getLong("time") == timeLastSend) {
				// The viewer has successfully received the image
				timeLastSend = 0;
			}
		} catch(JSONException e) {
			
		}
	}

	
	@Override
	public void onConnectedToProxy() {
		
	}

	@Override
	public void onConnectedToGame() {
		buttonCapture.setEnabled(true);
	}

	@Override
	public void onDisconnectedFromGame() {
		buttonCapture.setEnabled(false);
	}

	@Override
	public void onDisconnectedFromProxy() {
		buttonCapture.setEnabled(false);
	}

}
