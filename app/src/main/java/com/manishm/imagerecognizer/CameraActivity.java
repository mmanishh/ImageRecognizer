package com.manishm.imagerecognizer;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
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
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.gson.Gson;
import com.manishm.imagerecognizer.model.JsonResponse;
import com.manishm.imagerecognizer.utils.PackageManagerUtils;
import com.manishm.imagerecognizer.utils.PermissionUtils;
import com.manishm.imagerecognizer.utils.SessionManager;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import net.gotev.speech.Speech;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CameraActivity extends AppCompatActivity {

    private final String TAG = CameraActivity.class.getSimpleName();
    private final int CAMERA_REQUEST_CODE = 2;
    private final int GALLERY_PERMISSIONS_REQUEST = 3;
    private final int GALLERY_IMAGE_REQUEST = 31;
    private CameraView cameraView;
    private ImageView imgCapture;
    private TextView mTextView;
    private Handler mBackgroundHandler;
    private SessionManager session;
    private VisionServiceClient client;
    private FloatingActionButton fabInfo,fabCameraToggle,fabVolumeToggle,fabFlash,fabAddFromGallery;

    private boolean volumeToggle = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();

        Speech.init(this);

        session = new SessionManager(this);
        //volumeToggle = session.getValue(SessionManager.TOGGLE_VALUE);

        cameraView = (CameraView) findViewById(R.id.camera_view);
        mTextView = (TextView) findViewById(R.id.tv_camera);
        imgCapture = (ImageView) findViewById(R.id.img_capture);
        fabAddFromGallery = (FloatingActionButton) findViewById(R.id.fab_menu_add_photo);
        fabCameraToggle = (FloatingActionButton) findViewById(R.id.fab_menu_toggle_camera);
        fabFlash = (FloatingActionButton) findViewById(R.id.fab_menu_toggle_flash);
        fabVolumeToggle = (FloatingActionButton) findViewById(R.id.fab_menu_toggle_volume);
        fabInfo = (FloatingActionButton) findViewById(R.id.fab_menu_info);



        if(!session.getValue(SessionManager.TOGGLE_SPEAKER_VALUE))
            fabVolumeToggle.setImageResource(R.drawable.ic_volume_off);

        if(!session.getValue(SessionManager.TOGGLE_SPEAKER_VALUE))
            fabFlash.setImageResource(R.drawable.ic_flash_off);

        if (client == null) {
            client = new VisionServiceRestClient(getString(R.string.subscription_key));
        }



        if (cameraView != null) {
            cameraView.addCallback(mCallback);
        }

        cameraView.setOnClickListener(onClickListener);
        imgCapture.setOnClickListener(onClickListener);


        fabInfo.setOnClickListener(onClickListener);
        fabVolumeToggle.setOnClickListener(onClickListener);
        fabFlash.setOnClickListener(onClickListener);
        fabCameraToggle.setOnClickListener(onClickListener);
        fabAddFromGallery.setOnClickListener(onClickListener);


    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            switch (view.getId()) {
                case R.id.fab_menu_toggle_flash:
                    toggleFlashIcon();
                    break;
                case R.id.fab_menu_toggle_camera:
                    if (cameraView.getFacing() == CameraView.FACING_BACK)
                        cameraView.setFacing(CameraView.FACING_FRONT);
                    else
                        cameraView.setFacing(CameraView.FACING_BACK);
                    break;
                case R.id.fab_menu_info:
                    startActivity(new Intent(CameraActivity.this, InfoActivity.class));
                    break;

                case R.id.fab_menu_add_photo:
                    startGalleryChooser();
                    break;

                case R.id.fab_menu_toggle_volume:
                    toggleVolumeIcon();

                    break;

                case R.id.img_capture:
                    Log.d(TAG + " view tapped", "true");

                    if (CommonUtils.isNetworkAvailable(CameraActivity.this)) {
                        cameraView.takePicture();
                    } else {
                        notifyUser(getResources().getString(R.string.warn_internet_conn));
                    }


                    break;

            }


        }
    };

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission2(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }


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


            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            uploadImage(bitmap);


