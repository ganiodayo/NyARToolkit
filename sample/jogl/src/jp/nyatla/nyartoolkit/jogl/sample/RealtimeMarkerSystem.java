package jp.nyatla.nyartoolkit.jogl.sample;

import java.awt.event.*;
import java.awt.*;
import javax.media.opengl.*;

import com.sun.opengl.util.*;
import jp.nyatla.nyartoolkit.*;
import jp.nyatla.nyartoolkit.core.param.*;
import jp.nyatla.nyartoolkit.core.types.NyARIntSize;
import jp.nyatla.nyartoolkit.jmf.utils.*;
import jp.nyatla.nyartoolkit.jogl.utils.*;
import jp.nyatla.nyartoolkit.nyar.NyARMarkerSystem;


/**
 * JMFからの映像入力からマーカを検出し、そこに立方体を重ねます。
 * スケッチシステムを使わない、新しいマーカシステムのサンプルです。
 * ARマーカには、patt.hiroを使用して下さい。
 */
public class RealtimeMarkerSystem implements GLEventListener
{
	// NyARToolkit関係
	private NyARJmfCamera _camera;
	private NyARGlMarkerSystem _nyar;
	private final static String ARCODE_FILE = "../../Data/patt.hiro";
	private final static String ARCODE_FILE2 = "../../Data/patt.kanji";
	private int[] ids=new int[2];
	public RealtimeMarkerSystem(NyARParam i_param) throws NyARException
	{		
		this._camera=new NyARJmfCamera(i_param,30.0f);//create sensor system
		this._nyar=new NyARGlMarkerSystem(i_param);   //create MarkerSystem
		this.ids[0]=this._nyar.addARMarker(ARCODE_FILE2,16,25,80);
		this.ids[1]=this._nyar.addARMarker(ARCODE_FILE,16,25,80);
		
		Frame frame= new Frame("NyARTK program");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
		GLCanvas canvas = new GLCanvas();
		frame.add(canvas);
		canvas.addGLEventListener(this);
		NyARIntSize s=i_param.getScreenSize();

		frame.setVisible(true);
		Insets ins = frame.getInsets();
		frame.setSize(s.w + ins.left + ins.right,s.h + ins.top + ins.bottom);		
		canvas.setBounds(ins.left, ins.top, s.w,s.h);


		this._camera.start();
	}

	public void init(GLAutoDrawable drawable)
	{
		GL gl=drawable.getGL();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadMatrixd(this._nyar.getGlProjectionMatrix(),0);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		Animator animator = new Animator(drawable);
		animator.start();
		return;
	}
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
		GL gl=drawable.getGL();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glViewport(0, 0, width, height);
		return;
	}
	public void display(GLAutoDrawable drawable)
	{
		GL gl=drawable.getGL();
		synchronized(this._camera)
		{
			try {
				gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT); // Clear the buffers for new frame.
				NyARGLDrawUtil.drawBackGround(gl,this._camera.getSourceImage(), 1.0);				
				this._nyar.update(this._camera);
				if(this._nyar.isExistMarker(this.ids[0])){
					gl.glMatrixMode(GL.GL_MODELVIEW);
					gl.glPushMatrix();
					gl.glLoadMatrixd(this._nyar.getGlMarkerTransMat(this.ids[0]),0);
					NyARGLDrawUtil.drawColorCube(gl,40);
					gl.glPopMatrix();
				}
				if(this._nyar.isExistMarker(this.ids[1])){
					gl.glMatrixMode(GL.GL_MODELVIEW);
					gl.glPushMatrix();
					gl.glLoadMatrixd(this._nyar.getGlMarkerTransMat(this.ids[1]),0);
					NyARGLDrawUtil.drawColorCube(gl,40);
					gl.glPopMatrix();
				}
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2)
	{
	}
	
	private final static String PARAM_FILE = "../../Data/camera_para.dat";
	private final static int SCREEN_X = 640;
	private final static int SCREEN_Y = 480;

	public static void main(String[] args)
	{
		try {
			NyARParam param = new NyARParam();
			param.loadARParamFromFile(PARAM_FILE);
			param.changeScreenSize(SCREEN_X, SCREEN_Y);
			new RealtimeMarkerSystem(param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

}