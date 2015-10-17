package com.awprog.camerasofasample;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Base64;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.fbessou.sofa.GameIOClient.ConnectionStateChangedListener;
import com.fbessou.sofa.GameIOHelper;
import com.fbessou.sofa.GameIOHelper.CustomMessageListener;
import com.fbessou.sofa.GameIOHelper.GamePadCustomMessage;
import com.fbessou.sofa.GameIOHelper.GamePadStateChangedEvent;
import com.fbessou.sofa.GameIOHelper.StateChangedEventListener;
import com.fbessou.sofa.GameInformation;

public class ViewerActivity extends Activity implements ConnectionStateChangedListener, CustomMessageListener, StateChangedEventListener {
	GameIOHelper easyIO;
	TextView textConnected;
	SurfaceView surfaceViewer;
	SurfaceHolder holderViewer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_viewer);
		
		textConnected = (TextView) findViewById(R.id.textConnected);
		surfaceViewer = (SurfaceView) findViewById(R.id.surfaceViewViewer);
		holderViewer = surfaceViewer.getHolder();
		
		/** SOFA **/
		GameInformation info = new GameInformation("Image Viewer");
		easyIO = new GameIOHelper(this, info, null, this, this);
		easyIO.start(this);
	}
	
	@Override
	public void onConnected() {
		textConnected.setText("Connected to proxy");
	}
	@Override
	public void onDisconnected() {
		textConnected.setText("Disconnected from proxy");
	}
	
	Bitmap image;
	byte[] buffer = new byte[32*1024];
	private void displayJpeg(final String jpegBase64) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// Decode the string
				byte[] jpeg = Base64.decode(jpegBase64, Base64.DEFAULT);
				// Decode the jpeg
				Options opts = new Options();
				if(image != null)
					opts.inBitmap = image;
				opts.inPreferQualityOverSpeed = false;
				opts.inPreferredConfig = Bitmap.Config.RGB_565;
				opts.inTempStorage = buffer;
				Bitmap img = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
				image = img;
				if(image == null)
					return;
				
				Canvas canvas = holderViewer.lockCanvas();
				if(canvas == null)
					return;
				
				float scale = Math.min(canvas.getHeight()/(float)image.getHeight(), canvas.getWidth()/(float)image.getWidth());
				canvas.scale(scale, scale);
				canvas.drawBitmap(image, 0, 0, null);
				
				holderViewer.unlockCanvasAndPost(canvas);
			}
		}).start();
	}

	@Override
	public void onCustomMessageReceived(GamePadCustomMessage message) {
		try {
			// Read the message
			JSONObject json = new JSONObject(message.customMessage);
			long time = json.getLong("time");
			// Decode the image and display it
			displayJpeg(json.getString("img64"));
			
			// Send an answer to the game pad
			JSONObject answer = new JSONObject();
			answer.put("time", time);
			easyIO.sendCustomMessage(answer.toString(), message.gamePadId);
		} catch (JSONException e) {
		}
	}

	@Override
	public void onPadEvent(GamePadStateChangedEvent event) {
		switch(event.eventType) {
		case JOINED:
			textConnected.setText(easyIO.getGamePadInformation(event.gamePadId).staticInformations.getNickname()+" is connected");
			break;
		case INFORMATION:
			break;
		case LEFT:
		case UNEXPECTEDLY_DISCONNECTED:
			textConnected.setText(easyIO.getGamePadInformation(event.gamePadId).staticInformations.getNickname()+" is gone");
			break;
		}
	}
}
