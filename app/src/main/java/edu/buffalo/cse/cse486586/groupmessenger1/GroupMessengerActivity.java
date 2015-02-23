package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import  java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.FileOutputStream;
import java.net.UnknownHostException;

import android.net.Uri;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    public static final String[] PORTS = {"11108", "11112", "11116", "11120", "11124"};
    public static final Integer SERVER_SOCKET = 10000;
    public static final String CLASS_NAME = GroupMessengerActivity.class.getSimpleName();
    public static Integer count = 0;
    public final String providerUri = "edu.buffalo.cse.cse486586.groupmessenger1.provider";
    public final String content = "content";
    public static Uri uri;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private Uri buildProviderURI(String content, String uri) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(uri);
        uriBuilder.scheme(content);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        //code starts
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        uri = buildProviderURI(content, providerUri);

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_SOCKET);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException e) {
            Log.e(CLASS_NAME, e.getMessage());
        }
        //code ends
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView v1 = (TextView)findViewById(R.id.editText1);
                String enteredMsg = v1.getText().toString();
                v1.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, enteredMsg);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... params) {
            ContentResolver myDbResolver = null;
            final String METHOD_NAME = " :doInBackground ";
            final String CLASS_NAME = ServerTask.class.getName();
            ServerSocket myServerSocket = params[0];
            Socket mySocket = null;
            try {
                while(true) {
                    if((mySocket=myServerSocket.accept())!=null) {
                        BufferedReader myBufferedReader = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
                        String myReceivedMsg = myBufferedReader.readLine();
                        publishProgress(myReceivedMsg);
                        myDbResolver = getContentResolver();
                        ContentValues myValues = new ContentValues();
                        myValues.put(KEY_FIELD, count.toString());
                        myValues.put(VALUE_FIELD, myReceivedMsg);
                        myDbResolver.insert(uri, myValues);
                        myBufferedReader.close();
                        count++;
                    }
                }
            }
            catch(IOException e) {
                Log.e(CLASS_NAME+METHOD_NAME, e.getMessage());
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            final String METHOD_NAME = " :onProgressUpdate";
            final String CLASS_NAME = ServerTask.class.getName();
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\n");

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(CLASS_NAME+METHOD_NAME, e.getMessage());
            }

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            final String CLASS_NAME = ServerTask.class.getName();
            final String METHOD_NAME = " :doInBackground";
            String myMessage = params[0];
            Socket mySocket = null;
            try {
                InetAddress myIpAddress = InetAddress.getByAddress(new byte[]{10, 0, 2, 2});
                for(String myPortString: PORTS) {
                   mySocket = new Socket(myIpAddress, Integer.parseInt(myPortString));
                   //code starts
                   OutputStream myOutputStream = mySocket.getOutputStream();
                   myOutputStream.write(myMessage.getBytes());
                   myOutputStream.close();
                   //code ends
                   mySocket.close();
                }
            }catch (UnknownHostException e) {
                Log.e(CLASS_NAME+METHOD_NAME, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(CLASS_NAME+METHOD_NAME, "ClientTask socket IOException");
            }
            return null;
        }
    }

}
