package com.daniele22.mlkitdemo.CaptureFaceDetection;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

import com.daniele22.mlkitdemo.MainActivity;
import com.daniele22.mlkitdemo.R;
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

        // check if permissions are granted
        if (allPermissionsGranted()) {
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
        for (Face face : faces) {
            getProps(face);
            drawLandMark(face.getLandmark(FaceLandmark.LEFT_EAR));
            drawLandMark(face.getLandmark(FaceLandmark.RIGHT_EAR));
            drawLandMark(face.getLandmark(FaceLandmark.LEFT_EYE));
            drawLandMark(face.getLandmark(FaceLandmark.RIGHT_EYE));
            drawLandMark(face.getLandmark(FaceLandmark.MOUTH_BOTTOM));
            drawLandMark(face.getLandmark(FaceLandmark.MOUTH_LEFT));
            drawLandMark(face.getLandmark(FaceLandmark.MOUTH_RIGHT));
            drawLandMark(face.getLandmark(FaceLandmark.LEFT_CHEEK));
            drawLandMark(face.getLandmark(FaceLandmark.RIGHT_CHEEK));
            drawContours(face.getContour(FaceContour.FACE));
            drawContours(face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM));
            drawContours(face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM));
            drawContours(face.getContour(FaceContour.LEFT_EYE));
            drawContours(face.getContour(FaceContour.RIGHT_EYE));
            drawContours(face.getContour(FaceContour.LEFT_EYEBROW_TOP));
            drawContours(face.getContour(FaceContour.RIGHT_EYEBROW_TOP));
            drawContours(face.getContour(FaceContour.LOWER_LIP_BOTTOM));
            drawContours(face.getContour(FaceContour.LOWER_LIP_TOP));
            drawContours(face.getContour(FaceContour.UPPER_LIP_BOTTOM));
            drawContours(face.getContour(FaceContour.UPPER_LIP_TOP));
            drawContours(face.getContour(FaceContour.NOSE_BRIDGE));
            drawContours(face.getContour(FaceContour.NOSE_BOTTOM));
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
    private void drawContours(FaceContour contour) {
        int counter = 0;
        if (contour != null){
            List<PointF> points = contour.getPoints();
            for (PointF point : points) {
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


    /**
     * Check if all permissions are granted
     * @return
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
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