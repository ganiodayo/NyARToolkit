/* 
 * PROJECT: NyARToolkit JOGL sample program.
 * --------------------------------------------------------------------------------
 * The MIT License
 * Copyright (c) 2008-2011 nyatla
 * airmail(at)ebony.plala.or.jp
 * http://nyatla.jp/nyartoolkit/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 */
package jp.nyatla.nyartoolkit.rpf.sample;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.Buffer;
import javax.media.opengl.*;

import jp.nyatla.nyartoolkit.NyARException;
import jp.nyatla.nyartoolkit.core.param.NyARParam;
import jp.nyatla.nyartoolkit.core.types.NyARIntPoint2d;
import jp.nyatla.nyartoolkit.core.types.matrix.NyARDoubleMatrix44;
import jp.nyatla.nyartoolkit.rpf.mklib.ASyncIdMarkerTable;
import jp.nyatla.nyartoolkit.rpf.mklib.ASyncIdMarkerTable.IResultListener;
import jp.nyatla.nyartoolkit.rpf.reality.nyartk.NyARRealityTarget;
import jp.nyatla.nyartoolkit.rpf.reality.nyartk.NyARRealityTargetList;
import jp.nyatla.nyartoolkit.rpf.realitysource.nyartk.NyARRealitySource_Jmf;
import jp.nyatla.nyartoolkit.jmf.utils.JmfCaptureDevice;
import jp.nyatla.nyartoolkit.jmf.utils.JmfCaptureDeviceList;
import jp.nyatla.nyartoolkit.jmf.utils.JmfCaptureListener;
import jp.nyatla.nyartoolkit.jogl.utils.NyARGLDrawUtil;
import jp.nyatla.nyartoolkit.rpf.reality.nyartk.gl.NyARRealityGl;

import com.sun.opengl.util.Animator;

/**
 * NyARRealityシステムのサンプル。
 * このプログラムは、非同期マーカー認識の実験プログラムです。
 * リアルタイムにマーカ一致を行わずに、外部プログラムやサーバなどで非同期にマーカ認識処理を行うテンプレートになります。
 * 
 * このプログラムでは、IDマーカの非同期認識（別スレッドで認識）を行い、認識が成功したらその上に立方体を表示します。
 * 
 * マーカには、IDマーカを使ってください。
 * @author nyatla
 *
 */
public class Test_NyARRealityGl_AsyncIdMarker implements GLEventListener, JmfCaptureListener,IResultListener
{

	private final static int SCREEN_X = 320;
	private final static int SCREEN_Y = 240;

	private Animator _animator;
	private JmfCaptureDevice _capture;

	private GL _gl;

	private Object _sync_object=new Object();

	NyARRealityGl _reality;
	NyARRealitySource_Jmf _src;
	ASyncIdMarkerTable _mklib;

