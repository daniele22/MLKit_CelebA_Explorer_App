package com.daniele22.mlkitdemo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.daniele22.mlkitdemo.CaptureFaceDetection.GalleryFaceDetectionActivity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

//import com.asmaamir.mlkitdemo.CustomModelDetection.CustomModelDetectionActivity;

interface Callback {
    void myResponseCallback(String result, int index);
}

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    public static final int REQUEST_CODE_PERMISSION = 111;

//    private static DrawerLayout drawerLayout;
//    private static ActionBarDrawerToggle actionBarDrawerToggle;

    private File sd;
    private static ArrayList<String> myList;

    public static ArrayList<String> getFilenameList() {
        return myList;
    }

    private int counter = 0; // this is needed to process the images and add write data to file
    private TextView imgNameView;
    private Button invisibleBtn;

    private final boolean do_background_operation = false;
    private Context context;

    /**
     * Get the list of filenames from a file called 'celeba_file_names.txt' save in the asset package
     * @return list of names
     */
    public ArrayList<String> getFileList() {
        // read the txt file 'celeba_file_names.txt' in asset with all the image names
        BufferedReader abc;
        ArrayList<String> celebaFileNames = new ArrayList<>();
        try {
            abc = new BufferedReader(new InputStreamReader(
                    getAssets().open("celeba_file_names.txt")));
            String line;
            while ((line = abc.readLine()) != null) {
                celebaFileNames.add(line);
            }
            abc.close();
            System.out.println("Number of file detected: " + celebaFileNames.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // list of all filenames of celeba dataset
        String[] listCelebaFiles = new String[celebaFileNames.size()];
        listCelebaFiles = celebaFileNames.toArray(listCelebaFiles);
        System.out.println("Number of files of CelebA dataset: " + listCelebaFiles.length);

        // List of files from the folder containing the images
        String root_sd = System.getenv("EXTERNAL_STORAGE");
        System.out.println("ROOT: " + root_sd);
        sd = new File(root_sd + "/Download/img_align_celeba");
//        File list2[] = sd.listFiles();
//        System.out.println("Number of files: "+list.length);
//        for( int i=0; i< list2.length; i++) {
//            String filename = list2[i].getName();
//            if (filename.contains("img") && !lines.contains(filename)){
//                file_list.add(filename);
//            }
//        }

        if (do_background_operation){
            // Read the names of files already analysed
            File path = context.getFilesDir(); // internal storage
            System.out.println("Internal storage path: " + path);
            String tmp_filename = "/celeba-mlkit-analysis_200k.txt"; // file with the data already alanysed
            File tml_file = new File(path, tmp_filename);
            ArrayList<String> allAnalysedFiles = new ArrayList<>(); // list of all filenames
            try {
                abc = new BufferedReader(new FileReader(tml_file));
                String line;
                while ((line = abc.readLine()) != null) {
                    String n = line.split(",")[0];
                    allAnalysedFiles.add(n);
                }
                abc.close();
                System.out.println("Number of file already analysed detected: " + allAnalysedFiles.size());
            } catch (IOException e) {
                e.printStackTrace();
            }

            ArrayList<String> file_list = new ArrayList<>();
            int last = Integer.parseInt(allAnalysedFiles.get(allAnalysedFiles.size() - 1).split("\\.")[0].replace("img", ""));
            for (int i = 0; i < listCelebaFiles.length; i++) {
                String filename = listCelebaFiles[i];//.getName();
                int current = Integer.parseInt(filename.split("\\.")[0].replace("img", ""));
                if ( current > last ){
//                if (filename.contains("img") && !allAnalysedFiles.contains(filename)) {
                    file_list.add(filename);
                }
            }
            System.out.println("filelist final length: " + file_list.size());
            return file_list;
        }else{
            return celebaFileNames;
        }
    }

    /**
     * Check if all permission are granted
     * @return bool
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
                initHome();
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        imgNameView = findViewById(R.id.imgName);
        invisibleBtn = findViewById(R.id.invisibleBtn);
        // The invisible button is needed to avoid lock of the screen
        invisibleBtn.setOnClickListener(v -> System.out.println("CLICKED INVISIBLE BUTTON"));
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION);
        } else {
            initHome();
        }
        // add chronometer to the view
        Chronometer simpleChronometer = findViewById(R.id.simpleChronometer); // initiate a chronometer
        simpleChronometer.start(); // start a chronometer
    }

    /**
     * Init the main scree of the app
     */
    public void initHome() {
        Spinner spinner = findViewById(R.id.spinnerImages);

        Bundle extras = getIntent().getExtras();
        if (extras != null && !extras.getBoolean("reloadAll")) {
            spinner.setSelection(0);
        }else{
            myList = getFileList();

            // Add elements to the spinner
            myList.add(0, "No Selection");
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, myList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // change activity when an item is selected
                String filename = myList.get(position);
                System.out.println("Selected item: " + filename);
                if (!filename.equals("No Selection"))
                    switchActivityWithImg(GalleryFaceDetectionActivity.class, filename);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // do nothing
            }
        });

        Button button = (Button) findViewById(R.id.ShowRandomBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Btn switch");
                switchActivity(GalleryFaceDetectionActivity.class);
            }
        });

