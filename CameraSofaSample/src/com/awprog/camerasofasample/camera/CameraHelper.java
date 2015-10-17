package com.awprog.camerasofasample.camera;

import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;

public class CameraHelper implements Callback {

    // Path of the directory in the external storage
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/testOCR/";
    // Jpeg file name for the picture taken
    public static final String TMP_JPEG_PATH = DATA_PATH +"tmp_ocr.jpg";
    // Camera & Co
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private PictureCallback jpegCallback;
    private Camera camera;
    private int cameraId;
    // camera states
    private boolean cameraIsOn = false;
    private boolean cameraPreviewIsEnable = false;
    private boolean cameraIsAutoFocusState = false;
    
    OnImageListener listener;
    
    public CameraHelper(SurfaceView surfaceView) {
		this.surfaceView = surfaceView;
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Create a callback to handle the picture taken by the camera
        jpegCallback = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                // Re-enable preview
                camera.startPreview();
                cameraPreviewIsEnable = true;

                if(listener != null)
                	listener.onPhotoTaken(data);
            }
        };
        
        // get the camera id
        cameraId = findBackFacingCamera();
	}
    
    public void performAutoFocus() {
    	// Start auto focus
        if(cameraIsOn && cameraPreviewIsEnable && !cameraIsAutoFocusState) {
            cameraIsAutoFocusState = true;

            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    cameraIsAutoFocusState = false;
                }
            });
        }
    }
    public void performCapture() {
    	// Take a picture if the camera is ready
        if (cameraIsOn && cameraPreviewIsEnable && !cameraIsAutoFocusState) {
            camera.takePicture(null, null, jpegCallback);
            // the camera preview has been disabled by takePicture()
            cameraPreviewIsEnable = false;
        }
    }
    
    public void enableAutoFocusOnClickSurfaceView() {
    	surfaceView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				performAutoFocus();
			}
		});
    }
    
    public void resume() {
    	if(cameraId != -1) {
            // Initialize camera
            camera = Camera.open(cameraId);
            camera.getParameters().setPreviewFpsRange(15, 25);
            cameraIsOn = true;
        }
    }
    public void pause() {
        // cancel current auto focus
        if(cameraIsAutoFocusState) {
            camera.cancelAutoFocus();
            cameraIsAutoFocusState = false;
        }

        // Disable preview
        camera.stopPreview();
        cameraPreviewIsEnable = false;

        // release camera for other app
        camera.release();
        cameraIsOn = false;
    }

    /** return the id of the back facing camera **/
    private int findBackFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    /** Select appropriate the picture size according to the preview size **/
    private Camera.Size selectPictureSize(Camera.Size preview) {
        // ratio of the preview image
        float ratioPreview = (float) preview.width / preview.height;

        // get the list of available sizes for picture
        List<Camera.Size> list = camera.getParameters().getSupportedPictureSizes();

        Camera.Size bestSize = list.get(0);
        float nearestRatio = (float) bestSize.width / bestSize.height;

        // find the best size with the best ratio
        for(Camera.Size size : list) {
            // the picture must not be smaller than the preview
            if (size.width >= preview.width && size.height >= preview.height) {
                float ratio = (float) size.width / size.height;
                // select it if the ratio is closer to the preview ratio
                if (Math.abs(ratio - ratioPreview) <= Math.abs(nearestRatio - ratioPreview)) {
                    nearestRatio = ratio;
                    bestSize = size;
                }
            }
        }

        return bestSize;
    }

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
            // define this surface view as the preview display for th ecamera
            camera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(camera != null) {
            Camera.Parameters params = camera.getParameters();

            // Get the list of available size for the preview
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            // use the first size of the list (supposed to be the biggest)
            Camera.Size selected = sizes.get(0);
            params.setPreviewSize(selected.width, selected.height);

            // Select an appropriated picture size
            Camera.Size picture = selectPictureSize(selected);
            params.setPictureSize(picture.width, picture.height);

            // Apply the params and enable preview
            camera.setParameters(params);
            camera.startPreview();
            cameraPreviewIsEnable = true;
        }
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		
	}
	
	public void setOnImageListener(OnImageListener l) {
		listener = l;
	}
	
	public interface OnImageListener {
		void onPhotoTaken(byte[] jpg);
	}
}
