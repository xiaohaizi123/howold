package com.andy.howold;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

/**
 * 人脸识别类 完成对人脸识别,得到识别结果(json),通过Callback回调给调用者
 * 
 * @author andy
 * 
 */
public class FaceDetect
{

	/**
	 * 毁掉函数,如果成功,返回得到的json数据 如果失败 就抛出一个异常
	 * 
	 * @author andy
	 * 
	 */
	public interface Callback
	{
		/**
		 * 检测成功,调用此函数,返回json
		 * 
		 * @param jsonObject
		 */
		public void success(JSONObject jsonObject);

		/**
		 * 检测失败,调用此函数,返回异常
		 * 
		 * @param exception
		 */
		public void fail(FaceppParseException exception);
	}

	/**
	 * 人脸识别类的主要函数,检测人脸,
	 * 
	 * @param bitmap
	 *            图片
	 * @param callback
	 *            回调函数实例
	 */
	public void detect(final Bitmap bitmap, final Callback callback)
	{
		/**
		 * 主要原理就是通过我提交图片到face++服务器,他们分析,返回数据,本地解析数据//
		 * 
		 * 
		 */
		new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				// TODO Auto-generated method stub
				// http请求
				HttpRequests httpRequests = new HttpRequests(CONSTANTS.apikey, CONSTANTS.apisecret, true, true);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				float scale = Math.min(1, Math.min(600f / bitmap.getWidth(), 600f / bitmap.getHeight()));
				Matrix matrix = new Matrix();
				matrix.postScale(scale, scale);

				Bitmap imgSmall = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
				// Log.v(TAG, "imgSmall size : " + imgSmall.getWidth() + " " +
				// imgSmall.getHeight());

				imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, bos);
				// 要传过去字节数组,并且大小不能超过3M
				byte[] array = bos.toByteArray();
				PostParameters parameters = new PostParameters();
				parameters.setImg(array);
				try
				{
					JSONObject result = httpRequests.detectionDetect(parameters);
					if (callback != null)
					{
						callback.success(result);
						Log.i("json success", result.toString());
					}
				}
				catch (FaceppParseException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					if (callback != null)
					{
						callback.fail(new FaceppParseException("fail"));
					}
				}

			}
		}).start();

	}
}
