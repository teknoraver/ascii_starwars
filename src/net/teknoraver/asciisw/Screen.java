package net.teknoraver.asciisw;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

public class Screen extends View {
	static final int COLS = 67;
	static final int ROWS = 13;

	private Paint paint;
	private Frame frame;
	private Rect fontBounds;

	private Rect findMaxBounds() {
		Rect max = new Rect();
		Rect bounds = new Rect();
		char c[] = new char[1];
//		int maxwidth = -1;
//		int maxheight = -1;
		for(c[0] = ' '; c[0] <= '~'; c[0]++) {
			paint.getTextBounds(c, 0, 1, bounds);
			max.union(bounds);
/*			if(bounds.width() > maxwidth) {
				System.out.printf("Metrics: '%c' largest (%d)\n", c[0], bounds.width());
				maxwidth = bounds.width();
			}
			if(bounds.height() > maxheight) {
				System.out.printf("Metrics: '%c' tallest (%d)\n", c[0], bounds.height());
				maxheight = bounds.height();
			}*/
		}
		return max;
	}

	private void calcTextSize(int w, int h) {
		int fontsize = 40;
//		System.out.printf("Metrics: calculating for %dx%d\n", w, h);
		do {
			paint.setTextSize(--fontsize);
			fontBounds = findMaxBounds();
//			System.out.printf("Metrics(%d): %d > %d || %d > %d\n", fontsize, fontBounds.width() * COLS, w, fontBounds.height() * ROWS, h);
		} while((fontBounds.width() - 1) * COLS > w || fontBounds.height() * ROWS > h);
//		System.out.printf("Metrics: calculated %d\n", fontsize);
//		return fontsize;
	}

	public Screen(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		paint.setTypeface(Typeface.MONOSPACE);
		paint.setAntiAlias(true);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		calcTextSize(w, h);
//		System.out.printf("Metrics: %dx%d\n", fontBounds.width(), fontBounds.height());
//		paint.setTextSize(20);
//System.out.println("Metrics: Spacing: " + paint.getFontSpacing());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if(frame == null)
			return;

		for(int i = 0; i < frame.rows.length; i++)
			canvas.drawText(frame.rows[i], 0, fontBounds.height() * (i + 1), paint);

		if(frame.comment != null)
			Toast.makeText(getContext(), frame.comment, Toast.LENGTH_SHORT).show();
	}

	public void setText(Frame f) {
		frame = f;
		invalidate();
	}

	public void clear() {
		frame = new Frame(0, new String[0]);
		invalidate();
	}
}
