package com.daniele22.mlkitdemo.CaptureFaceDetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.daniele22.mlkitdemo.MainActivity;
import com.daniele22.mlkitdemo.R;
import com.daniele22.mlkitdemo.SettingsActivity;
import com.daniele22.mlkitdemo.utils;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GalleryFaceDetectionActivity extends AppCompatActivity {
    private static final String TAG = "PickActivity";
    public static final int REQUEST_CODE_PERMISSION = 111;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"};
    private static final int PICK_IMAGE_CODE = 100;
    private ImageView imageView;
    private ImageView imageViewCanvas;
    private InputImage image; // the image that will be displayed
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint dotPaint, linePaint;
    private TextView smilingView, eyeLeftView, eyeRightView, rotXView, rotYView, rotZView, imgNameTextView;
    private boolean randomImg = true;
    private String imgFilename;
    private ArrayList<String> filenames;
    public static Handler handler = new Handler();
    private Boolean showBboxFace,showBboxEye, showBboxEyebrow, showBboxNose, showBboxLip;
    private Boolean showContourFace,showContourEye, showContourEyebrow, showContourNose, showContourLip;
    private Boolean showLandmark, showAxis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {  // function called on the creation of the page
        System.out.println("Creation Gallery Face Detection Activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_face_detection);

        // Check if an image has been selected from the dropdown or not
        Bundle extras = getIntent().getExtras();
        System.out.println("RANDOM IMAGE? "+extras.getBoolean("random-img"));
        if (extras != null && !extras.getBoolean("random-img")) {
            randomImg = false;
            //The key argument here must match that used in the other activity
            imgFilename = extras.getString("img-filename");
        }
        filenames = MainActivity.getFilenameList();

        // Read settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        //bbox settings
        showBboxFace = sharedPref.getBoolean(SettingsActivity.KEY_BBOX_FACE, false);
        showBboxEye = sharedPref.getBoolean(SettingsActivity.KEY_BBOX_EYE, false);
        showBboxEyebrow = sharedPref.getBoolean(SettingsActivity.KEY_BBOX_EYEBROW, false);
        showBboxNose = sharedPref.getBoolean(SettingsActivity.KEY_BBOX_NOSE, false);
        showBboxLip = sharedPref.getBoolean(SettingsActivity.KEY_BBOX_LIP, false);
        //contour settings
        showContourFace = sharedPref.getBoolean(SettingsActivity.KEY_CONTOUR_FACE, false);
        showContourEye = sharedPref.getBoolean(SettingsActivity.KEY_CONTOUR_EYE, false);
        showContourEyebrow = sharedPref.getBoolean(SettingsActivity.KEY_CONTOUR_EYEBROW, false);
        showContourNose = sharedPref.getBoolean(SettingsActivity.KEY_CONTOUR_NOSE, false);
        showContourLip = sharedPref.getBoolean(SettingsActivity.KEY_CONTOUR_LIP, false);
        //landmark setting
        showLandmark = sharedPref.getBoolean(SettingsActivity.KEY_LANDMARKS, false);
        //axis setting
        showAxis = sharedPref.getBoolean(SettingsActivity.KEY_ORIENTATION_AXIS, false);

        // check if permissions are granted
        if (utils.allPermissionsGranted(REQUIRED_PERMISSIONS, this)) {
            initViews();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_CODE) {
            if (data != null) {
                imageView.setImageURI(data.getData());
                smilingView.setText("Unknown");
                eyeRightView.setText("Unknown");
                eyeLeftView.setText("Unknown");
                rotXView.setText("Unknown");
                rotYView.setText("Unknown");
                rotZView.setText("Unknown");
                imgNameTextView.setText("Image: No img");
                try {
                    image = InputImage.fromFilePath(this, Objects.requireNonNull(data.getData()));
                    initDetector(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Initialize buttons and text
     */
    private void initViews() {  // function called when all permissions are granted
        imageView = findViewById(R.id.img_view_pick);
        ImageButton imageButton = findViewById(R.id.img_btn_pick);
        Button reloadBtn = findViewById(R.id.Reload);
        imageViewCanvas = findViewById(R.id.img_view_pick_canvas);
        smilingView = findViewById(R.id.smilingRes);
        eyeLeftView = findViewById(R.id.eyeLeftRes);
        eyeRightView = findViewById(R.id.eyeRightRes);
        rotXView = findViewById(R.id.RotXRes);
        rotYView = findViewById(R.id.RotYRes);
        rotZView = findViewById(R.id.RotZRes);
        imgNameTextView = findViewById(R.id.imgNameTextView);

        if (randomImg) showRandomImage(filenames);
        else showImage(imgFilename);

        imageButton.setOnClickListener(v -> {
            System.out.println("Clicked image button");
            pickImage();
        });
        reloadBtn.setOnClickListener(v -> {
            System.out.println("Clicked reload button");
            cleanView();
            showRandomImage(filenames);
        });
        //pickImage();
    }

    /**
     * Show a random image of CelebA dataset and compute the relative attributes and landmarks
     * @param filenames list of images of celeba dataset
     */
    public void showRandomImage(ArrayList<String> filenames){
        // select random file
        Random rand = new Random();
        String randomFile = filenames.get(rand.nextInt(filenames.size()));
        imgNameTextView.setText("Image: "+randomFile);

        // extact the file from the folder
        String imagePath = System.getenv("EXTERNAL_STORAGE")+ "/Download/img_align_celeba/" +randomFile;
        Drawable myDrawable = Drawable.createFromPath(imagePath);

        // diplay the file
        Bitmap bitmapImg = BitmapFactory.decodeFile(imagePath);
        InputImage inputImage = InputImage.fromBitmap(bitmapImg, 0);
        ImageView img = new ImageView(this);
        img.setImageDrawable(myDrawable);
        imageView.setImageBitmap(bitmapImg);
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmapImg);
        initDrawingUtils(firebaseVisionImage); // this function initialize some standard params
        image = inputImage;

        // use mlkit to analyse the image
        initDetector(inputImage);
    }

    /**
     * Show a specific image of CelebA dataset and compute the relative attributes and landmarks
     * @param filename
     */
    public void showImage(String filename){
        imgNameTextView.setText("Image: "+filename);

        // extrct the file from the folder
        System.out.println("File selected: "+filename);
        String imagePath = System.getenv("EXTERNAL_STORAGE")+ "/Download/img_align_celeba/" +filename;
        Drawable myDrawable = Drawable.createFromPath(imagePath);
        Bitmap bitmapImg = BitmapFactory.decodeFile(imagePath);

        // display the file
        ImageView img = new ImageView(this);
        img.setImageDrawable(myDrawable);
        imageView.setImageBitmap(bitmapImg);
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmapImg);
        initDrawingUtils(firebaseVisionImage); // this function initialize some standard params
        InputImage inputImage = InputImage.fromBitmap(bitmapImg, 0);
        image = inputImage;

        // use mlkit to analyse the image
        initDetector(inputImage);
    }

    /**
     * Pick an image from the camera
     */
    private void pickImage() {
        System.out.println("Pick image");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_CODE);
    }

    /**
     * Clear all the content of the screen by setting default value
      */
    public void cleanView(){
        imageView.setImageResource(android.R.color.transparent);
        imageViewCanvas.setImageResource(android.R.color.transparent);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        smilingView.setText("Unknown");
        eyeRightView.setText("Unknown");
        eyeLeftView.setText("Unknown");
        rotXView.setText("Unknown");
        rotYView.setText("Unknown");
        rotZView.setText("Unknown");
        imgNameTextView.setText("Image: No img");
    }

    /**
     * display the image on the canvas and show it in imageview with the contours and the landmarks.
     * @param image
     */
    private void initDetector(InputImage image) {
        FaceDetectorOptions detectorOptions = new FaceDetectorOptions
                .Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        FaceDetector faceDetector = FaceDetection.getClient(detectorOptions);
        faceDetector
                .process(image)
                .addOnSuccessListener(mlFaces -> {
                    if (!mlFaces.isEmpty()) {
                        System.out.println("Process faces, num: "+mlFaces.size());
                        processFaces(mlFaces); // process all the faces in the image
                    } else {
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
                        Log.i(TAG, "No faces");
                        imgNameTextView.append(" -> No faces detecetd");
                    }
                }).addOnFailureListener(e -> Log.i(TAG, e.toString()));
    }


    /**
     * Process the faces found by mlkit in the image and drow the landmawks and contour
     * @param faces
     */
    private void processFaces(List<Face> faces) {
        int i = 0;
        for (Face face : faces) {
            // Debugging
            i++;
            System.out.println("Results face "+i);

            if(face.getContour(FaceContour.FACE) == null){
                System.out.println("No face contour detected for face "+i);
            }else{
                System.out.println("Contour detected: "+face.getContour(FaceContour.FACE).getPoints().size());
            }
            // End debugging
            getProps(face);
            if(showLandmark) {
                drawLandMark(face.getLandmark(FaceLandmark.LEFT_EAR));
                drawLandMark(face.getLandmark(FaceLandmark.RIGHT_EAR));
                drawLandMark(face.getLandmark(FaceLandmark.LEFT_EYE));
                drawLandMark(face.getLandmark(FaceLandmark.RIGHT_EYE));
                drawLandMark(face.getLandmark(FaceLandmark.MOUTH_BOTTOM));
                drawLandMark(face.getLandmark(FaceLandmark.MOUTH_LEFT));
                drawLandMark(face.getLandmark(FaceLandmark.MOUTH_RIGHT));
                drawLandMark(face.getLandmark(FaceLandmark.LEFT_CHEEK));
                drawLandMark(face.getLandmark(FaceLandmark.RIGHT_CHEEK));
            }
            drawContours(face.getContour(FaceContour.FACE), showAxis, showContourFace, showBboxFace);
            drawContours(face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM), false, showContourEyebrow, showBboxEyebrow);
            drawContours(face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM), false, showContourEyebrow, showBboxEyebrow);
            drawContours(face.getContour(FaceContour.LEFT_EYE), false, showContourEye, showBboxEye);
            drawContours(face.getContour(FaceContour.RIGHT_EYE), false, showContourEye, showBboxEye);
            drawContours(face.getContour(FaceContour.LEFT_EYEBROW_TOP), false, showContourEyebrow, showBboxEyebrow);
            drawContours(face.getContour(FaceContour.RIGHT_EYEBROW_TOP), false, showContourEyebrow, showBboxEyebrow);
            drawContours(face.getContour(FaceContour.LOWER_LIP_BOTTOM), false, showContourLip, showBboxLip);
            drawContours(face.getContour(FaceContour.LOWER_LIP_TOP), false, showContourLip, showBboxLip);
            drawContours(face.getContour(FaceContour.UPPER_LIP_BOTTOM), false, showContourLip, showBboxLip);
            drawContours(face.getContour(FaceContour.UPPER_LIP_TOP), false, showContourLip, showBboxLip);
            drawContours(face.getContour(FaceContour.NOSE_BRIDGE), false, showContourNose, showBboxNose);
            drawContours(face.getContour(FaceContour.NOSE_BOTTOM), false, showContourNose, showBboxNose);
        }
        imageViewCanvas.setImageBitmap(bitmap);
    }

    /**
     * Analyse the image and compute the probability of Smiling, Leff eye open, right eye open.
     * Then compute the euler angle of rotation X, Y, Z, of the head.
     * @param face
     */
    private void getProps(Face face) {
        String res = "";
        float smileProb = 0, rightEyeOpenProb = 0, leftEyeOpenProb = 0;
        if (face.getSmilingProbability() != null) {
            smileProb = face.getSmilingProbability();
        }
        if (face.getRightEyeOpenProbability() != null) {
            rightEyeOpenProb = face.getRightEyeOpenProbability();
        }
        if (face.getLeftEyeOpenProbability() != null) {
            leftEyeOpenProb = face.getRightEyeOpenProbability();
        }
        // Euler X: A face with a positive Euler X angle is facing upward.
        float rotX = face.getHeadEulerAngleX();
        // Euler Y: A face with a positive Euler Y angle is looking to the right of the camera, or looking to the left if negative.
        float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
        // Euler Z: A face with a positive Euler Z angle is rotated counter-clockwise relative to the camera.
        float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

        // diplay the data in the textviews
        System.out.println("RorX: "+rotX+"; RotY: "+rotY+"; RotZ: "+rotZ);
        DecimalFormat df = new DecimalFormat("##.##"); // max 2 decimals
        df.setRoundingMode(RoundingMode.DOWN);
        smilingView.setText(df.format(smileProb));
        eyeLeftView.setText(df.format(leftEyeOpenProb));
        eyeRightView.setText(df.format(rightEyeOpenProb));
        rotXView.setText(df.format(rotX));
        rotYView.setText(df.format(rotY));
        rotZView.setText(df.format(rotZ));
    }

    /**
     * Draw a landmark in the canvas
     * @param landmark
     */
    private void drawLandMark(FaceLandmark landmark) {
        if (landmark != null) {
            PointF point  = landmark.getPosition();
            canvas.drawCircle(point.x, point.y, 6, dotPaint);
        }
    }

    /**
     * Draw the contour in the canvas
     * @param contour face contour to form the list of points (x, y)
     */
    private void drawContours(FaceContour contour, boolean drawAx, boolean drawContour, boolean drawBbox) {
        int counter = 0;
        if (contour != null){
            List<PointF> points = contour.getPoints();
            float x_min = 1000;
            float x_max = 0;
            float y_min = 1000;
            float y_max = 0;
            for (PointF point : points) {
                if (point.x < x_min) x_min = point.x;
                if (point.x > x_max) x_max = point.x;
                if (point.y < y_min) y_min = point.y;
                if (point.y > y_max) y_max = point.y;
                if (drawContour){  // check if draw contour is enabled
                    if (counter != points.size() - 1) {
                        canvas.drawLine(point.x,
                                point.y,
                                points.get(counter + 1).x,
                                points.get(counter + 1).y,
                                linePaint);
                    } else {
                        canvas.drawLine(point.x,
                                point.y,
                                points.get(0).x,
                                points.get(0).y,
                                linePaint);
                    }
                    counter++;
                    canvas.drawCircle(point.x, point.y, 3, dotPaint);
                }
            }
            // check if draw bbox is enabled
            if (drawBbox)
                canvas.drawRect(x_min, y_min, x_max, y_max, linePaint);
            // check if draw orientation axis is enabled
            if (drawAx){  // TODO controllo sui valori nulli
                drawAxis(Float.parseFloat((String) rotXView.getText()),
                        -Float.parseFloat((String) rotYView.getText()),
                        -Float.parseFloat((String) rotZView.getText()),
                        (x_min+x_max)/2,
                        (y_min+y_max)/2,
                        Math.abs(x_max-x_min));
            }
        }
    }

    private void drawAxis(float pitch, float yaw, float roll, float tdx, float tdy, float size){
        // Referenced from HopeNet https://github.com/natanielruiz/deep-head-pose
        pitch = (float) (pitch * Math.PI / 180);
        yaw = (float) (-(yaw * Math.PI / 180));
        roll = (float) (roll * Math.PI / 180);

//        height, width = img.shape[:2]
//        tdx = width / 2
//        tdy = height / 2

        // X-Axis pointing to right.
        float x1 = (float) (size * (Math.cos(yaw) * Math.cos(roll)) + tdx);
        float y1 = (float) (size * (Math.cos(pitch) * Math.sin(roll) + Math.cos(roll) * Math.sin(pitch) * Math.sin(yaw)) + tdy);

        // Y-Axis |
        //        v
        float x2 = (float) (size * (-Math.cos(yaw) * Math.sin(roll)) + tdx);
        float y2 = (float) (size * (Math.cos(pitch) * Math.cos(roll) - Math.sin(pitch) * Math.sin(yaw) * Math.sin(roll)) + tdy);

        // Z-Axis (out of the screen)
        float x3 = (float) (size * (Math.sin(yaw)) + tdx);
        float y3 = (float) (size * (-Math.cos(yaw) * Math.sin(pitch)) + tdy);

        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.GRAY);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(3f);

//        Path path1 = new Path();
//        path1.moveTo(tdx, tdy);
//        path1.lineTo(x1, y1);
//        canvas.drawPath(path1, linePaint);

        canvas.drawLine(tdx, tdy, x1, y1, axisPaint);
        axisPaint.setColor(Color.WHITE);
        canvas.drawLine(tdx, tdy, x2, y2, axisPaint);
        axisPaint.setColor(Color.CYAN);
        canvas.drawLine(tdx, tdy, x3, y3, axisPaint);
    }


    /**
     * Initialize drawing parameters, colors and create the bitmap image
     * @param image
     */
    private void initDrawingUtils(FirebaseVisionImage image) {
        bitmap = Bitmap.createBitmap(image.getBitmap().getWidth(),
                image.getBitmap().getHeight(),
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        dotPaint = new Paint();
        dotPaint.setColor(Color.RED);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStrokeWidth(1f);
        dotPaint.setAntiAlias(true);
        linePaint = new Paint();
        linePaint.setColor(Color.GREEN);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (utils.allPermissionsGranted(REQUIRED_PERMISSIONS, this)) {
                initViews();
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                System.out.println("Home ");
                Intent homeIntent = new Intent(this, MainActivity.class);
                homeIntent.putExtra("reloadAll", false);
                startActivity(homeIntent);
        }
        return (super.onOptionsItemSelected(menuItem));
    }
}