//        initNavigationDrawer(); //TODO rimuovi

        if (do_background_operation){
            new LongOperation(this).execute();
            startTimer();
        }

    }

//    private void initNavigationDrawer() {
//        drawerLayout = findViewById(R.id.drawer_layout);
//        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
//        drawerLayout.addDrawerListener(actionBarDrawerToggle);
//        actionBarDrawerToggle.syncState();
//
//        if (getSupportActionBar() != null)
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//
//        NavigationView navigationView = findViewById(R.id.nav_view);
//
//        navigationView.setNavigationItemSelectedListener((MenuItem menuItem) -> {
//                    int id = menuItem.getItemId();
//                    switch (id) {
//                        case R.id.camerax:
//                            switchActivity(CameraxActivity.class);
//                            return true;
//                        case R.id.realtime_face_detection:
//                            switchActivity(RealTimeFaceDetectionActivity.class);
//                            return true;
//                        case R.id.capture_face_detection:
//                            switchActivity(GalleryFaceDetectionActivity.class);
//                            return true;
//                        case R.id.realtime_object_detection:
//                            switchActivity(RealTimeObjectDetectionActivity.class);
//                            return true;
//                        case R.id.face_tracking:
//                            switchActivity(FaceTrackingActivity.class);
//                            return true;
//                        case R.id.object_detection_local_video:
//                            //switchActivity(CustomModelDetectionActivity.class);
//                            return true;
//                        default:
//                            return false;
//                    }
//                }
//        );
//    }

    /**
     * Load a new activity
     * @param c
     */
    private void switchActivity(Class c) {
        Intent intent = new Intent(this, c);
        intent.putExtra("random-img", true);
        this.startActivity(intent);
    }

    /**
     * Load a new activity and pass to the new activity the name of the file to show
     * @param c
     * @param filename file to show in the new activity
     */
    private void switchActivityWithImg(Class c, String filename) {
        Intent intent = new Intent(this, c);
        intent.putExtra("random-img", false);
        intent.putExtra("img-filename", filename);
        this.startActivity(intent);
    }

