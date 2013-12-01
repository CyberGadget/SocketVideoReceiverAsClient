package com.kahlda.socketvideoreceiverasclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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
	private String mMessage;
	private String mEchoMessage;

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

		Button sendButton1 = (Button) findViewById(R.id.sendButton1);
		sendButton1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Button button = (Button) v;
				Toast.makeText(MainActivity.this, "Send 1", Toast.LENGTH_SHORT).show();
				new SendCommandTask().execute("Message 1\n");
			}
		});
		Button sendButton2 = (Button) findViewById(R.id.sendButton2);
		sendButton2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(MainActivity.this, "Send 2", Toast.LENGTH_SHORT).show();
				new SendCommandTask().execute("MESSAGE 2\n");				
			}
		});
		Button exitButton = (Button) findViewById(R.id.exitButton);
		exitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(MainActivity.this, "Exit", Toast.LENGTH_SHORT).show();
				new SendCommandTask().execute(EXIT_STRING);
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
	
	public class SendCommandTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			//Log.d(TAG, "SendFrameTask.doInBackground is executing.");
			if(mSendable){
				try {
					//Log.d(TAG, "Sending frame via mWriter.");
					mWriter.write(params[0]);
					mWriter.flush();
				} catch (IOException e) {
					Log.d(TAG, "Could not send via mWriter.");
				}
			}
			return null;
		}
		
//		@Override
//		protected void onPostExecute(Void result) {
//			// TODO Auto-generated method stub
//			super.onPostExecute(result);
//		}

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

			while (mConnectionFlag) {
				int bytesRead = 0, current = 0;
				byte[] byteArray = new byte[100000];
				int start, end, echoStart;

				try {
					do {
						bytesRead = mSocket.getInputStream().read(byteArray,
								current, (byteArray.length - current));
						mMessage = new String(byteArray,
								Charset.forName("ISO-8859-1"));
						//mEchoReceived = mMessage.startsWith("<<<ECHO>>>");
						echoStart = mMessage.indexOf("<<<ECHO>>>");
						start = mMessage.indexOf("<<<FRAME>>>");
						end = mMessage.indexOf("<<<END>>>");
						if (end < start) {
							end = mMessage.indexOf("<<<END>>>", end + 1);
						}
						if (bytesRead >= 0)
							current += bytesRead;
					//} while ((start == -1 || end == -1) && !mEchoReceived);
					} while ( (start == -1 || end == -1) && (echoStart == -1) );


					if (echoStart != -1) {
						int echoEnd = mMessage.indexOf("<<<STFU>>>", echoStart);
						mEchoMessage = mMessage.substring(echoStart + 10, echoEnd);
						mHandler.post(new Runnable() {
							public void run() {
								Toast.makeText(MainActivity.this,
										"Echo: " + mEchoMessage, Toast.LENGTH_SHORT)
										.show();
							}
						});
					} else {
						mFrameString = mMessage.substring(start + 11, end);
						mHandler.post(new Runnable() {
							public void run() {
								byte[] frame = mFrameString.getBytes(Charset
										.forName("ISO-8859-1"));
								ByteArrayInputStream bais = new ByteArrayInputStream(
										frame);
								Bitmap map = BitmapFactory.decodeStream(bais);
								mImageView.setImageBitmap(map);
							}
						});
					}
				} catch (StringIndexOutOfBoundsException e) {
					Log.d(TAG, "Unable to parse string.");
					Log.d(TAG, e.toString());
				}

				// while (true) {
				// mMessage = mReader.readLine(); // blocking
				// if (mMessage.length() > 3){
				// //Log.d(TAG, "Message received, Length = " +
				// mMessage.length());
				// Log.d(TAG, "Message: " + mMessage);
				// }
				// }
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
