package com.example.animalsentimentsapp1;


import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.app.Activity;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Upload2 extends AppCompatActivity {
    ImageView imageVie;
    Button button, button2,buclassify,buttoncapture;
    // TextView classitext;
    private StorageReference storageReference;
    private static final int PICK_IMAGE = 100;
    Uri imageUri,url;
    String email;
    Bitmap bitmap;
    protected Interpreter tflite;
    private MappedByteBuffer tfliteModel;
    private TensorImage inputImageBuffer;
    private  int imageSizeX;
    private  int imageSizeY;
    private  TensorBuffer outputProbabilityBuffer;
    private  TensorProcessor probabilityProcessor;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private List<String> labels;
    String result;
    int mediaReq = 5;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload2);
        // button2 = (Button) findViewById(R.id.next);
        imageVie = (ImageView)findViewById(R.id.imageView2);
        // textview2 = (TextView)findViewById(R.id.textView8);
        button = (Button)findViewById(R.id.upload2);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait.....");
        buclassify=(Button)findViewById(R.id.buclassify2);
        buttoncapture=(Button)findViewById(R.id.capture2);
        // classitext=(TextView)findViewById(R.id.textView8);
        email = SharedPrefManager.getInstance(this).getUserEmail();
        storageReference = FirebaseStorage.getInstance().getReference("Animals");
        buttoncapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 7);
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Image"),mediaReq);

            }
        });
        try{
            tflite=new Interpreter(loadmodelfile(this));
        }catch (Exception e) {
            e.printStackTrace();
        }


//        button2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });
        buclassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                final StorageReference storageReference1 = storageReference.child(System.currentTimeMillis() + "." + GetFileExtension(imageUri));
                storageReference1.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storageReference1.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                url = uri;
                                int imageTensorIndex = 0;
                                int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
                                imageSizeY = imageShape[1];
                                imageSizeX = imageShape[2];
                                DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
                                int probabilityTensorIndex = 0;
                                int[] probabilityShape =
                                        tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
                                DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

                                inputImageBuffer = new TensorImage(imageDataType);
                                outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
                                probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

                                inputImageBuffer = loadImage(bitmap);

                                tflite.run(inputImageBuffer.getBuffer(),outputProbabilityBuffer.getBuffer().rewind());
                                showresult();
                            }
                        });
                    }
                });
            }
        });
    }
    public String GetFileExtension(Uri uri)
    {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK ) {
            if (requestCode == 7)
            {
                bitmap = (Bitmap) data.getExtras().get("data");


                imageVie.setImageBitmap(bitmap);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                String path = MediaStore.Images.Media.insertImage(this.getContentResolver(), bitmap, "Test", null);
                imageUri= Uri.parse(path);
//                bitmap = (Bitmap) data.getExtras().get("data");
//
//
//                imageVie.setImageBitmap(bitmap);
            }
        }
        if (requestCode == mediaReq){
            imageUri = data.getData();
            imageVie.setImageURI(imageUri);
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageVie.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            Toast.makeText(getApplicationContext(),bitmap.toString(),Toast.LENGTH_LONG).show();
        }
    }

//        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE){
//            imageUri = data.getData();
//            imageVie.setImageURI(imageUri);
//            try {
//                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
//                imageVie.setImageBitmap(bitmap);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
////            Toast.makeText(getApplicationContext(),bitmap.toString(),Toast.LENGTH_LONG).show();
//        }

    private TensorImage loadImage(final Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }

    private MappedByteBuffer loadmodelfile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("model2.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }

    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private TensorOperator getPostprocessNormalizeOp(){
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }

    private void showresult(){
        try{
            labels = FileUtil.loadLabels(this,"labels.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
        float maxValueInMap =(Collections.max(labeledProbability.values()));
        progressDialog.dismiss();
        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue()==maxValueInMap) {
                //classitext.setText(entry.getKey());
                //Toast.makeText(getApplicationContext(),entry.getKey(),Toast.LENGTH_LONG).show();
                result = entry.getKey();
                Intent i = new Intent(Upload2.this,Result.class);
                i.putExtra("url",url.toString());
                i.putExtra("email",email);
                i.putExtra("result",result);
                //    i.putExtra("bitmap",bitmap.toString());
                startActivity(i);
            }
        }
    }


}
