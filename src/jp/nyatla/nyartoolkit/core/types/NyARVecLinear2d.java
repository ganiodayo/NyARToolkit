package jp.nyatla.nyartoolkit.core.types;


/**
 * 点の座標と、そのベクトルで定義する直線を格納します。
 *
 */
public class NyARVecLinear2d
{
	public double x;
	public double y;
	public double dx;
	public double dy;
	public static NyARVecLinear2d[] createArray(int i_length)
	{
		NyARVecLinear2d[] r=new NyARVecLinear2d[i_length];
		for(int i=0;i<i_length;i++){
			r[i]=new NyARVecLinear2d();
		}
		return r;
	}
	/**
	 * 法線ベクトルを計算します。
	 * @param i_src
	 * 元のベクトルを指定します。この値には、thisを指定できます。
	 */
	public final void normalVec(NyARVecLinear2d i_src)
	{
		double w=this.dx;
		this.dx=i_src.dy;
		this.dy=-w;
	}
	public final void setValue(NyARVecLinear2d i_value)
	{
		this.dx=i_value.dx;
		this.dy=i_value.dy;
		this.x=i_value.x;
		this.y=i_value.y;
	}
	/**
	 * このベクトルと指定したベクトルの作るCos値を返します。
	 * @param i_v1
	 * @return
	 */
	public final double getVecCos(NyARVecLinear2d i_v1)
	{
		double x1=i_v1.dx;
		double y1=i_v1.dy;
		double x2=this.dx;
		double y2=this.dy;
		double d=(x1*x2+y1*y2)/Math.sqrt((x1*x1+y1*y1)*(x2*x2+y2*y2));
		return d;
	}
	/**
	 * 個のベクトルと指定したベクトルの作るCos値を返します。
	 * @param i_v2_x
	 * @param i_v2_y
	 * @return
	 */
	public final double getVecCos(double i_v2_x,double i_v2_y)
	{
		double x1=this.dx;
		double y1=this.dy;
		double d=(x1*i_v2_x+y1*i_v2_y)/Math.sqrt((x1*x1+y1*y1)*(i_v2_x*i_v2_x+i_v2_y*i_v2_y));
		return d;
	}
	public final double getAbsVecCos(double i_v2_x,double i_v2_y)
	{
		double x1=this.dx;
		double y1=this.dy;
		double d=(x1*i_v2_x+y1*i_v2_y)/Math.sqrt((x1*x1+y1*y1)*(i_v2_x*i_v2_x+i_v2_y*i_v2_y));
		return d>=0?d:-d;
	}	
	/**
	 * 交点を求めます。
	 * @param i_vector1
	 * @param i_vector2
	 * @param o_point
	 * @return
	 */
	public final boolean crossPos(NyARVecLinear2d i_vector1,NyARDoublePoint2d o_point)
	{
		double a1= i_vector1.dy;
		double b1=-i_vector1.dx;
		double c1=(i_vector1.dx*i_vector1.y-i_vector1.dy*i_vector1.x);
		double a2= this.dy;
		double b2=-this.dx;
		double c2=(this.dx*this.y-this.dy*this.x);
		final double w1 = a1 * b2 - a2 * b1;
		if (w1 == 0.0) {
			return false;
		}
		o_point.x = (b1 * c2 - b2 * c1) / w1;
		o_point.y = (a2 * c1 - a1 * c2) / w1;
		return true;
	}
	/**
	 * 直線と、i_sp1とi_sp2の作る線分との二乗距離値の合計を返します。
	 * 線分と直線の類似度を
	 * @param i_sp1
	 * @param i_sp2
	 * @param o_point
	 * @return
	 * 距離が取れないときは無限大です。
	 */
	public final double sqDistBySegmentLineEdge(NyARDoublePoint2d i_sp1,NyARDoublePoint2d i_sp2)
	{
		double sa,sb,sc;
		sa= this.dy;
		sb=-this.dx;
		sc=(this.dx*this.y-this.dy*this.x);
		

		double lc;
		double x,y,w1;
		//thisを法線に変換

		//交点を計算
		w1 = sa * (-sa) - sb * sb;
		if (w1 == 0.0) {
			return Double.POSITIVE_INFINITY;
		}
		//i_sp1と、i_linerの交点
		lc=-(sb*i_sp1.x-sa*i_sp1.y);
		x = ((sb * lc +sa * sc) / w1)-i_sp1.x;
		y = ((sb * sc - sa * lc) / w1)-i_sp1.y;
		double sqdist=x*x+y*y;

		lc=-(sb*i_sp2.x-sa*i_sp2.y);
		x = ((sb * lc + sa * sc) / w1)-i_sp2.x;
		y = ((sb * sc - sa * lc) / w1)-i_sp2.y;

		return sqdist+x*x+y*y;
	}	

}