//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        if (actionBarDrawerToggle.onOptionsItemSelected(item))
//            return true;
//        return super.onOptionsItemSelected(item);
//    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Background computation part
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Compute the info about CelebA dataset and save the information in a txt file.
     * The hole operation is done in background.
     * The function is a recursive function that call itself
     * @param filenames list of file to analyse
     * @param index current index
     */
    private void computeCelebaInfoAndSaveToFile(ArrayList<String> filenames, int index) {

        String mDrawableName = filenames.get(index);
        System.out.println("Compute File: " + mDrawableName);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // set the name of the current alanysed image in a textView
                imgNameView.setText(mDrawableName);
            }
        });

        // read the image from the folder
        String imagePath = System.getenv("EXTERNAL_STORAGE") + "/Download/img_align_celeba/" + mDrawableName;
        Bitmap bitmapImg = BitmapFactory.decodeFile(imagePath);
        InputImage inputImage = InputImage.fromBitmap(bitmapImg, 0);

        // Detect face attributes and wait for the result
        useDetector(inputImage, mDrawableName, index, new Callback() {
            @Override
            public void myResponseCallback(String img_data, int index) {
                // manage the result
                int numValues = img_data.split(",").length;
                img_data = img_data + " ---NumElements: " + numValues;
                // write data in the file
                appendDataToFile(MyApplication.getAppContext(), img_data);
                counter = counter + 1;
                index = index + 1;
                System.out.println("Counter value: " + counter);
                if (index != filenames.size())
                    computeCelebaInfoAndSaveToFile(filenames, index);
            }
        });
        System.out.println("CONTINUE........" + counter);

    }

    /**
     * Use MLKIT to analyse the image
     * @param image
     * @param imagename
     * @param index
     * @param callback
     */
    private void useDetector(InputImage image, String imagename, int index, final Callback callback) {
        System.out.println("USE DETECTON ON: " + imagename);
        AtomicReference<String> data = new AtomicReference<>("");
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
                        if (mlFaces.size() == 1) { // We assume that there is only a single face in the image
                            data.set(getFaceDataString(mlFaces)); // process the face
                        } else {
                            Log.i(TAG, "Multiple faces detected");
                        }
                    } else {
                        Log.i(TAG, "No faces");
                    }
                })
                .addOnFailureListener(e -> Log.i(TAG, e.toString()))
                .addOnCompleteListener(mlfaces -> {
                    System.out.println("callback " + data);
                    // return the result using a callback
                    callback.myResponseCallback(imagename + "," + data.get(), index);
                });
    }


    /**
     * Compute all the data and transform them to a single string separated by commas
     * @param faces we assume a single face
     * @return
     */
    private String getFaceDataString(List<Face> faces) {
        String res = "";
        for (Face face : faces) {
            String props = getPropsString(face);
            String leftear = getLandMarkString(face.getLandmark(FaceLandmark.LEFT_EAR));
            String rightear = getLandMarkString(face.getLandmark(FaceLandmark.RIGHT_EAR));
            String lefteye = getLandMarkString(face.getLandmark(FaceLandmark.LEFT_EYE));
            String righteye = getLandMarkString(face.getLandmark(FaceLandmark.RIGHT_EYE));
            String mouthbottom = getLandMarkString(face.getLandmark(FaceLandmark.MOUTH_BOTTOM));
            String mouthleft = getLandMarkString(face.getLandmark(FaceLandmark.MOUTH_LEFT));
            String mouthright = getLandMarkString(face.getLandmark(FaceLandmark.MOUTH_RIGHT));
            String leftcheek = getLandMarkString(face.getLandmark(FaceLandmark.LEFT_CHEEK));
            String rightcheek = getLandMarkString(face.getLandmark(FaceLandmark.RIGHT_CHEEK));
            String face_p = getContoursString(face.getContour(FaceContour.FACE));
            String lefteyebrowbottom_p = getContoursString(face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM));
            String righteyebrowbottom_p = getContoursString(face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM));
            String lefteye_p = getContoursString(face.getContour(FaceContour.LEFT_EYE));
            String righeye_p = getContoursString(face.getContour(FaceContour.RIGHT_EYE));
            String lefteyebrowtop_p = getContoursString(face.getContour(FaceContour.LEFT_EYEBROW_TOP));
            String righteyebrowtop_p = getContoursString(face.getContour(FaceContour.RIGHT_EYEBROW_TOP));
            String lowerlipbottom_p = getContoursString(face.getContour(FaceContour.LOWER_LIP_BOTTOM));
            String lowerliptop_p = getContoursString(face.getContour(FaceContour.LOWER_LIP_TOP));
            String upperlipbottom_p = getContoursString(face.getContour(FaceContour.UPPER_LIP_BOTTOM));
            String upperliptop_p = getContoursString(face.getContour(FaceContour.UPPER_LIP_TOP));
            String nosebridge_p = getContoursString(face.getContour(FaceContour.NOSE_BRIDGE));
            String nosebottom_p = getContoursString(face.getContour(FaceContour.NOSE_BOTTOM));
            res = props + "," + leftear + "," + rightear + "," + lefteye + "," + righteye + "," + mouthbottom + "," + mouthleft + "," + mouthright + "," + leftcheek + "," + rightcheek + "," + face_p + "," + lefteyebrowbottom_p + "," + righteyebrowbottom_p + "," + lefteye_p + "," + righeye_p + "," + lefteyebrowtop_p + "," + righteyebrowtop_p + "," + lowerlipbottom_p + "," + lowerliptop_p + "," + upperlipbottom_p + "," + upperliptop_p + "," + nosebridge_p + "," + nosebottom_p;
        }
        return res;
    }


    /**
     * Compute 6 values: Euler angles X, Y, Z, Smiling probability, LefEyeOpen probability, rightEyeOpen probability
     * @param face
     * @return
     */
    private String getPropsString(Face face) {
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

        res = rotX + "," + rotY + "," + rotZ + "," + smileProb + "," + rightEyeOpenProb + "," + leftEyeOpenProb;
        System.out.println("Get props; " + res);
        return res;
    }


    /**
     * Compute a string with all the facial landmarks points (x, y)
     * @param landmark
     * @return
     */
    private String getLandMarkString(FaceLandmark landmark) {
        String res = "";
        if (landmark != null) {
            PointF point = landmark.getPosition();
            System.out.println("point: " + point.toString());
            res = point.toString();
            // replacement of , with ; is needed to make a correct csv file
            res = res.replace(",", ";");
        }
        return res;
    }

    /**
     * Compute a string with all the contour data
     * @param contour face contour
     * @return
     */
    private String getContoursString(FaceContour contour) {
        String res = "";
        if (contour != null) {
            List<PointF> points = contour.getPoints();
            List<String> contours = points.stream().map(p -> p.toString()).collect(Collectors.toList());
            res = Arrays.toString(contours.toArray());
            res = res.replace(",", ";");
            System.out.println(res);
        }
        return res;
    }


    /**
     * Write data to a new file
     * @param context
     * @param arr list of data to write
     */
    private void writeDataToFile(Context context, ArrayList<String> arr) {
        File path = context.getFilesDir(); // internal storage
        System.out.println("Filepath: " + path);
        //File path = context.getExternalFilesDir(null); // external storage (SD card)
        String tmp_filename = "celeba-mlkit-analysis_" + counter + ".txt";
        File file = new File(path, tmp_filename);

        try {
            FileWriter writer = new FileWriter(file);
            System.out.println("Writing file to " + file.getName());
            for (String str : arr) {
                System.out.println("Element!");
                writer.write(str + System.lineSeparator());
            }
            writer.close();
        } catch (Exception e) {
            System.out.println("Error during file writing");
            e.getMessage();
        }
    }

    /**
     * Append data to an existing file
     * @param context
     * @param newLine new line that is written
     */
    private void appendDataToFile(Context context, String newLine) {
        File path = context.getFilesDir(); // internal storage
        System.out.println("Filepath: " + path);
        //File path = context.getExternalFilesDir(null); // external storage (SD card)
        String tmp_filename = "celeba-mlkit-analysis_200k.txt";
        File file = new File(path, tmp_filename);

        try {
            FileWriter writer = new FileWriter(file, true);
            System.out.println("Writing file to " + file.getName());
            System.out.println("Element!");
            writer.write(newLine + System.lineSeparator());
            writer.close();
        } catch (Exception e) {
            System.out.println("Error during file writing");
            e.getMessage();
        }
    }

    /**
     * This is the asynchronous task that is done in background
     */
    public class LongOperation extends AsyncTask<Void, Void, Void> {
        public LongOperation(Context context) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            System.out.println("Start computing celeba info");
            // We start from index 1 because the first item is "No Selection" that
            // is added to show no selection in dropdown menu
            computeCelebaInfoAndSaveToFile(myList, 1);
            return null;
        }

        protected void onPostExecute(String result) {
            System.out.println("FINISH CELEBA COMPUTATION");
        }
    }

    final Handler handlertimer = new Handler();
    Timer timer = new Timer();
    TimerTask timerTask;

    /**
     * Automatic button click with timer.
     * This is needed to avoid power of in the android emulator.
     */
    public void callAsynchronousBtnClick() {
        timerTask = new TimerTask() {
            public void run() {
                handlertimer.post(new Runnable() {
                    public void run() {
                        //code to run after every N seconds
//                        invisibleBtn.performClick();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                invisibleBtn.performClick();
                            }
                        });

                    }
                });
            }
        };
    }

    /**
     * timer
     */
    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        callAsynchronousBtnClick();
        // schedule timer every 60 seconds
        timer.schedule(timerTask, 0, 60000);
    }
}