/*
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
*/


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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                if (CommonUtils.isNetworkAvailable(CameraActivity.this)) {
                    uploadImage(bitmap);
                } else {
                    notifyUser(getResources().getString(R.string.warn_internet_conn));

                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(CameraActivity.this, "Please choose another image.", Toast.LENGTH_LONG).show();

            }


        }
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
        if (volumeToggle)
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
                            Feature logoDetection = new Feature();
                            logoDetection.setType("LOGO_DETECTION");
                            logoDetection.setMaxResults(10);
                            add(logoDetection);
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
                if (volumeToggle)
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
            /*try {*/
            // scale the image to save on bandwidth
            Bitmap finalBitmap =
                    scaleBitmapDown(
                            bitmap,
                            1200);

            sentToServer(finalBitmap);
            //mMainImage.setImageBitmap(bitmap);

           /* } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }*/
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
            JSONArray labelResponseArray = responeObject.getJSONArray("labelAnnotations");
            JSONObject labelObject = labelResponseArray.getJSONObject(0);
            /*JSONArray logoResponseArray = responeObject.getJSONArray("logoAnnotations");
            JSONObject logoObject = labelResponseArray.getJSONObject(0);*/

            Log.d(TAG + "json_result2", webObject.toString());


            Gson gson = new Gson();

            JsonResponse gsondata = gson.fromJson(String.valueOf(response), JsonResponse.class);

            if (gsondata.getResponses().get(0).getLogoAnnotations().get(0).getDescription() != null) {
                message += gsondata.getResponses().get(0).getLogoAnnotations().get(0).getDescription();
                double score = labelObject.getDouble("score");
            } else {
                message += gsondata.getResponses().get(0).getLabelAnnotations().get(0).getDescription();
                double score = labelObject.getDouble("score");
            }
            //JSONObject entityObject = entityArray.getJSONObject(0);


        } catch (JSONException e) {
            e.printStackTrace();
            return "Something is wrong .Please Try again later.";

        }

        return message;
    }

    private String process(Bitmap bitmap) throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        AnalysisResult v = this.client.describe(inputStream, 1);

        String result = gson.toJson(v);
        Log.d(TAG + "microsoft result", result);

        return result;
    }

    private void sentToServer(final Bitmap bitmap) {
        final Exception[] error = {null};

        notifyUser(getResources().getString(R.string.camera_image_being_upload));

        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... args) {
                try {
                    return process(bitmap);
                } catch (Exception e) {
                    error[0] = e;    // Store error
                }

                return null;
            }

            @Override
            protected void onPostExecute(String data) {
                super.onPostExecute(data);
                // Display based on error existence
                if (error[0] != null) {
                    mTextView.setText(error[0].getMessage());
                } else {
                    Gson gson = new Gson();
                    AnalysisResult result = gson.fromJson(data, AnalysisResult.class);
                    for (Caption c : result.description.captions) {
                        notifyUser(c.text);
                    }
                }


            }

        }.execute();
    }

    private class doRequest extends AsyncTask<Bitmap, String, String> {
        // Store error message
        private Exception e = null;

        public doRequest() {
            mTextView.setText(getResources().getString(R.string.camera_image_being_upload));

        }

        @Override
        protected String doInBackground(Bitmap... args) {
            try {
                if (args[0] != null)
                    return process(args[0]);
            } catch (Exception e) {
                this.e = e;    // Store error
            }

            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            // Display based on error existence
            if (e != null) {
                mTextView.setText(e.getMessage());
            } else {
                Gson gson = new Gson();
                AnalysisResult result = gson.fromJson(data, AnalysisResult.class);
                mTextView.setText(result.description.captions.get(0).toString());
            }


        }

    }

    private void notifyUser(String str) {
        mTextView.setText(str);
        if (session.getValue(SessionManager.TOGGLE_SPEAKER_VALUE))
            Speech.getInstance().say(str);
    }

    private void toggleVolumeIcon() {
        if (session.getValue(SessionManager.TOGGLE_SPEAKER_VALUE)) {
            volumeToggle = false;
            session.toggleSpeaker(false);
            fabVolumeToggle.setImageResource(R.drawable.ic_volume_off);

        } else {
            volumeToggle = true;
            session.toggleSpeaker(true);
            fabVolumeToggle.setImageResource(R.drawable.ic_volume_up_black_24px);

        }
    }

    private void toggleFlashIcon() {
        if (session.getValue(SessionManager.TOGGLE_FLASH_VALUE)) {
            session.toggleFlash(false);
            fabFlash.setImageResource(R.drawable.ic_flash_off);
            cameraView.setFlash(CameraView.FLASH_OFF);

        } else {
            session.toggleFlash(true);
            fabFlash.setImageResource(R.drawable.ic_flash_on_black_24px);
            cameraView.setFlash(CameraView.FLASH_ON);

        }
    }
}
