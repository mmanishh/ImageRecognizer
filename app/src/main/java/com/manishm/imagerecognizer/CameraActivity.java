package com.manishm.imagerecognizer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import net.gotev.speech.Speech;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;

public class CameraActivity extends AppCompatActivity {

    private final String TAG = CameraActivity.class.getSimpleName();
    private final int CAMERA_REQUEST_CODE = 2;
    private CameraView cameraView;
    private ImageView imgFrontCam, imgInfo, imgFlash,imgCapture;
    private TextView mTextView;
    private Handler mBackgroundHandler;
    public Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();

        Speech.init(this);

        cameraView = (CameraView) findViewById(R.id.camera_view);
        mTextView = (TextView) findViewById(R.id.tv_camera);
        imgFrontCam = (ImageView) findViewById(R.id.img_front_cam);
        imgFlash = (ImageView) findViewById(R.id.img_flash);
        imgInfo = (ImageView) findViewById(R.id.img_info);
        imgCapture = (ImageView) findViewById(R.id.img_capture);




        /*PermissionUtils.requestPermission(this, CAMERA_REQUEST_CODE, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE);*/

        //cameraView.start();

        if (cameraView != null) {
            cameraView.addCallback(mCallback);
        }

        cameraView.setOnClickListener(onClickListener);
        imgFrontCam.setOnClickListener(onClickListener);
        imgFlash.setOnClickListener(onClickListener);
        imgInfo.setOnClickListener(onClickListener);
        imgCapture.setOnClickListener(onClickListener);
        //Speech.getInstance().say(getResources().getString(R.string.camera_tap_to_capture));


    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.img_flash:
                    if (cameraView.getFlash() == CameraView.FLASH_OFF)
                        cameraView.setFlash(CameraView.FLASH_ON);
                    else
                        cameraView.setFacing(CameraView.FLASH_OFF);
                    break;
                case R.id.img_front_cam:
                    if (cameraView.getFacing() == CameraView.FACING_BACK)
                        cameraView.setFacing(CameraView.FACING_FRONT);
                    else
                        cameraView.setFacing(CameraView.FACING_BACK);
                    break;
                case R.id.img_info:
                    startActivity(new Intent(CameraActivity.this, InfoActivity.class));
                    break;

                case R.id.img_capture:
                    Log.d(TAG + " view tapped", "true");

                    if (CommonUtils.isNetworkAvailable(CameraActivity.this)) {
                        cameraView.takePicture();
                    } else {
                        Speech.getInstance().say(getResources().getString(R.string.warn_internet_conn));
                        mTextView.setText(getResources().getString(R.string.warn_internet_conn));

                    }


                    break;

            }


        }
    };


    private CameraView.Callback mCallback
            = new CameraView.Callback() {
        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);

            //Uri imageUri2 = Uri.fromFile()

            Toast.makeText(cameraView.getContext(), "Picture taken", Toast.LENGTH_SHORT)
                    .show();

            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            uploadImage(bitmap);


            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    // This demo app saves the taken picture to a constant file.
                    // $ adb pull /sdcard/Android/data/com.google.android.cameraview.demo/files/Pictures/picture.jpg
                    File dir = makeDir();
                    File file = new File(dir.getAbsolutePath(),
                            "picture.jpg");


                    imageUri = Uri.fromFile(file);

                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(file);
                        os.write(data);
                        os.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }


                }


            });


            //upload to cloud


        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        if (PermissionUtils.requestPermission2(CameraActivity.this, CAMERA_REQUEST_CODE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE
                , Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE)) {
            cameraView.start();
            Speech.init(CameraActivity.this);
        }
        ;

    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
        Speech.getInstance().stopTextToSpeech();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PermissionUtils.requestPermission2(CameraActivity.this, CAMERA_REQUEST_CODE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE
                , Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE)) {
            cameraView.start();
        }
        Speech.init(CameraActivity.this);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        cameraView.stop();
        Speech.getInstance().stopTextToSpeech();


    }

    private File makeDir() {

        File f3 = new File(Environment.getExternalStorageDirectory() + "/imagrecg/");
        if (!f3.exists())
            f3.mkdirs();
        return f3;
    }


    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "images");
            directory.mkdirs();
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Switch text to loading

        mTextView.setText(getResources().getString(R.string.camera_image_being_upload));
        Speech.getInstance().say(getResources().getString(R.string.camera_image_being_upload));

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(Constants.CLOUD_VISION_API_KEY) {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(Constants.ANDROID_PACKAGE_HEADER, packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(Constants.ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(10);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {

                mTextView.setText(result);
                Speech.getInstance().say(result);
                Toast.makeText(CameraActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public void uploadImage(Bitmap bitmap) {
        if (bitmap != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap finalBitmap =
                        scaleBitmapDown(
                                bitmap,
                                1200);

                callCloudVision(finalBitmap);
                //mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message = "I think image is ";

        Log.d("json_result", response.toString());


        try {
            JSONObject jsonObject = new JSONObject(response.toString());
            Log.d("json_object", jsonObject.toString());

            JSONArray webObject = jsonObject.getJSONArray("responses");
            JSONObject responeObject = webObject.getJSONObject(0);
            JSONArray responseArray = responeObject.getJSONArray("labelAnnotations");
            JSONObject firstObject = responseArray.getJSONObject(0);
            //JSONArray entityArray =firstObject.getJSONArray("webEntities");


            Log.d(TAG + "json_result2", webObject.toString());


            //JSONObject entityObject = entityArray.getJSONObject(0);

            message += firstObject.getString("description");
            double score = firstObject.getDouble("score");


        } catch (JSONException e) {
            e.printStackTrace();
            return "Something is wrong .Please Try again later.";

        }

        return message;
    }


}
