package com.mediremind.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility for launching camera capture or gallery pick,
 * and returning the resulting File back to the caller.
 */
public class ImagePickerHelper {

    public static final int REQUEST_CAMERA  = 201;
    public static final int REQUEST_GALLERY = 202;

    private File photoFile;

    /** Launch camera intent and return the Intent to start. photoFile is stored internally. */
    public Intent getCameraIntent(Activity activity) throws IOException {
        photoFile = createImageFile(activity);
        Uri photoUri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".provider",
                photoFile
        );
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        return intent;
    }

    /** Launch gallery picker intent. */
    public Intent getGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        return intent;
    }

    /**
     * Call after onActivityResult with REQUEST_CAMERA to get the captured file.
     * Returns null if photo file wasn't created.
     */
    public File getCapturedFile() {
        return photoFile;
    }

    /**
     * Resolve gallery Uri → File path.
     */
    public static File resolveGalleryUri(Activity activity, Uri uri) throws IOException {
        // Copy to a temp file in cache dir
        File destDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (destDir == null) destDir = activity.getCacheDir();
        File dest = new File(destDir, "gallery_" + System.currentTimeMillis() + ".jpg");

        try (java.io.InputStream in = activity.getContentResolver().openInputStream(uri);
             java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
            if (in == null) throw new IOException("Cannot open gallery URI");
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        }
        return dest;
    }

    private File createImageFile(Activity activity) throws IOException {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) storageDir = activity.getCacheDir();
        return File.createTempFile("PRESCRIPTION_" + stamp, ".jpg", storageDir);
    }
}
