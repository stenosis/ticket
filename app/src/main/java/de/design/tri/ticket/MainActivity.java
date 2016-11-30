package de.design.tri.ticket;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

/**
 * The Ticket application is based on a small,
 * simple and single android activity.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Key values
    private final static int PAGE_NR = 0;                               // Pdf page to be rendered
    private final static int ZOOM_FACTOR = 3;                           // Zoom factor of the Pdf
    private final static String KEY_MAIN_IMAGE = "main_image";          // Key value to save the main image
    private final static String KEY_FULLSCREEN_MODE = "fullscreen";     // Key value to save the fullscreen mode
    private final static String FILE_TICKET_IMAGE = "ticket.png";       // File name for the ticket image
    private final static String FILE_TICKET_PDF = "ticket.pdf";         // File name for the ticket Pdf
    private final static String FILE_PDF_MIME = "application/pdf";      // MIME type of a Pdf file
    private final static int REQUEST_CODE_FILE_PERMISSION = 42;         // Permission request code

    // The main image to be displayed
    private Bitmap mainImage = null;
    private boolean fullscreen = false;

    /**
     * The method onCreate gets called by entering the Ticket activity.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Restore the main image
        if (doesFileExists(FILE_TICKET_IMAGE)) {
            receiveMainImageFromInternalStorage();
        } else if (savedInstanceState != null) {
            this.mainImage = savedInstanceState.getParcelable(KEY_MAIN_IMAGE);
            setFrontImage(this.mainImage);
        }

        // Floating action button for selecting a file
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Select a pdf file to be cropped and displayed
                selectFileFromExternalStorage();
            }
        });

        // Deactivate the sidebar menu
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        drawer.setDrawerListener(toggle);
//        toggle.syncState();

//        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
//        navigationView.setNavigationItemSelectedListener(this);

    }


    /**
     * Saving the State
     * @param toSave
     */
    @Override
    public void onSaveInstanceState(Bundle toSave) {
        super.onSaveInstanceState(toSave);
        // We're going to save the main image of the session
        if (this.mainImage != null) toSave.putParcelable(KEY_MAIN_IMAGE, this.mainImage);
        toSave.putBoolean(KEY_FULLSCREEN_MODE, this.fullscreen);
    }

    /**
     * Restoring the state
     * @param savedInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // And also restoring the main image of the session
        if (this.mainImage == null) {
            this.mainImage = savedInstanceState.getParcelable(KEY_MAIN_IMAGE);
            if (this.mainImage != null) setFrontImage(this.mainImage);
        }

        // Restores the fullscreen setting
        this.fullscreen = savedInstanceState.getBoolean(KEY_FULLSCREEN_MODE);
        setFullscreen(this.fullscreen);
    }

    /**
     * Inflate the menu.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Handles the selected option.
     *
     * @param item selected item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_about) {

            displayAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows the about dialog of the Ticket application.
     */
    private void displayAboutDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        builder.setTitle(getResources().getString(R.string.app_name));
        builder.setCancelable(true);
        builder.setIcon(R.mipmap.ic_launcher);

        View view = inflater.inflate(R.layout.dialog_about, null);
        builder.setView(view);

        Display display = getWindowManager().getDefaultDisplay();

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Opens up the GPLv3.
     *
     * @param v
     */
    public void openGPLv3(View v) {

        String url = "https://www.gnu.org/licenses/gpl-3.0.en.html";
        openHomepage(Uri.parse(url));
    }

    /**
     * Open up my Twitter homepage.
     *
     * @param v
     */
    public void openCredits(View v) {

        String url = "https://twitter.com/stenosis101";
        openHomepage(Uri.parse(url));
    }

    /**
     * Opens up a given URL.
     *
     * @param uri to open
     */
    private void openHomepage(Uri uri) {

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(uri);
        startActivity(i);
    }

    /**
     * Handle navigation view item clicks
     *
     * @param item
     * @return
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Starts the file manager to select a file from the external storage.
     */
    private void selectFileFromExternalStorage() {

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_FILE_PERMISSION);

            }
        } else {

            // This always works
            Intent i = new Intent(this, FilePickerActivity.class);

            // Set these depending on your use case. These are the defaults.
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

            // Configure initial directory by specifying a String.
            // You could specify a String like "/storage/emulated/0/", but that can
            // dangerous. Always use Android's API calls to get paths to the SD-card or
            // internal memory.
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
            startActivityForResult(i, 0);
        }
    }

    /**
     * Display the given bitmap into the main image view.
     * @param bitmap
     */
    private void setFrontImage(Bitmap bitmap) {

        if (this.mainImage != null && this.mainImage.getHeight() > 0) {

            ImageView image = (ImageView) findViewById(R.id.pdfViewer);

            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;

            image.setImageBitmap(bitmap);
            image.invalidate();

            // Hide the information default text
            TextView txt = (TextView) findViewById(R.id.txt_choose_ticket);
            txt.setVisibility(View.INVISIBLE);


            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    fullscreen = !fullscreen;
                    setFullscreen(fullscreen);
                }
            });

            // Long time onclick listener for opening the original Pdf file
            image.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    openPdfFile();
                    return false;
                }
            });
        }
    }

    /**
     * Displays a message at the bottom of the main view.
     *
     * @param message to be displayed.
     */
    private void displaySnackbarMessage(String message) {

        RelativeLayout v = (RelativeLayout) findViewById(R.id.content_main);
        Snackbar.make(v, message, Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    /**
     * Result for the file access permission request.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_FILE_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                selectFileFromExternalStorage();
            }

            } else {

                Toast.makeText(this, getResources().getString(R.string.error_permission),
                        Toast.LENGTH_SHORT).show();
                selectFileFromExternalStorage();
            }
    }

    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        /***
         *CROP IMAGE RESULT
         **/
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                Uri resultUri = result.getUri();
                try {

                    // Determine and save the bitmap
                    Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    saveImageToInternalStorage(bmp);
                    this.mainImage = bmp;

                    setFrontImage(bmp);
                    displaySnackbarMessage(getResources().getString(R.string.msg_added_ticket));

                } catch (Exception e) {

                    e.printStackTrace();
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

        /***
         * FILE MANAGER RESULT
         **/
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {

            if (!data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {

                Uri uri = data.getData();
                savePdfToInternalStorage(uri);
                convertPdfToBitmap(uri);
            }
        }
    }

    /**
     * The convertPdfToBitmap is a helper function
     * to convert and render a pdf document to a bitmap.
     *
     * @param uri to the pdf document
     */
    private void convertPdfToBitmap(Uri uri) {

        PdfiumCore pdfiumCore = new PdfiumCore(this);
        try {

            // PDF to image conversion
            // http://www.programcreek.com/java-api-examples/index.php?api=android.os.ParcelFileDescriptor
            ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r");
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
            pdfiumCore.openPage(pdfDocument, PAGE_NR);
            int width = pdfiumCore.getPageWidthPoint(pdfDocument, PAGE_NR) * ZOOM_FACTOR;
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, PAGE_NR) * ZOOM_FACTOR;
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pdfiumCore.renderPageBitmap(pdfDocument, bmp, PAGE_NR, 0, 0, width, height);
            pdfiumCore.closeDocument(pdfDocument);

            // Start the crop image process
            startCropImageProcess(bmp);

        } catch(Exception e) {

            e.printStackTrace();
            displaySnackbarMessage(getResources().getString(R.string.error_open_file));
        }
    }

    /**
     * Start the crop image process.
     *
     * @param bitmap to be cropped.
     */
    private void startCropImageProcess(Bitmap bitmap) {

        try {

            File outputDir = this.getCacheDir();
            File outputFile = File.createTempFile("tmp", ".png", outputDir);

            FileOutputStream out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);

            out.flush();
            out.close();

            Uri uri = Uri.parse(outputFile.toURI().toString());

            //Start the crop image library
            CropImage.activity(uri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);

        } catch (Exception e) {

            e.printStackTrace();
            displaySnackbarMessage(getResources().getString(R.string.error_crop_image));
        }
    }

    /**
     * Saves a copy of the input pdf to the internal storage.
     *
     * @param uri to the pdf.
     */
    private void savePdfToInternalStorage(Uri uri) {

        FileOutputStream out;
        try {

            // Convert input file to byte array
            RandomAccessFile f = new RandomAccessFile(uri.getPath(), "r");
            byte[] data = new byte[(int)f.length()];
            f.readFully(data);

            // Write byte array to a new internal file
            out = openFileOutput(FILE_TICKET_PDF, getApplicationContext().MODE_PRIVATE);
            out.write(data);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.error_saving_pdf), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Saves an image to the internal storage.
     *
     * @param bmp to be saved.
     */
    private void saveImageToInternalStorage(Bitmap bmp) {

        FileOutputStream out;
        try {

            out = openFileOutput(FILE_TICKET_IMAGE, getApplicationContext().MODE_PRIVATE);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);

            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.error_saving_image), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Checks if a given file name exists in the internal storage.
     *
     * @param fname to check
     * @return does the file exists in the internal storage
     */
    private boolean doesFileExists(String fname){

        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    /**
     * Restores the main image from the internal storage.
     */
    private void receiveMainImageFromInternalStorage() {

        // Determine the file path
        String filePath = getApplicationContext().getFilesDir() + "/" + FILE_TICKET_IMAGE;

        // File to Bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        // And display the image
        this.mainImage = bmp;
        setFrontImage(bmp);
    }

    /**
     * Opens up the original stored Pdf file from the internal storage.
     */
    private void openPdfFile() {

        try {
            // Determine the original Pdf file
            File orgFile = new File(getApplicationContext().getFilesDir() + "/"+ FILE_TICKET_PDF);

            // And open it up with a content provider
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "de.design.tri.ticket", orgFile);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_VIEW);
            shareIntent.setDataAndType(contentUri, FILE_PDF_MIME);
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.dialog_open_with)));

        } catch (Exception e) {

            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_open_pdf), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets the fullscreen of the view.
     *
     * @param enable
     */
    private void setFullscreen(boolean enable) {

        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        ActionBar actionBar = getSupportActionBar();

        if (enable) {

            // https://docs.oracle.com/javase/tutorial/java/nutsandbolts/op3.html
            attrs.flags ^= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(attrs);
            actionBar.hide();

        } else {

            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(attrs);
            actionBar.show();
        }
    }
}