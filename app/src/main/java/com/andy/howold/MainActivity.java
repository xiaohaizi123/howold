package com.andy.howold;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.Platform.ShareParams;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;

import com.andy.howold.FaceDetect.Callback;
import com.andy.utils.L;
import com.andy.utils.toastMgr;
import com.facepp.error.FaceppParseException;

public class MainActivity extends Activity implements OnClickListener, PlatformActionListener
{

	private static final int PICK_PHOTO_CODE = 0X110;
	protected static final int DETECT_SUCCESS = 0X111;
	protected static final int DETECT_FAIL = 0X112;
	private Button btn_getImage;
	private Button btn_detect;
	private Button btn_share;
	private TextView tv_state;
	private ImageView mPhoto;//
	private Bitmap mPhotoImage;// 从本地得到的图片
	private Bitmap mPhotoImageDetected;// 检测之后,重绘的图片
	private String mCurrentPhotoString;

	private Context mContext;

	// 在bitmap上画人脸框
	private Canvas mCanvas;
	private Paint mPaint;

	// 检测进度条
	private ProgressDialog pdDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContext = this;

		intiView();

		initEvents();

	}

	/**
	 */
	private void initEvents()
	{
		btn_getImage.setOnClickListener(this);
		btn_detect.setOnClickListener(this);
		btn_share.setOnClickListener(this);

	}

	/**
	 */
	private void intiView()
	{
		btn_getImage = (Button) findViewById(R.id.getImage);
		btn_detect = (Button) findViewById(R.id.detect);
		tv_state = (TextView) findViewById(R.id.tv_state);
		mPhoto = (ImageView) findViewById(R.id.imageView1);
		btn_share = (Button) findViewById(R.id.btn_share);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// TODO Auto-generated method stub
		if (resultCode == RESULT_OK)
		{
			if (requestCode == PICK_PHOTO_CODE)
			{
				if (data != null)
				{
					Uri uri = data.getData();

					try
					{
						mPhotoImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
						mPhotoImageDetected = mPhotoImage;
					}
					catch (FileNotFoundException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					catch (IOException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					// ContentResolver cr = this.getContentResolver();
					// try
					// {
					// mPhotoImage = BitmapFactory.decodeStream(cr
					// .openInputStream(uri));
					//
					// }
					// catch (FileNotFoundException e)
					// {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// }

					// Cursor cursor = getContentResolver().query(uri, null, null, null, null);
					//
					// cursor.moveToFirst();
					//
					// int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);

					// mCurrentPhotoString = cursor.getString(index);
					mCurrentPhotoString = "/storage/sdcard0/DCIM/Screenshots/Screenshot_2015-05-21-10-42-22.png";

					// cursor.close();

					// resizePhoto();

					mPhoto.setImageBitmap(mPhotoImage);
				}
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void resizePhoto()
	{
		// TODO Auto-generated method stub
		mPhotoImage = BitmapFactory.decodeFile(mCurrentPhotoString);
	}

	/**
	 * 
	 * @param arg0
	 */
	@Override
	public void onClick(View v)
	{
		// TODO Auto-generated method stub
		switch (v.getId())
		{
		case R.id.getImage:
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			// Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, PICK_PHOTO_CODE);
			break;
		case R.id.detect:
			pdDialog = new ProgressDialog(mContext);
			pdDialog.setTitle("正在检测....");
			pdDialog.setCancelable(false);// 不可取消
			pdDialog.setCanceledOnTouchOutside(false);
			pdDialog.show();

			FaceDetect faceDetect = new FaceDetect();
			faceDetect.detect(mPhotoImage, new Callback()
			{

				@Override
				public void success(JSONObject jsonObject)
				{
					// TODO Auto-generated method stub
					Message msg = new Message();
					msg.what = DETECT_SUCCESS;
					msg.obj = jsonObject;
					handler.sendMessage(msg);
				}

				@Override
				public void fail(FaceppParseException exception)
				{
					// TODO Auto-generated method stub
					Message msg = new Message();
					msg.what = DETECT_SUCCESS;
					handler.sendMessage(msg);
				}
			});
			// showShare();

			break;
		case R.id.btn_share:
			// shareDirect();
			showShare();
			break;
		default:
			break;
		}
	}

	Handler handler = new Handler()
	{
		public void handleMessage(android.os.Message msg)
		{
			if (msg.what == DETECT_SUCCESS)
			{
				pdDialog.dismiss();

				JSONObject resultJson = (JSONObject) msg.obj;
				L.i(resultJson.toString());
				tv_state.setText("click to detect===>");
				prepareBitmap(resultJson);
				// mPhotoImage是原始图片,不做修改 mPhotoImageDetected是修改的图片
				// 因为如果用户再一次点击检测,就会在原始图片的基础上画一次
				mPhoto.setImageBitmap(mPhotoImageDetected);
				toastMgr.builder.displayCenter("检测成功", 1);
			}
			else if (msg.what == DETECT_FAIL)
			{
				pdDialog.dismiss();
				toastMgr.builder.display("检测失败", 1);
			}
			else if (msg.arg1 == 1)
			{
				Platform plat = (Platform) msg.obj;
				String text = plat.getName() + "completed at";
				toastMgr.builder.display(text, 1);
			}
		}

	};

	private void prepareBitmap(JSONObject resultJson)
	{
		// TODO Auto-generated method stub
		// 解析json数据
		int age = 0;
		String gender;
		float centerX;
		float centerY;
		float centerW;
		float centerH;

		Bitmap bm = Bitmap.createBitmap(mPhotoImage.getWidth(), mPhotoImage.getHeight(), mPhotoImage.getConfig());
		mCanvas = new Canvas(bm);
		mPaint = new Paint();
		mPaint.setColor(Color.WHITE);
		mPaint.setStrokeWidth(3);
		// 在bm上画mPhotoImage
		mCanvas.drawBitmap(mPhotoImage, 0, 0, null);

		try
		{
			JSONArray faces = resultJson.getJSONArray("face");
			int faceCount = faces.length();
			L.i("face count = " + faceCount);
			for (int i = 0; i < faces.length(); i++)
			{
				JSONObject face = faces.getJSONObject(i);
				age = (Integer) face.getJSONObject("attribute").getJSONObject("age").get("value");
				gender = (String) face.getJSONObject("attribute").getJSONObject("gender").get("value");
				centerX = (float) face.getJSONObject("position").getJSONObject("center").getDouble("x");
				centerY = (float) face.getJSONObject("position").getJSONObject("center").getDouble("y");
				centerW = (float) face.getJSONObject("position").getDouble("width");
				centerH = (float) face.getJSONObject("position").getDouble("height");

				// 根据centerX 和centerY 拿到这个点对应的在图片中真是位置, 也就是dp
				centerX = centerX * mPhotoImage.getWidth() / 100;
				centerY = centerY * mPhotoImage.getHeight() / 100;
				centerW = centerW * mPhotoImage.getWidth() / 100;
				centerH = centerH * mPhotoImage.getHeight() / 100;

				// 同样是在bm上面划线
				// 画人脸box
				// 上面一条横线
				mCanvas.drawLine(centerX - centerW / 2, centerY - centerH / 2, centerX + centerW / 2, centerY - centerH / 2, mPaint);
				// 左边竖线
				mCanvas.drawLine(centerX - centerW / 2, centerY - centerH / 2, centerX - centerW / 2, centerY + centerH / 2, mPaint);
				// 右边竖线
				mCanvas.drawLine(centerX + centerW / 2, centerY - centerH / 2, centerX + centerW / 2, centerY + centerH / 2, mPaint);

				mCanvas.drawLine(centerX - centerW / 2, centerY + centerH / 2, centerX + centerW / 2, centerY + centerH / 2, mPaint);

				// 在这里画性别跟年龄
				Bitmap ageBitmap = buildAgeBitmap(age, "Male".equals(gender));

				// 比例
				int ageWidth = ageBitmap.getWidth();
				int ageHeight = ageBitmap.getHeight();
				int bmWidth = bm.getWidth();
				int bmHeight = bm.getHeight();
				int mPhotoWidth = mPhoto.getWidth();
				int mPhotoHeight = mPhoto.getHeight();
				L.i("bmWidth = " + bmWidth + "  bmHeight = " + bmHeight + "  mPhotoWidth = " + mPhotoWidth + "  mPhotoHeight" + mPhotoHeight);
				if (bm.getWidth() <= mPhoto.getWidth() && bm.getHeight() <= mPhoto.getHeight())
				{
					float ratio = Math.max(bm.getWidth() * 1.0f / mPhoto.getWidth(), bm.getHeight() * 1.0f / mPhoto.getHeight());
					ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int) (ageWidth * ratio), (int) (ageHeight * ratio), false);
				}
				mCanvas.drawBitmap(ageBitmap, centerX - ageWidth / 2, centerY - centerH / 2 - ageBitmap.getHeight(), null);

			}
			mPhotoImageDetected = bm;
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			L.i(e.toString());
		}

	}

	/**
	 * 
	 * @param age
	 * @param equals
	 * @return
	 */
	private Bitmap buildAgeBitmap(int age, boolean isMale)
	{
		// TODO Auto-generated method stub
		TextView textView = (TextView) findViewById(R.id.id_age_and_gender);
		textView.setText(age + "");
		// 男的
		if (isMale)
		{
			textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);

		}
		else
		{
			textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
		}
		textView.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(textView.getDrawingCache());
		textView.destroyDrawingCache();

		return bitmap;
	};

	/**
	 * 
	 */
	private void showShare()
	{

		ShareSDK.initSDK(this);
		OnekeyShare oks = new OnekeyShare();
		// 关闭sso授权
		oks.disableSSOWhenAuthorize();

		// title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
		oks.setTitle(getString(R.string.share));
		// titleUrl是标题的网络链接，仅在人人网和QQ空间使用
		oks.setTitleUrl("http://sharesdk.cn");
		// text是分享文本，所有平台都需要这个字段
		oks.setText("我是分享文本");
		// imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
		oks.setImagePath(mCurrentPhotoString);// 确保SDcard下面存在此张图片
		// url仅在微信（包括好友和朋友圈）中使用
		oks.setUrl("http://sharesdk.cn");
		// comment是我对这条分享的评论，仅在人人网和QQ空间使用
		oks.setComment("我是测试评论文本");
		// site是分享此内容的网站名称，仅在QQ空间使用
		oks.setSite(getString(R.string.app_name));
		// siteUrl是分享此内容的网站地址，仅在QQ空间使用
		oks.setSiteUrl("http://sharesdk.cn");

		// 启动分享GUI
		oks.show(this);
	}

	private void shareDirect()
	{
		ShareSDK.initSDK(mContext);
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("BypassApproval", false);
		ShareSDK.registerPlatform(LaiwangCustomize.class);
		ShareSDK.setPlatformDevInfo("Wechat", map);
		ShareSDK.setPlatformDevInfo("WechatMoments", map);
		Platform plat = null;
		ShareParams sp = getShareParams();
		plat = ShareSDK.getPlatform("WecharMoments");
		plat.setPlatformActionListener(this);
		plat.share(sp);
	}

	private ShareParams getShareParams()
	{
		// TODO Auto-generated method stub
		ShareParams sp = new ShareParams();
		sp.setTitle("这是title");
		sp.setText("这是text");
		sp.setShareType(Platform.SHARE_TEXT);
		sp.setShareType(Platform.SHARE_IMAGE);
		sp.setImageData(mPhotoImageDetected);
		return sp;
	}

	// /////////////////////下面这部分是ShareSDK的////////////////////////////////////////////////////
	@Override
	public void onCancel(Platform plat, int actioin)
	{
		// TODO Auto-generated method stub
		Message msg = new Message();
		msg.arg1 = 3;// 取消
		msg.arg2 = actioin;
		msg.obj = plat;
		handler.sendMessage(msg);

	}

	@Override
	public void onComplete(Platform plat, int actioin, HashMap<String, Object> arg2)
	{
		// TODO Auto-generated method stub
		Message msg = new Message();
		msg.arg1 = 1;// 成功
		msg.arg2 = actioin;
		msg.obj = plat;
		handler.sendMessage(msg);
	}

	@Override
	public void onError(Platform plat, int actioin, Throwable t)
	{
		// TODO Auto-generated method stub

	}
	// ////////////////////上面这部分是ShareSDK的/////////////////////////////////////////////////////
}
