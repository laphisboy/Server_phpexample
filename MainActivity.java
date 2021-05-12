package com.example.androidphpfile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class MainActivity extends Activity {
    /* Activity is the base class to build the screens of your application
    and it has all the lifecycle callbacks expected by the Android Framework.
    */

    TextView messageText;
    // A user interface element that displays text to the user.

    Button uploadButton;
    // A user interface element the user can tap or click to perform an action.

    int serverResponseCode = 0;

    ProgressDialog dialog = null;
    // A dialog showing a progress indicator

    String uploadServerUri = null;


    /************* File Path **************/

    // final class : class that can't be extended
    final String uploadFilePath = "/storage/emulated/0/DCIM/Camera/";
    final String uploadFileName = "IMG_20210407_043542.jpg";
    // need to turn on phone and check directory
    // and take a photo and check its name :)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        OnCreate(Bundle savedInstanceState):
            https://stackoverflow.com/questions/10810418/whats-oncreatebundle-savedinstancestate

        OnCreate:
            리소스를 새롭게 갱신해야 할 때 호출된다.
            is where you initialize your activity
            usually call setContentView(int) with a layout resource defining your UI
            and using findViewById(int) to retrieve the widgets in that UI that you need


        protected:
            Protecting a constructor prevents the users from
            creating the instance of the class, outside the package.

        Bundle:
            A mapping from String keys to various Parcelable values. hmm...

            Bundle은 여러가지의 타입의 값을 저장하는 Map 클래스이다.
            기본타입인 int, double, long, String 부터 FloatArray, StringArrayList, Serializable, Parcelable 까지 구현한다

            Android에서는 Activity간에 데이터를 주고 받을 때 Bundle 클래스를 사용하여 데이터를 전송한다


        Bundle savedInstanceState:
            Activity를 생성할 때 아래와 같이 Bundle savedInstanceState 객체를 가지고 와서
            액티비티를 중단할 때 savedInstanceState 메서드를 호출하여 임시적으로 데이터를 저장한다
        */

        super.onCreate(savedInstanceState);
        // reaches Activity to load saveInstanceState

        setContentView(R.layout.activity_main);
        // Activity의 setContentView() 메소드의 인자로 레이아웃 리소스 ID를 전달해서,
        // activity_main.xml로 만든 레이아웃이 출력된다.

        messageText = (TextView)findViewById(R.id.messageText);
        uploadButton = (Button)findViewById(R.id.uploadButton);
        // findViewById: activity_main.xml 에 설정된 View를 가져와서 수정할 수 있게끔
        //               해주는 method


        uploadServerUri = "http://61.73.67.88/UploadToServer.php";

        uploadButton.setOnClickListener(new View.OnClickListener() {
            // the system executes the code you write in onClick(View)
            // after the user presses the button.

            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
                // shows progress dialog (pop-up)

                new Thread(new Runnable(){
                    // what is a thread?
                    // path followed for execution

                    public void run(){
                        runOnUiThread(new Runnable(){
                            public void run() {
                                messageText.setText("uploading started...");
                            }
                        });

                        uploadFile(uploadFilePath + "" + uploadFileName);
                    }
                }).start(); // begin execution of thread - calls run() in Runnable()
            }
        });
    }

    public int uploadFile(String sourceFileUri) {
        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        // A URLConnection with support for HTTP-specific features

        DataOutputStream dos = null;
        //  lets an application write primitive Java data types
        //  to an output stream in a portable way
        // (not 100% sure about this yet) : 자바 형식의 데이터를 어플에 출력할 수 있는 변수


        //////////////////////////////////////////////////////
        // Need to find out why these strings are important //
        //////////////////////////////////////////////////////
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        ///////////////////////////////////////////////////////


        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        File sourceFile = new File(sourceFileUri);
        // File: abstract representation of file and directory pathnames :)

        if (!sourceFile.isFile()){

            dialog.dismiss(); // dismiss the dialog (whatever that means)

            Log.e("uploadFile", "Source File does not exist :"
                    + uploadFilePath + "" + uploadFileName);
            // send error message


            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File does not exist :"
                            + uploadFilePath + "" + uploadFileName);
                }
            });
            // makes Runnable() code get executed on the Main Thread

            return 0;

        }
        else {  // sourceFile.isFile()
            try{

                // open a URL connection to the Servlet (=small program that runs on server)
                ////////////////////////////////////
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                // reading raw bytes of file

                URL url = new URL(uploadServerUri);
                // URL(http purpose) is part of URI


                // open a HTTP connection to the URL
                ////////////////////////////////////
                conn = (HttpURLConnection) url.openConnection(); // connect to URL

                conn.setDoInput(true); // allow inputs
                conn.setDoOutput(true); // allow outputs
                conn.setUseCaches(false); // don't use a cached copy

                conn.setRequestMethod("POST");

                // HTTP Headers
                // set value for each header(?) before sending to server (?)
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                // ???? what is ENCTYPE
                // enctype="multipart/form-data"

                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
                // request body 전달시 multipart/form-data;boundary=***** 로 서버에 전달 (?)
                // "*****" is user-defined value to split data

                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());
                // getOutputStream(): returns output stream for writing bytes into this socket
                // DataOutputStream(): let application write Java data types to output stream

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);
                // s in Byte(s) represents string
                // Writes out the string to the underlying output stream as a sequence of bytes.


                // create a buffer of maximum size
                ///////////////////////////////////
                bytesAvailable = fileInputStream.available();
                // .available(): returns the estimate of number of remaining bytes
                //               that can be read from input stream
                //               without blocking next invocation of a method(?)

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                // int maxBufferSize = 1 * 1024 * 1024;

                buffer = new byte[bufferSize];


                // read file and write
                ////////////////////////
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                // read(byte[] b, int off, int len) off: offset
                // Reads up to len bytes of data from this input stream into an array of bytes.

                while(bytesRead > 0){
                    dos.write(buffer, 0, bufferSize);
                    // write(byte[] b, int off, int len)
                    // Writes len bytes from the specified byte array starting at
                    // offset off to the underlying output stream.

                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    // keep read & writing file
                    // within the buffer window (either available bytes or buffer size)
                    // until there is no more to read (?)
                }


                // send multipart form data necesssary after file data...
                ///////////////////////////////////////////////////////////

                // nope I think its just
                // since all required data is written
                // need to split with boundary
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                ////////////////////////////////////////////////

                serverResponseCode = conn.getResponseCode();
                // Gets the status code from an HTTP response message.

                String serverResponseMessage = conn.getResponseMessage();
                // Gets the HTTP response message, if any,
                // returned along with the response code from a server.

                Log.i("uploadFile", "Http Response is :"
                        + serverResponseMessage + ": " + serverResponseCode);
                // info log
                // example usage: whether successfully connected to a server

                if(serverResponseCode == 200){ // successful!

                    runOnUiThread(new Runnable() {
                        public void run() {
                            String msg = "File Upload Completed.\n\n See uploaded file here "
                                    + ": \n\n" + uploadFileName;
                            messageText.setText(msg);
                            Toast.makeText(MainActivity.this, "File Upload complete.",
                                    Toast.LENGTH_SHORT).show();
                            // Toast: small popup feedback provider
                        }
                    });
                }

                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException e) {
                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(MainActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server", "error: " + e.getMessage(), e);
                // send a error log message and log the exception
                // .getMessage() : get detailed error message
                // e: throwable (Error types)

            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("Got Exception : see logcat");
                        Toast.makeText(MainActivity.this, "Got Exception : see logcat",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server", "Exception :"
                        + e.getMessage(), e);
            }

            dialog.dismiss();
            return serverResponseCode;
        }
    }


}
