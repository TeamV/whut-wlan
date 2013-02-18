package edu.whut.teamv.whutwlan;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;

@SuppressLint({ "ShowToast", "NewApi" })
public class MainActivity extends Activity {
	private EditText mPassword = null;
	private EditText mUsername = null;
	private String username = "";
	private String password = "";
	private String ip = "";
	private String mac="";
	private RedirectHandler rh = null;
	public static final String SwitchUrl = "http://12.130.132.30/";
	public static final String ActionUrl = "http://172.30.16.53/cgi-bin/srun_portal";

	private void onLoginError(){
		username="";
		password="";
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//4.0以后的版本中，下载不能再主线程中执行，所以增加以下的代码。
		if(VERSION.SDK_INT>=9){
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectDiskReads().detectDiskWrites().detectNetwork() // or .detectAll()// for// all// detectable// problems
					.penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectLeakedSqlLiteObjects().detectLeakedClosableObjects()
					.penaltyLog().penaltyDeath().build());
		}
		//防止程序302跳转
		rh = new RedirectHandler() {//不准跳转
			@Override
			public boolean isRedirectRequested(HttpResponse response,
					HttpContext context) {
				return false;
			}
			@Override
			public URI getLocationURI(HttpResponse response, HttpContext context)
					throws ProtocolException {
				return null;
			}
		};
		CheckBox mDisplay = (CheckBox) this.findViewById(R.id.chkDisplay);
		mPassword = (EditText) this.findViewById(R.id.txtPassword);
		mUsername = (EditText) this.findViewById(R.id.txtUsername);
		// 设置显示密码的功能
		mDisplay.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked)
					mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
				else
					mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
			}
		});
		// 设置按钮的功能，设置连接
		Button mConnect = (Button) this.findViewById(R.id.btnConnect);
		mConnect.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				username = mUsername.getText().toString();
				password = mPassword.getText().toString();
				if ("".equals(username)) {
					// TODO 弹不出来
					Toast.makeText(MainActivity.this, "请输入您的账号！",
							Toast.LENGTH_LONG);
					mUsername.requestFocus();
					onLoginError();
					return;
				}
				if ("".equals(password)) {
					mPassword.requestFocus();
					Toast.makeText(MainActivity.this, "请输入您的密码！",
							Toast.LENGTH_LONG);
					onLoginError();
					return;
				}
				// 1 开始请求分配的地址
				HttpGet g = new HttpGet(SwitchUrl);
				try {
					HttpClient c = new DefaultHttpClient();
					((DefaultHttpClient)c).setRedirectHandler(rh);
					HttpResponse r = c.execute(g);
					int code =r.getStatusLine().getStatusCode(); 
					switch (code) {
					case 302:// 正常，获得分配的ip
						Header h = r.getFirstHeader("Location");
						String s = h.getValue();
						//目前手动设置这个值
//						s = "Location: http://172.30.16.58/index_client_3.html?cmd=login&switchip=172.30.12.244&mac=00:1e:64:7d:2a:6a&ip=10.136.254.33&essid=WHUT-WLAN&url=http://12.130.132.30/";
						String[] ss = s.split("&|=");
						List<NameValuePair> params = new ArrayList<NameValuePair>();
						params.add(new BasicNameValuePair("action","login"));
						params.add(new BasicNameValuePair("username",username));
						params.add(new BasicNameValuePair("password",password));
						params.add(new BasicNameValuePair("drop","0"));
						params.add(new BasicNameValuePair("type","2"));
						params.add(new BasicNameValuePair("n","23"));
						params.add(new BasicNameValuePair("ip","0"));
						params.add(new BasicNameValuePair("mbytes","0"));
						params.add(new BasicNameValuePair("minutes","0"));
						params.add(new BasicNameValuePair("ac_id","3"));
						// 提取常用的信息
						int i = 1;
						while (i<ss.length){
							if("switchip".equals(ss[i])){
								ip = ss[i+1];
								params.add(new BasicNameValuePair("nas_ip",ip));
							}else if("mac".equals(ss[i])){
								mac = ss[i+1];
								params.add(new BasicNameValuePair("mac",mac));
							}
							i++;
						}
						//post登录请求
						HttpPost hp = new HttpPost(ActionUrl);
						hp.setHeader("User-Agent", "my session");
						hp.setHeader("Host", "172.30.16.58");
						HttpEntity he = new UrlEncodedFormEntity(params);
						hp.setEntity(he);
						r = c.execute(hp);
						switch(r.getStatusLine().getStatusCode()){
						case 200://有两种情况
							String sRet = EntityUtils.toString(r.getEntity(), HTTP.UTF_8);
							char cc = sRet.charAt(0);
							if (cc == '<'){
								new AlertDialog.Builder(MainActivity.this).setTitle("错误提示")
								.setMessage("登录成功").show();
							}else if (cc=='e'){
								new AlertDialog.Builder(MainActivity.this).setTitle("错误提示")
								.setMessage("密码错误").show();
								onLoginError();
							}
							break;
						case 500://账户不存在
							new AlertDialog.Builder(MainActivity.this).setTitle("错误提示")
							.setMessage("账户不存在\r\n服务器挂了").show();
							onLoginError();
							break;
						default:
							new AlertDialog.Builder(MainActivity.this).setTitle("错误提示")
							.setMessage("未知错误,请联系TeamV").show();
							onLoginError();
							break;
						}
						break;
					case 403:// 已经成功登陆
						new AlertDialog.Builder(MainActivity.this)
								.setTitle("登陆请求失败")
								.setMessage("以我的经验来看,可能您的账户已经登录了").show();
						break;
					default:
						new AlertDialog.Builder(MainActivity.this)
								.setTitle("登陆请求失败").setMessage("未知错误,可能服务器挂了")
								.show();
						onLoginError();
						break;
					}
				 }catch(HttpHostConnectException e){
					 e.printStackTrace();
					 onLoginError();
					 Toast.makeText(MainActivity.this, "网络连接有问题\r\n请确认已经连接上WHUT-WLAN",
								Toast.LENGTH_LONG);
				} catch (Exception e) {
					e.printStackTrace();
					onLoginError();
					Toast.makeText(MainActivity.this, "程序出了问题\r\n请重启程序。",
							Toast.LENGTH_LONG);
				}
			}
		});// end 连接按钮
			// 设置断开连接按钮的功能
		Button mDisconnect = (Button) this.findViewById(R.id.btnDisconnect);
		mDisconnect.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				if("".equals(username) || "".equals(password)){
					new AlertDialog.Builder(MainActivity.this).setTitle("错误提示")
					.setMessage("必须先成功连接才能再断开").show();
					return;
				}
				HttpPost hp = new HttpPost(ActionUrl);
				hp.setHeader("User-Agent", "my session");
				hp.setHeader("Host", "172.30.16.58");
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("action","logout"));
				params.add(new BasicNameValuePair("username",username));
//				params.add(new BasicNameValuePair("password",password));
				params.add(new BasicNameValuePair("ac_id","3"));
				params.add(new BasicNameValuePair("type","2"));
				params.add(new BasicNameValuePair("mac",mac));
				params.add(new BasicNameValuePair("nas_ip",ip));
				try{
					HttpEntity he = new UrlEncodedFormEntity(params);
					hp.setEntity(he);
					HttpClient c = new DefaultHttpClient();
					((DefaultHttpClient)c).setRedirectHandler(rh);
					HttpResponse r = c.execute(hp);
					int code = r.getStatusLine().getStatusCode();
					if (code == 500){
						Toast.makeText(MainActivity.this, "可能断开连接成功，服务器端返回500了",
								Toast.LENGTH_LONG);
					}
				}catch(Exception e){
					e.printStackTrace();
					Toast.makeText(MainActivity.this, "断开连接失败",
							Toast.LENGTH_LONG);
				}
			}
		});// end 断开连接按钮
	}
}
