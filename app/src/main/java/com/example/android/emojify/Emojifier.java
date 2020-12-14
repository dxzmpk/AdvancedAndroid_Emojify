package com.example.android.emojify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class Emojifier {

    private static final String TAG = "Emojifier";

    public static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    public static Bitmap detectFacesAndOverlayEmoji(final Context context, final Bitmap bitmap)  {


//        Bitmap bitmap = BitmapFactory.decodeFile("/mnt/sdcard/DX/faceTest.jpg");

        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        final Bitmap[] resultBitMap = {bitmap};

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        final FaceDetector detector = FaceDetection.getClient(highAccuracyOpts);

        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
//                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
//                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees
//
//                                            Emoji emoji = whichEmoji(face);
//                                            Bitmap emojiBitMap = getEmojiPic(context, emoji);
//                                            resultBitMap[0] = addBitmapToFace(resultBitMap[0], emojiBitMap, face);
                                        }

                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                        Log.w(TAG, "Failed" + e.getMessage());
                                        Toast.makeText(context, "No faces Dected", Toast.LENGTH_SHORT).show();
                                    }
                                });

        List<Face> faces = result.getResult();
        for (Face face : faces) {
            Rect bounds = face.getBoundingBox();
            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

            Emoji emoji = whichEmoji(face);
            Bitmap emojiBitMap = getEmojiPic(context, emoji);
            resultBitMap[0] = addBitmapToFace(resultBitMap[0], emojiBitMap, face);
        }

        return resultBitMap[0];


    }

    static Bitmap getEmojiPic(final Context context, Emoji emoji) {
        Bitmap emojiBitmap = null;
        switch (emoji) {
            case SMILE:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.smile);
                break;
            case FROWN:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.frown);
                break;
            case LEFT_WINK:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.leftwink);
                break;
            case RIGHT_WINK:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.rightwink);
                break;
            case LEFT_WINK_FROWN:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.leftwinkfrown);
                break;
            case RIGHT_WINK_FROWN:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.rightwinkfrown);
                break;
            case CLOSED_EYE_SMILE:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.closed_smile);
                break;
            case CLOSED_EYE_FROWN:
                emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.closed_frown);
                break;
            default:
                emojiBitmap = null;
                Toast.makeText(context, R.string.no_emoji, Toast.LENGTH_SHORT).show();
        }
        return emojiBitmap;
    }


    private static final double SMILING_PROB_THRESHOLD = .15;
    private static final double EYE_OPEN_PROB_THRESHOLD = .5;

    static Emoji whichEmoji(Face face) {

        // Log all the probabilities
        Log.d(TAG, " whichEmoji: smilingProb = "
                + face.getSmilingProbability());
        Log.d(TAG, " whichEmoji: leftEyeOpenProb = "
                + face.getLeftEyeOpenProbability());
        Log.d(TAG, " whichEmoji: rightEyeOpenProb = "
                + face.getRightEyeOpenProbability());


        boolean smiling = face.getSmilingProbability() > SMILING_PROB_THRESHOLD;

        boolean leftEyeClosed = face.getLeftEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;
        boolean rightEyeClosed = face.getRightEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;
        // Determine and log the appropriate emoji
        Emoji emoji;
        if(smiling) {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.LEFT_WINK;
            }  else if(rightEyeClosed && !leftEyeClosed){
                emoji = Emoji.RIGHT_WINK;
            } else if (leftEyeClosed){
                emoji = Emoji.CLOSED_EYE_SMILE;
            } else {
                emoji = Emoji.SMILE;
            }
        } else {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.LEFT_WINK_FROWN;
            }  else if(rightEyeClosed && !leftEyeClosed){
                emoji = Emoji.RIGHT_WINK_FROWN;
            } else if (leftEyeClosed){
                emoji = Emoji.CLOSED_EYE_FROWN;
            } else {
                emoji = Emoji.FROWN;
            }
        }
        Log.d(TAG, "whichEmoji: " + emoji.name());
        return emoji;
    }


    private static float EMOJI_SCALE_FACTOR = 0.9f;

    /**
     * 将原始照片与表情符号位图相结合
     *
     * @param backgroundBitmap 原始照片
     * @param emojiBitmap      所选表情符号
     * @param face             检测到的面孔
     * @return 最终位图，包括面孔表情符号
     */
    static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        Rect bounds = face.getBoundingBox();
        // 将结果位图初始化为原始图片的可变副本
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // 调整表情符号，使其在面孔上的效果看起来更好
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // 根据面孔宽度判断表情符号的大小，并保持宽高比
        int newEmojiWidth = (int) (bounds.width() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // 调整表情符号
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // 判断表情符号的位置，以便与面孔保持一致
        float emojiPositionX =
                (bounds.left + bounds.width() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (bounds.top + bounds.height() ) - emojiBitmap.getHeight() / 3;

        // 创建画布并在上面绘制位图
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }


    enum Emoji {
        SMILE,
        FROWN,
        LEFT_WINK,
        RIGHT_WINK,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_SMILE,
        CLOSED_EYE_FROWN
    }

}
