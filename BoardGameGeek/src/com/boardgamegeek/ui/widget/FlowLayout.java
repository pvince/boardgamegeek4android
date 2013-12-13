package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;

// xmlns:bgg="http://schemas.android.com/apk/res/com.boardgamegeek"

public class FlowLayout extends ViewGroup {
	private int mHorizontalSpacing;
	private int mVerticalSpacing;

	public FlowLayout(Context context) {
		super(context);
	}

	public FlowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout);
		try {
			mHorizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_horizontalSpacing, 0);
			mVerticalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_horizontalSpacing, 0);
		} finally {
			a.recycle();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec) - getPaddingRight();

		int width = 0;
		int height = getPaddingTop();

		int currentWidth = getPaddingLeft();
		int currentHeight = 0;

		boolean breakLine = false;
		// boolean newLine = false;

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);

			if (child.getVisibility() == View.GONE) {
				continue;
			}

			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			measureChild(child, widthMeasureSpec, heightMeasureSpec);

			if (breakLine || currentWidth + child.getMeasuredWidth() > widthSize) {
				height += currentHeight + mVerticalSpacing;
				width = Math.max(width, currentWidth - mHorizontalSpacing);

				currentHeight = 0;
				currentWidth = getPaddingLeft();

				// newLine = false;
				// } else {
				// newLine = true;
			}

			lp.x = currentWidth;
			lp.y = height;

			currentWidth += child.getMeasuredWidth() + mHorizontalSpacing;
			currentHeight = Math.max(currentHeight, child.getMeasuredHeight());

			breakLine = lp.breakLine;
		}

		// if (newLine) {
		height += currentHeight + mVerticalSpacing;
		width = Math.max(width, currentWidth - mHorizontalSpacing);
		// }

		width += getPaddingRight();
		height = height - mVerticalSpacing + getPaddingBottom();

		setMeasuredDimension(resolveSize(widthSize, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);

			if (child.getVisibility() == View.GONE) {
				continue;
			}

			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			child.layout(lp.x, lp.y, lp.x + child.getMeasuredWidth(), child.getMeasuredHeight());
		}
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected android.view.ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p.width, p.height);
	}

	public static class LayoutParams extends ViewGroup.LayoutParams {
		public boolean breakLine;
		public int spacing;

		private int x;
		private int y;

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);

			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout);
			try {
				spacing = a.getDimensionPixelSize(R.styleable.FlowLayout_LayoutParams_layout_spacing, -1);
				breakLine = a.getBoolean(R.styleable.FlowLayout_LayoutParams_breakLine, false);
			} finally {
				a.recycle();
			}
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}
	}
}
