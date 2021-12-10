package com.daniele22.mlkitdemo.CaptureFaceDetection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.daniele22.mlkitdemo.MainActivity;
import com.daniele22.mlkitdemo.R;
import com.daniele22.mlkitdemo.utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

/*
Reference  https://developers.google.com/ml-kit/vision/selfie-segmentation/android
 */

public class GalleryFaceSegmentationActivity extends AppCompatActivity {
    private static final String TAG = "PickActivity";
    public static final int REQUEST_CODE_PERMISSION = 111;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"};
    private static final int PICK_IMAGE_CODE = 100;

    private boolean randomImg = true;
    private String imgFilename;
    private ArrayList<String> filenames;

    private ImageView imageView, maskView;
    private ImageButton imageButton;
    private ImageView imageViewCanvas, maskViewCanvas;
    private InputImage image; // the image that will be displayed
    private Bitmap bitmap;
    private Canvas canvas;
    private TextView imgNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("Creation Gallery Face Segmentation Activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_face_segmentation);

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
                imgNameTextView.setText("Image: No img");
                try {
                    image = InputImage.fromFilePath(this, Objects.requireNonNull(data.getData()));
                    initSegmentator(image);
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
        imageView = findViewById(R.id.img_view_pick_segm);
        ImageButton imageButton = findViewById(R.id.img_btn_pick_segm);
        Button reloadBtn = findViewById(R.id.Reload_segm);
        imageViewCanvas = findViewById(R.id.img_view_pick_canvas_segm);
        imgNameTextView = findViewById(R.id.imgNameTextView_segm);

        maskView = findViewById(R.id.mask_view_pick_segm);
        maskViewCanvas = findViewById(R.id.mask_view_pick_canvas_segm);

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
     * Show a random image of CelebA dataset and compute the relative segmentation
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
        //init drawing utils
        bitmap = Bitmap.createBitmap(firebaseVisionImage.getBitmap().getWidth(),
                firebaseVisionImage.getBitmap().getHeight(),
                Bitmap.Config.ARGB_8888);
        //canvas = new Canvas(bitmap);
        image = inputImage;

        // use mlkit to analyse the image
        initSegmentator(inputImage);
    }

    /**
     * Show a specific image of CelebA dataset and compute the relative segmentation
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
        //init drawing utils
        bitmap = Bitmap.createBitmap(firebaseVisionImage.getBitmap().getWidth(),
                firebaseVisionImage.getBitmap().getHeight(),
                Bitmap.Config.ARGB_8888);
        //canvas = new Canvas(bitmap);
        InputImage inputImage = InputImage.fromBitmap(bitmapImg, 0);
        image = inputImage;

        // use mlkit to analyse the image
        initSegmentator(inputImage);
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
        maskView.setImageResource(android.R.color.transparent);
        maskViewCanvas.setImageResource(android.R.color.transparent);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        imgNameTextView.setText("Image: No img");
    }


    private void initSegmentator(InputImage image){
        SelfieSegmenterOptions options =
                new SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        //.enableRawSizeMask()
                        .build();
        Segmenter segmenter = Segmentation.getClient(options);
        Task<SegmentationMask> result =
                segmenter.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<SegmentationMask>() {
                                    @Override
                                    public void onSuccess(SegmentationMask mask) {
                                        // Task completed successfully
                                        // ...
                                        System.out.println("Find segmentation mask");
                                        processMask(mask);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                        System.out.println("Failure in finding segmentation");
                                    }
                                });
    }

    private void processMask(SegmentationMask segmentationMask){
        ByteBuffer mask = segmentationMask.getBuffer();
        int maskWidth = segmentationMask.getWidth();
        int maskHeight = segmentationMask.getHeight();

//        System.out.println("Image w-h: "+bitmap.getWidth()+" "+bitmap.getHeight());
//        System.out.println("Mask w-h: "+maskWidth+" "+maskHeight);
//        Bitmap maskBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
//
//        for (int y = 0; y < maskHeight; y++) {
//            for (int x = 0; x < maskWidth; x++) {
//                // Gets the confidence of the (x,y) pixel in the mask being in the foreground.
//                int bgConfidence = (int)((1.0 - mask.getFloat()) * 255);
//                System.out.println("Background confidence "+bgConfidence);
//                maskBitmap.setPixel(x, y, Color.argb(bgConfidence, 0, 255, 0));
//
////                float foregroundConfidence = mask.getFloat();
////                maskBitmap.setPixel(x, y, Color.argb((int) foregroundConfidence, 0, 255, 0));
//            }
//        }
//        System.out.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEND");
//        //mask.rewind();
//
//        // merge bitmaps
//        //Bitmap merged = Bitmap.createBitmap(image.getWidth(), image.getHeight(), image.getBitmapInternal().getConfig())
//        System.out.println("Drawing bitmappppppp-");
//        Matrix matrix = new Matrix();
//        canvas = new Canvas();
//        canvas.drawBitmap(bitmap, matrix, null);
//        canvas.drawBitmap(maskBitmap, matrix, null);


        Bitmap bitmapMask = Bitmap.createBitmap(maskColorsFromByteBuffer(mask, maskWidth, maskHeight),
                maskWidth,
                maskHeight,
                Bitmap.Config.ARGB_8888);
//        imageView.setImageResource(android.R.color.transparent);
//        imageView.setImageBitmap(bitmapMask);

        Bitmap mutableBitmap = bitmapMask.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmap, new Matrix(), null);
        canvas.drawBitmap(mutableBitmap, new Matrix(), null);
        imageViewCanvas.setImageBitmap(bitmap);
        maskView.setImageBitmap(mutableBitmap);
    }

    /** Converts byteBuffer floats to ColorInt array that can be used as a mask. */
    @ColorInt
    private int[] maskColorsFromByteBuffer(ByteBuffer byteBuffer, int maskWidth, int maskHeight) {
        @ColorInt int[] colors = new int[maskWidth * maskHeight];
        for (int i = 0; i < maskWidth * maskHeight; i++) {
            //float backgroundLikelihood = 1 - byteBuffer.getFloat();
            float foregroundLikelihood =  byteBuffer.getFloat();
            if (foregroundLikelihood > 0.9) {
                colors[i] = Color.argb(128, 255, 0, 255);
            } else if (foregroundLikelihood > 0.2) {
                // Linear interpolation to make sure when backgroundLikelihood is 0.2, the alpha is 0 and
                // when backgroundLikelihood is 0.9, the alpha is 128.
                // +0.5 to round the float value to the nearest int.
                int alpha = (int) (182.9 * foregroundLikelihood - 36.6 + 0.5);
                colors[i] = Color.argb(alpha, 255, 0, 255);
            }
        }
        return colors;
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