	public Test_NyARRealityGl_AsyncIdMarker(NyARParam i_param) throws NyARException
	{
		Frame frame = new Frame("NyARToolkit+RPF["+this.getClass().getName()+"]");
		
		// キャプチャの準備
		JmfCaptureDeviceList devlist = new JmfCaptureDeviceList();
		this._capture = devlist.getDevice(0);
		if (!this._capture.setCaptureFormat(SCREEN_X, SCREEN_Y, 30.0f)) {
			throw new NyARException();
		}
		this._capture.setOnCapture(this);
		//Realityの構築
		i_param.changeScreenSize(SCREEN_X, SCREEN_Y);	
		//キャプチャ画像と互換性のあるRealitySourceを構築
//		this._src=new NyARRealitySource_Jmf(this._capture.getCaptureFormat(),i_param.getDistortionFactor(),2,100);
		this._src=new NyARRealitySource_Jmf(this._capture.getCaptureFormat(),null,2,100);
		//OpenGL互換のRealityを構築		
		this._reality=new NyARRealityGl(i_param.getPerspectiveProjectionMatrix(),i_param.getScreenSize(),10,10000,3,3);
		//非同期マーカライブラリ(NyId)の構築
		this._mklib= new ASyncIdMarkerTable(this);
				
		// 3Dを描画するコンポーネント
		GLCanvas canvas = new GLCanvas();
		frame.add(canvas);
		canvas.addGLEventListener(this);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});

		frame.setVisible(true);
		Insets ins = frame.getInsets();
		frame.setSize(SCREEN_X + ins.left + ins.right, SCREEN_Y + ins.top + ins.bottom);
		canvas.setBounds(ins.left, ins.top, SCREEN_X, SCREEN_Y);
	}

	public void init(GLAutoDrawable drawable)
	{
		this._gl = drawable.getGL();
		this._gl.glEnable(GL.GL_DEPTH_TEST);
		this._gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		NyARGLDrawUtil.setFontStyle("SansSerif",Font.BOLD,24);
		// NyARToolkitの準備
		try {
			// キャプチャ開始
			_capture.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this._animator = new Animator(drawable);
		this._animator.start();
		return;
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
		_gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		_gl.glViewport(0, 0, width, height);

		// 視体積の設定
		_gl.glMatrixMode(GL.GL_PROJECTION);
		_gl.glLoadIdentity();
		// 見る位置
		_gl.glMatrixMode(GL.GL_MODELVIEW);
		_gl.glLoadIdentity();
	}

	public void display(GLAutoDrawable drawable)
	{
		//RealitySourceにデータが処理する。
		if(!this._src.isReady())
		{
			return;
		}
		
		// 背景を書く
		this._gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT); // Clear the buffers for new frame.
		try{
			synchronized(this._sync_object){
				
				this._reality.glDrawRealitySource(this._gl,this._src);
				// Projection transformation.
				this._gl.glMatrixMode(GL.GL_PROJECTION);
				this._reality.glLoadCameraFrustum(this._gl);
				//ターゲットリストを走査して、画面に内容を反映
				NyARRealityTargetList tl=this._reality.refTargetList();
				for(int i=tl.getLength()-1;i>=0;i--){
					NyARRealityTarget t=tl.getItem(i);
					switch(t.getTargetType())
					{
					case NyARRealityTarget.RT_KNOWN:
						//立方体の描画
						this._gl.glMatrixMode(GL.GL_MODELVIEW);
						this._gl.glLoadIdentity();
						NyARDoubleMatrix44 m=t.refTransformMatrix();
						this._reality.glLoadModelViewMatrix(this._gl,m);
						_gl.glPushMatrix(); // Save world coordinate system.
						//_gl.glRotatef(90,0.0f,0.0f,1.0f); // Place base of cube on marker surface.
						
						_gl.glTranslatef(0,0,10f); // Place base of cube on marker surface.
						_gl.glDisable(GL.GL_LIGHTING); // Just use colours.
						NyARGLDrawUtil.drawColorCube(this._gl,20f);
						_gl.glPopMatrix(); // Restore world coordinate system.
						//マーカ情報の描画
						_gl.glPushMatrix(); // Save world coordinate system.
						this._reality.glLoadModelViewMatrix(this._gl,m);
						_gl.glTranslatef(-30,0,40f); // Place base of cube on marker surface.
						_gl.glRotatef(90,1.0f,0.0f,0.0f); // Place base of cube on marker surface.
						//マーカ情報の表示
						NyARGLDrawUtil.setFontColor(t.getGrabbRate()<50?Color.RED:Color.BLUE);
						NyARGLDrawUtil.drawText("ID:"+(Long)(t.tag)+" GRUB:"+t._grab_rate+"%",0.5f);
						_gl.glPopMatrix();
						
						break;
					case NyARRealityTarget.RT_UNKNOWN:
						NyARGLDrawUtil.beginScreenCoordinateSystem(this._gl,SCREEN_X,SCREEN_Y,false);
						NyARGLDrawUtil.setFontColor(t.getGrabbRate()<50?Color.RED:Color.BLUE);
						NyARIntPoint2d cp=new NyARIntPoint2d();
						t.getTargetCenter(cp);
						_gl.glTranslated(cp.x,SCREEN_Y-cp.y,1);
						NyARGLDrawUtil.drawText("now matching marker.",1f);
						NyARGLDrawUtil.endScreenCoordinateSystem(this._gl);						
						break;
					}
				}
			}
			Thread.sleep(1);// タスク実行権限を一旦渡す
		}catch(Exception e){
			e.printStackTrace();
		}

	}
	public void OnDetect(boolean i_result,long i_serial,int i_dir,double i_width,long i_id)
	{
		try{
		//Realityを触るのでロック
		synchronized (this._sync_object)
		{
			NyARRealityTarget t=this._reality.refTargetList().getItemBySerial(i_serial);
			if(t==null){
				return;
			}
			if(t.getTargetType()!=NyARRealityTarget.RT_UNKNOWN){
				return;
			}
			if(i_result){
				this._reality.changeTargetToKnown(t, i_dir, i_width);
				t.tag=new Long(i_id);
			}else{
				this._reality.changeTargetToDead(t);
			}
		}
		}catch(Exception e){
			//握りつぶす
			e.printStackTrace();
		}
	}

	/**
	 * カメラのキャプチャした画像を非同期に受け取る関数。
	 * 画像を受け取ると、同期を取ってRealityを1サイクル進めます。
	 */
	public void onUpdateBuffer(Buffer i_buffer)
	{
		try {
			synchronized (this._sync_object)
			{
				this._src.setImage(i_buffer);
				this._reality.progress(this._src);
				//UnknownTargetを1個取得して、遷移を試す。
				NyARRealityTarget t=this._reality.selectSingleUnknownTarget();
				if(t==null){
					return;
				}
				//tagに何か入ってたらなにかやっている。
				if(t.tag!=null){
					return;
				}
				this._mklib.requestAsyncMarkerDetect(this._reality,this._src,t);
				t.tag=new Long(-1);//-1は認識中ということで。
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
	{
	}
	
	private final static String PARAM_FILE = "../../Data/camera_para.dat";

	public static void main(String[] args)
	{
		try {
			NyARParam param = new NyARParam();
			param.loadARParamFromFile(PARAM_FILE);
			new Test_NyARRealityGl_AsyncIdMarker(param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

}
