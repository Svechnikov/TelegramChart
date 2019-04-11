package io.svechnikov.telegramchart.chart.data;

import android.os.Parcel;
import android.os.Parcelable;

public class ChartsViewState implements Parcelable {

    public final ChartViewState[] states;
    public final int scroll;

    public ChartsViewState(ChartViewState[] chartStates,
                           int scroll) {
        this.states = chartStates;
        this.scroll = scroll;
    }

    public ChartsViewState(Parcel parcel) {
        states = parcel.createTypedArray(ChartViewState.CREATOR);
        scroll = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(states, flags);
        dest.writeInt(scroll);
    }

    public static final Parcelable.Creator<ChartsViewState> CREATOR =
            new Parcelable.Creator<ChartsViewState>() {

                public ChartsViewState createFromParcel(Parcel in) {
                    return new ChartsViewState(in);
                }

                public ChartsViewState[] newArray(int size) {
                    return new ChartsViewState[size];
                }
            };
}
