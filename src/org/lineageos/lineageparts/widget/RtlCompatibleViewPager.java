/*
 * SPDX-FileCopyrightText: 2016 The Android Open Source Project
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

import java.util.Locale;

/**
 * A {@link ViewPager} that's aware of RTL changes when used with FragmentPagerAdapter.
 */
public final class RtlCompatibleViewPager extends ViewPager {

    private int mHeightOffset;

    /**
     * Callback interface for responding to changing state of the selected page.
     * Positions supplied will always be the logical position in the adapter -
     * that is, the 0 index corresponds to the left-most page in LTR and the
     * right-most page in RTL.
     */

    public RtlCompatibleViewPager(Context context) {
        this(context, null /* attrs */);
    }

    public RtlCompatibleViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getCurrentItem() {
        return getRtlAwareIndex(super.getCurrentItem());
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(getRtlAwareIndex(item));
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();

        RtlSavedState rtlSavedState = new RtlSavedState(parcelable);
        rtlSavedState.position = getCurrentItem();
        return rtlSavedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        RtlSavedState rtlSavedState = (RtlSavedState) state;
        super.onRestoreInstanceState(rtlSavedState.getSuperState());

        setCurrentItem(rtlSavedState.position);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec - mHeightOffset);
    }

    public void setHeightOffset(int heightOffset) {
        mHeightOffset = heightOffset;
        requestLayout();
    }

    /**
     * Get a "RTL friendly" index. If the locale is LTR, the index is returned as is.
     * Otherwise it's transformed so view pager can render views using the new index for RTL. For
     * example, the second view will be rendered to the left of first view.
     *
     * @param index The logical index.
     */
    public int getRtlAwareIndex(int index) {
        // Using TextUtils rather than View.getLayoutDirection() because LayoutDirection is not
        // defined until onMeasure, and this is called before then.
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL) {
            return getAdapter().getCount() - index - 1;
        }
        return index;
    }

    static class RtlSavedState extends BaseSavedState {
        int position;

        public RtlSavedState(Parcelable superState) {
            super(superState);
        }

        private RtlSavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            position = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(position);
        }

        public static final Parcelable.ClassLoaderCreator<RtlSavedState> CREATOR
                = new Parcelable.ClassLoaderCreator<>() {
            @Override
            public RtlSavedState createFromParcel(Parcel source,
                    ClassLoader loader) {
                return new RtlSavedState(source, loader);
            }

            @Override
            public RtlSavedState createFromParcel(Parcel in) {
                return new RtlSavedState(in, null);
            }

            @Override
            public RtlSavedState[] newArray(int size) {
                return new RtlSavedState[size];
            }
        };

    }

}
