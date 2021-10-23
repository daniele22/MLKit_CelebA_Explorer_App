# 🔥 MLKit CelebA Explorer app
A demo app for Google ML-Kit which is a mobile SDK provided to bring Google's machine learning expertise to mobile apps in a powerful yet easy-to-use way.

![alt text](https://mmlab.ie.cuhk.edu.hk/projects/CelebA/intro.png)


## 📱 About the app
The app allows you to explore the popular CelebA dataset and visualize the prediction of MlKit models for the tasks of *face landmark localization*, *face contour detection* and *face attribute prediction*.

The CelebA dataset is not provided with the application, you can find on [Kaggle](https://www.kaggle.com/jessicali9530/celeba-dataset).

To let the app run properly you have to place the dataset in the folder `sdcard/Download/img_align_celeba`.

### 🧩 Getting Started
The application is very simple and has only two functionalities 
- Show random image (1): takes a random image from the dataset and compute landmark, contour and attributes
- Show specific image (2): you can choose a specific image form the dataset by using the dropdown menu that is shown in the home page
   - These two functionalities open a new screen that display: some attributes computed by MLKit libraries (4), the image with the detection result (5), the image name (6), a button to load an image from the phone gallery (7) and the button Reload to load a new random image (8).
- Compute all the measure available from Face Detection on MLKit and save them to a file `celeba-mlkit-analysis.txt` for all the images in CelebA dataset. This work is done in background, to run this functionatlity you have to change the value of the parameter `do_background_computation` in file `MainActivity.java` and set to it `true`. While the baground computation is being performed, the value of the current image analysed is displayed in the text field (3). The measures computed by MLKit library are: 
    * EulerX, Y and Z angles;
    * landmarks: Face oval, Left eyebrow (top), Left eyebrow (bottom), Right eyebrow (top), Right eyebrow (bottom), Left eye, Right eye, Upper lip (bottom), Lower lip (top), Upper lip (top), Lower lip (bottom), Nose bridge, Nose bottom (note that the center point is at index 128), Left cheek (center), Right cheek (center)
    * classification: left eye open, right eye open, smiling

![alt text](res/app.jpg)

### 🛑 Note
- 👮‍♀️ Presented codes are not optimized, since it is a demo it aims **simplicity**
- 🔎 The aim of the project was to inspect the performances of the MlKit library on a popular dataset, not build an app usable in real senarios





# 📎 Some links to useful resources 


- 💪 &nbsp;CelebA: https://mmlab.ie.cuhk.edu.hk/projects/CelebA.html
- 🗣️ MLKit: https://developers.google.com/ml-kit
- 🏆 Demo App with many MLKit functionalities: https://github.com/asmaamirkhan/MLKitDemo
- ✨Detect faces with ML Kit on Android: https://developers.google.com/ml-kit/vision/face-detection/android


# ✍️ Author   
**[Daniele Filippini]**

