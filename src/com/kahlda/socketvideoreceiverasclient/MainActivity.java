package com.kahlda.socketvideoreceiverasclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity implements Runnable {
	
	private static final String EXIT_STRING = "!!EXIT";
	private static final String HOST = "137.112.229.244";
	private static final int PORT = 8080;
	private static final String TAG = "VR";

	private boolean mSendable;
	private boolean mConnectionFlag;

	private Socket mSocket;
	private BufferedReader mReader;
	private BufferedWriter mWriter;
	
	private String mFrameString;
	
	private ImageView mImageView;
	
	volatile Thread mRunner;
	Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mSendable = false;
		mConnectionFlag = false;

		mImageView = (ImageView) findViewById(R.id.stream_display);
		
        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
                Toast.makeText(MainActivity.this, "Send", Toast.LENGTH_SHORT).show();
            }
        });
        Button exitButton = (Button) findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Exit", Toast.LENGTH_SHORT).show();
                mConnectionFlag = false;
            }
        });
        Button connButton = (Button) findViewById(R.id.connButton);
        connButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Connect", Toast.LENGTH_SHORT).show();
                mConnectionFlag = true;
                if (mRunner == null) {
        			mRunner = new Thread(MainActivity.this);
        			mRunner.start();
        		} else if (!mRunner.isAlive()) {
        			mRunner = new Thread(MainActivity.this);
        			mRunner.start();
        		} else {
        			Log.d(TAG, "Cannot start thread. Thread is created and alive.");
        		}
            }
        });
        

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void run() {
		try {
			mSocket = new Socket(HOST, PORT);
			mReader = new BufferedReader(new InputStreamReader(
					mSocket.getInputStream()));
			mWriter = new BufferedWriter(new OutputStreamWriter(
					mSocket.getOutputStream()));
			mSendable = true;
			
			while (mConnectionFlag){
				int bytesRead = 0, current = 0;
				byte[] byteArray = new byte[100000];
				int start, end;
				String message;
				
				try{
					do {
						bytesRead = mSocket.getInputStream().read(byteArray, current, (byteArray.length - current));
						message = new String(byteArray, Charset.forName("ISO-8859-1"));
						start = message.indexOf("<<<FRAME>>>");
						end = message.indexOf("<<<END>>>");
						if (end < start) {
							end = message.indexOf("<<<END>>>", end+1);
						}
						if(bytesRead >= 0) current += bytesRead;
					} while (start == -1 || end == -1);
					
					mFrameString = message.substring(start+11, end);
					mHandler.post(new Runnable() {
						public void run() {
							
							byte[] frame = mFrameString.getBytes(Charset.forName("ISO-8859-1"));
							ByteArrayInputStream bais = new ByteArrayInputStream(frame);
							Bitmap map = BitmapFactory.decodeStream(bais);
							mImageView.setImageBitmap(map);
						}
					});
				} catch (StringIndexOutOfBoundsException e) {
					Log.d(TAG, "Unable to parse string.");
					Log.d(TAG, e.toString());
				}
				
//				while (true) {
//					mMessage = mReader.readLine(); // blocking
//					if (mMessage.length() > 3){
//						//Log.d(TAG, "Message received, Length = " + mMessage.length());
//						Log.d(TAG, "Message: " + mMessage);
//					}
//				}
			}
			
			mSocket.close();
			mReader.close();
			mWriter.close();
			mSendable = false;
			
		} catch (IOException e) {
			try {
				if (mWriter != null) {
					mWriter.close();
				}

				if (mReader != null) {
					mReader.close();
				}

				if (mSocket != null) {
					mSocket.close();
				}

			} catch (IOException ex) {
				Log.e(ex.getClass().getName(), ex.getMessage());
			}

			Log.e(e.getClass().getName(), e.getMessage());
		}
		
	}

}
