// Copyright (C) 2013 Corbin Champion
//
// This file is part of gnuroot.
//
// gnuroot is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 3 of the License, or (at
// your option) any later version.
//
// gnuroot is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with gnuroot. If not, see <http://www.gnu.org/licenses/>.


package champion.gnuroot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.CheckBox;

public class gnurootMain extends Activity {

	private ProgressDialog mPd_ring;
	private boolean mAlreadyStarted;
	private boolean mErrOcc = false;
	private Spinner mCreateSpinner;
	private Spinner mLaunchSpinner;
	private Spinner mDeleteSpinner;
	private Button mCreateButton;
	private Button mLaunchButton;
	private Button mDeleteButton;
	private CheckBox mFakeRootCheckBox;
	private String mCreateSpinnerArray[];
	private String mLaunchSpinnerArray[];
	private String mDeleteSpinnerArray[];
	private File mInstallDir;
	
	private static final int POPULATE_LISTS = 0;
	private static final int SEARCH_MARKET = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main); 
		mCreateSpinner = (Spinner)findViewById(R.id.createRootfsSpinner);
		mLaunchSpinner = (Spinner)findViewById(R.id.launchRootfsSpinner);
		mDeleteSpinner = (Spinner)findViewById(R.id.deleteRootfsSpinner);
		mCreateButton = (Button)findViewById(R.id.createRootfsButton);
		mLaunchButton = (Button)findViewById(R.id.launchRootfsButton);
		mDeleteButton = (Button)findViewById(R.id.deleteRootfsButton);
		mFakeRootCheckBox = (CheckBox)findViewById(R.id.fakeRootCheckBox);
		
		mCreateButton.setOnClickListener(new OnClickListener() {			 
			@Override
			public void onClick(View arg0) {
				//inform the user we are unpacking and this will take a while
				mPd_ring = ProgressDialog.show(gnurootMain.this, "Unpacking a rootfs", "This may take some time.",true);
				mPd_ring.setCancelable(false);
				Thread t = new Thread() { 
					public void run() {
						try {
							//determine the file(s) to unpack
							String rootfsName = mCreateSpinner.getSelectedItem().toString();
							if (rootfsName.equals("Search Play Store")) {
								Message msg = Message.obtain();
					            msg.what = SEARCH_MARKET;
					            mHandler.sendMessage(msg);
							} else {
								String path = Environment.getExternalStorageDirectory() + "/Android/obb/champion.gnuroot." + rootfsName + "/";
								File f = new File(path);         
								File file[] = f.listFiles();
								if (file != null) {
									for (int i=0; i < file.length; i++)
									{
										if (file[i].getName().startsWith("main.") && file[i].getName().contains("champion.gnuroot." + rootfsName)) {
											//unpack the main file
											exec(mInstallDir.getAbsolutePath() +"/support/busybox tar -C " + mInstallDir.getAbsolutePath() + "/roots/ -xzf " + file[i].getAbsolutePath());
										}
									}
									for (int i=0; i < file.length; i++)
									{
										if (file[i].getName().startsWith("patch.") && file[i].getName().contains("champion.gnuroot." + rootfsName)) {
											//unpack the patch file
											exec(mInstallDir.getAbsolutePath() +"/support/busybox tar -C " + mInstallDir.getAbsolutePath() + "/roots/ -xzf " + file[i].getAbsolutePath());
										}
									}
								}
							}
							//done
							mPd_ring.dismiss();
							//repopulate lists
							Message msg = Message.obtain();
				            msg.what = POPULATE_LISTS;
				            mHandler.sendMessage(msg);
						} catch (Exception e) {
							Log.e("LongToast", "", e);
						}
					}
				};
				t.start();
			}
		});
		mDeleteButton.setOnClickListener(new OnClickListener() {			 
			@Override
			public void onClick(View arg0) {
				//inform the user we are deleting a rootfs and this will take a while
				mPd_ring = ProgressDialog.show(gnurootMain.this, "Deleting a rootfs", "This may take some time.",true);
				mPd_ring.setCancelable(false);
				Thread t = new Thread() { 
					public void run() {
						try {
							//determine which rootfs to delete
							String rootfsName = mDeleteSpinner.getSelectedItem().toString();
							//delete rootfs
							exec(mInstallDir.getAbsolutePath() +"/support/busybox rm -rf " + mInstallDir.getAbsolutePath() + "/roots/" + rootfsName);	
							//done
							mPd_ring.dismiss();
							//repopulate lists
							Message msg = Message.obtain();
				            msg.what = POPULATE_LISTS;
				            mHandler.sendMessage(msg);
						} catch (Exception e) {
							Log.e("LongToast", "", e);
						}
					}
				};
				t.start();
			}
		});
		mLaunchButton.setOnClickListener(new OnClickListener() {			 
			@Override
			public void onClick(View arg0) {
				//determine which rootfs to launch
				String rootfsName = mLaunchSpinner.getSelectedItem().toString();
				String fakeRootString = "";
				if (mFakeRootCheckBox.isChecked()) {
					fakeRootString = " -0 ";
				}
				//launch the rootfs 
				Intent termIntent = new Intent(gnurootMain.this, jackpal.androidterm.RemoteInterface.class);
				termIntent.addCategory(Intent.CATEGORY_DEFAULT);
				termIntent.setAction("jackpal.androidterm.OPEN_NEW_WINDOW");
				///data/data/champion.gnuroot/minimal/usr/bin/proot-arm -r /data/data/champion.gnuroot/minimal -b /dev -b /proc -b /data -q /data/data/champion.gnuroot/minimal/usr/bin/qemu-arm-static -v -1
				//in the future launch code and necessary files to launch it, should be included in the rootfs
				if (android.os.Build.VERSION.SDK_INT >= 16) {
				     // only for jelly bean and after
					termIntent.putExtra("defaultTerm", mInstallDir.getAbsolutePath()+"/support/proot -r " + mInstallDir.getAbsolutePath()+"/roots/" + rootfsName + fakeRootString + " -b /dev -b /proc -b /data -b /mnt -b /:/host-rootfs -q " + mInstallDir.getAbsolutePath()+"/support/qemu -v -1");
				} else {
					termIntent.putExtra("defaultTerm", mInstallDir.getAbsolutePath()+"/support/proot -r " + mInstallDir.getAbsolutePath()+"/roots/" + rootfsName + fakeRootString + " -b /dev -b /proc -b /data -b /mnt -b /:/host-rootfs -v -1");
				}
				
				startActivity(termIntent);
			}
		});
		
		mInstallDir = getDir("install",0);

		mAlreadyStarted = false;
		
		ViewTreeObserver viewTreeObserver = mLaunchButton.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) { 
			viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					if (mAlreadyStarted == false) { 
						mAlreadyStarted = true;
						if (updateRequired("champion.gnuroot")) {
							mPd_ring = ProgressDialog.show(gnurootMain.this, "Unpacking GNURoot", "This won't take long.",true);
							mPd_ring.setCancelable(false);
							Thread t = new Thread() { 
								public void run() {
									try {
										File tempFile = new File(mInstallDir.getAbsolutePath()+"/roots");
										if (!tempFile.exists()) {
											tempFile.mkdir();
										}
										tempFile = new File(mInstallDir.getAbsolutePath()+"/support");
										if (!tempFile.exists()) {
											tempFile.mkdir();
										}
										copyFolder("/data/data/champion.gnuroot/lib/lib__busybox.so",mInstallDir.getAbsolutePath()+"/support/busybox");
										copyFolder("/data/data/champion.gnuroot/lib/lib__proot.so",mInstallDir.getAbsolutePath()+"/support/proot");
										copyFolder("/data/data/champion.gnuroot/lib/lib__qemu.so",mInstallDir.getAbsolutePath()+"/support/qemu");
										mPd_ring.dismiss();
									} catch (Exception e) {
										Log.e("LongToast", "", e);
									}
									createVersionFile("champion.gnuroot");
									//populate lists
									Message msg = Message.obtain();
						            msg.what = POPULATE_LISTS;
						            mHandler.sendMessage(msg);
								}
							};
							t.start();
						}
					}
				}
			});
		}
	}
	
	private void populateLists() {
		List<String> tempList = new ArrayList<String>();
		String path = Environment.getExternalStorageDirectory() + "/Android/obb/";
		File f = new File(path);         
		File file[] = f.listFiles();
		if (file != null) {
			for (int i=0; i < file.length; i++)
			{
				if (file[i].getName().contains("champion.gnuroot")) {
					String[] parts = file[i].getName().split("\\.");
					File subFile[] = file[i].listFiles();
					if ((parts.length == 3) && (subFile.length > 0)) {
						tempList.add(parts[2]);
					}
				}
			}
		}
		tempList.add("Search Play Store");
		mCreateSpinnerArray = new String[tempList.size()];
		tempList.toArray(mCreateSpinnerArray);
        ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item, mCreateSpinnerArray);
        mCreateSpinner.setAdapter(adapter);
        
		List<String> tempList2 = new ArrayList<String>();
		String path2 = mInstallDir+"/roots/";
		File f2 = new File(path2);         
		File file2[] = f2.listFiles();
		if (file2 != null) {
			for (int i=0; i < file2.length; i++)
			{
				tempList2.add(file2[i].getName());
			}
		}
		mDeleteSpinnerArray = new String[tempList2.size()];
		tempList2.toArray(mDeleteSpinnerArray);
        ArrayAdapter adapter2 = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item, mDeleteSpinnerArray);
        mDeleteSpinner.setAdapter(adapter2);
        
        mLaunchSpinnerArray = new String[tempList2.size()];
		tempList2.toArray(mLaunchSpinnerArray);
        ArrayAdapter adapter3 = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item, mLaunchSpinnerArray);
        mLaunchSpinner.setAdapter(adapter3);
	}

	@Override
	public void onResume() {
		super.onResume(); 
		populateLists();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{ 
		super.onConfigurationChanged(newConfig);
	}

	public void copyFolder(String srcStr, String destStr) throws IOException {
		File src = new File(srcStr);
		File dest = new File(destStr);
		copyFolder(src, dest);
	}

	public void copyFolder(File src, File dest) throws IOException{

		if(src.isDirectory()){

			//if directory not exists, create it
			if(!dest.exists()){
				dest.mkdir();
				System.out.println("Directory copied from " + src + "  to " + dest);
				exec("chmod 0777 " + dest.getAbsolutePath());
			}

			//list all the directory contents
			String files[] = src.list();

			for (String file : files) {
				//construct the src and dest file structure
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				//recursive copy
				copyFolder(srcFile,destFile);
			}

		}else{
			//if file, then copy it
			//Use bytes stream to support all file types
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest); 

			byte[] buffer = new byte[1024];

			int length;
			//copy the file content in bytes 
			while ((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}

			in.close();
			out.close();
			System.out.println("File copied from " + src + " to " + dest);
			exec("chmod 0777 " + dest.getAbsolutePath());
		}
	}

	private void exec(String command) {
		Runtime runtime = Runtime.getRuntime(); 
		Process process;
		try {
			process = runtime.exec(command);
			try {
				String str;
				process.waitFor();
				BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				while ((str = stdError.readLine()) != null) {
					Log.e("Exec",str);
					mErrOcc = true;
				}
				process.getInputStream().close(); 
				process.getOutputStream().close(); 
				process.getErrorStream().close(); 
			} catch (InterruptedException e) {
				mErrOcc = true;
			}
		} catch (IOException e1) {
			mErrOcc = true;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		finish();
	}

	private boolean updateRequired(String packageName) {
		String version;

		try {
			PackageInfo pi = getPackageManager().getPackageInfo(packageName, 0);
			version = pi.versionName;     // this is the line Eclipse complains
		}
		catch (PackageManager.NameNotFoundException e) {
			return false;
		}

		File versionFile = new File("/data/data/champion.gnuroot/"+packageName+"."+version);

		if (versionFile.exists()==false) {
			return true;
		} else {
			return false;
		}
	}

	private void createVersionFile(String packageName) {
		String version;

		try {
			PackageInfo pi = getPackageManager().getPackageInfo(packageName, 0);
			version = pi.versionName;     // this is the line Eclipse complains
		}
		catch (PackageManager.NameNotFoundException e) {
			version = "?";
		}

		File versionFile = new File("/data/data/champion.gnuroot/"+packageName+"."+version);

		if (versionFile.exists()==false) {
			try {
				versionFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private Handler mHandler = new Handler(){
	    @Override
	    public void handleMessage(Message msg) {
	        switch(msg.what){
	        	case POPULATE_LISTS:
	        		populateLists();
	        		break;
	        	case SEARCH_MARKET:
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("market://search?q=champion.gnuroot"));
					startActivity(intent);
					break;
	        }
	    }
	};
}