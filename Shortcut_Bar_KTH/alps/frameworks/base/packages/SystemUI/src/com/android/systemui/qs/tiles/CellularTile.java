/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.Icon;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController.DataUsageInfo;
import com.android.systemui.statusbar.policy.NetworkController.NetworkSignalChangedCallback;

/// M: add DataUsage in quicksetting @{
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.PluginFactory;
/// add DataUsage in quicksetting @}
import com.mediatek.xlog.Xlog;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.widget.Toast;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

/** Quick settings tile: Cellular **/
public class CellularTile extends QSTile<QSTile.SignalState> {
    private static final Intent CELLULAR_SETTINGS = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));

    private static final String TAG = "CellularTile";
    private static final boolean DBG = true;
    
    private final NetworkController mController;
    private final MobileDataController mDataController;
    private final CellularDetailAdapter mDetailAdapter;
    /// M: add DataUsage for operator @{
    private IQuickSettingsPlugin mQuickSettingsPlugin;
    private boolean mDisplayDataUsage;
    private Icon mIcon;
    /// add DataUsage for operator @}

	// kth add for LFZSF-10  shortcut bar at 2010820 start
	private static final String CONTROLACTION_DATA = "com.android.test.controller.data";
	private static final String CONTROLACTION_DATA_DETAIL = "com.android.test.controller.data.detail";
	private static final String CONTROLACTION_DATA_UPDATE_STATE = "com.android.test.controller.data.updatestate";
	
	private BroadcastReceiver controlReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "CellularTile received = "+ intent.getAction());
			if (intent.getAction().equals(CONTROLACTION_DATA)) {
				// shortcut bar cellular controller is not controlled by this, it's not used now
				setListening(true);
				if (mDataController.isMobileDataSupported()
		                && SubscriptionManager.getDefaultDataSubId() != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
		            mDetailAdapter.setToggleState(!mDetailAdapter.getToggleState());
		        }
				Log.i(TAG, "CellularTile mDetailAdapter.getToggleState() = "+ mDetailAdapter.getToggleState());
				refreshState();
			}
			else if (intent.getAction().equals(CONTROLACTION_DATA_DETAIL)){
  				 mHost.startSettingsActivity(CELLULAR_SETTINGS);
			}
		}
	};
	// kth add for LFZSF-10  shortcut bar at 2010820 end
    public CellularTile(Host host) {
        super(host);
		// kth add for  LFZSF-10 shortcut bar at 2010820 start
		IntentFilter control_filter = new IntentFilter();
		control_filter.addAction(CONTROLACTION_DATA);
		control_filter.addAction(CONTROLACTION_DATA_DETAIL);
		// kth add for  LFZSF-10 shortcut bar at 2010820 end
		mContext.registerReceiver(controlReceiver, control_filter);
        mController = host.getNetworkController();
        mDataController = mController.getMobileDataController();
        mDetailAdapter = new CellularDetailAdapter();
        /// M: add DataUsage for operator @{
        mQuickSettingsPlugin = PluginFactory
                .getQuickSettingsPlugin(mContext);
        mDisplayDataUsage = mQuickSettingsPlugin.customizeDisplayDataUsage(false);
        mIcon = ResourceIcon.get(R.drawable.ic_qs_data_usage);
        /// add DataUsage for operator @}
    }

    @Override
    protected SignalState newTileState() {
        return new SignalState();
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    public void setListening(boolean listening) {
    Log.i(TAG, "CellularTile setListening " + listening);
        if (listening) {
            mController.addNetworkSignalChangedCallback(mCallback);
        } else {
            mController.removeNetworkSignalChangedCallback(mCallback);
        }
    }

    @Override
    public QSTileView createTileView(Context context) {
        return new SignalTileView(context);
    }

    @Override
    protected void handleClick() {
        // M: Start setting activity when default SIM isn't setted
        if (mDataController.isMobileDataSupported()
                && SubscriptionManager.getDefaultDataSubId() != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            showDetail(true);
        } else {
            mHost.startSettingsActivity(CELLULAR_SETTINGS);
        }
    }

    @Override
    protected void handleUpdateState(SignalState state, Object arg) {
        /// M: add DataUsage for operator @{
        if (mDisplayDataUsage) {
            Xlog.i(TAG, "customize datausage, displayDataUsage = " + mDisplayDataUsage);
            state.visible = true;
            state.icon = mIcon;
            state.label = mContext.getString(R.string.data_usage);
            state.contentDescription = mContext.getString(R.string.data_usage);
            return;
        }
        /// add DataUsage for operator @}
        state.visible = mController.hasMobileDataFeature();
        if (!state.visible) return;
        final CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) return;

        final Resources r = mContext.getResources();
        final int iconId = cb.noSim ? R.drawable.ic_qs_no_sim
                : !cb.enabled || cb.airplaneModeEnabled ? R.drawable.ic_qs_signal_disabled
                : cb.mobileSignalIconId > 0 ? cb.mobileSignalIconId
                : R.drawable.ic_qs_signal_no_signal;
        state.icon = ResourceIcon.get(iconId);
        state.isOverlayIconWide = cb.isDataTypeIconWide;
        state.autoMirrorDrawable = !cb.noSim;

        /// M: Update roaming icon with airplane mode state @{
        if (cb.enabled && (cb.dataTypeIconId > 0) && !cb.airplaneModeEnabled) {
            state.overlayIconId = cb.dataTypeIconId;
        } else {
            state.overlayIconId = 0;
        }
        /// M: Update roaming icon with airplane mode state @}

        state.filter = iconId != R.drawable.ic_qs_no_sim;
        state.activityIn = cb.enabled && cb.activityIn;
        state.activityOut = cb.enabled && cb.activityOut;

        state.label = cb.enabled
                ? removeTrailingPeriod(cb.enabledDesc)
                : r.getString(R.string.quick_settings_rssi_emergency_only);

        final String signalContentDesc = cb.enabled && (cb.mobileSignalIconId > 0)
                ? cb.signalContentDescription
                : r.getString(R.string.accessibility_no_signal);
        final String dataContentDesc = cb.enabled && (cb.dataTypeIconId > 0) && !cb.wifiEnabled
                ? cb.dataContentDescription
                : r.getString(R.string.accessibility_no_data);
		
        // /M: Change the label when default SIM isn't setted @{
        if (cb.enabled
                && TelephonyManager.from(mContext).getNetworkOperator() != null
                && SubscriptionManager.getDefaultDataSubId() == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                && !cb.noSim) {
            Xlog.d(TAG, "No default data sim");
            state.icon = ResourceIcon.get(R.drawable.ic_qs_data_sim_not_set);
            state.label = r.getString(R.string.quick_settings_data_sim_notset);
        }
        // @}
        state.contentDescription = r.getString(
                R.string.accessibility_quick_settings_mobile,
                signalContentDesc, dataContentDesc,
                state.label);
    }

    // Remove the period from the network name
    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    private static final class CallbackInfo {
        boolean enabled;
        boolean wifiEnabled;
        boolean wifiConnected;
        boolean airplaneModeEnabled;
        int mobileSignalIconId;
        String signalContentDescription;
        int dataTypeIconId;
        String dataContentDescription;
        boolean activityIn;
        boolean activityOut;
        String enabledDesc;
        boolean noSim;
        boolean isDataTypeIconWide;
    }

    private final NetworkSignalChangedCallback mCallback = new NetworkSignalChangedCallback() {
        private final CallbackInfo mInfo = new CallbackInfo();

        @Override
        public void onWifiSignalChanged(boolean enabled, boolean connected, int wifiSignalIconId,
                boolean activityIn, boolean activityOut,
                String wifiSignalContentDescriptionId, String description) {
            mInfo.wifiEnabled = enabled;
            mInfo.wifiConnected = connected;
            refreshState(mInfo);
        }

        @Override
        public void onMobileDataSignalChanged(boolean enabled,
                int mobileSignalIconId,
                String mobileSignalContentDescriptionId, int dataTypeIconId,
                boolean activityIn, boolean activityOut,
                String dataTypeContentDescriptionId, String description,
                boolean isDataTypeIconWide) {
            mInfo.enabled = enabled;
            mInfo.mobileSignalIconId = mobileSignalIconId;
            mInfo.signalContentDescription = mobileSignalContentDescriptionId;
            mInfo.dataTypeIconId = dataTypeIconId;
            mInfo.dataContentDescription = dataTypeContentDescriptionId;
            mInfo.activityIn = activityIn;
            mInfo.activityOut = activityOut;
            mInfo.enabledDesc = description;
            mInfo.isDataTypeIconWide = isDataTypeIconWide;
            if (DBG) {
                Xlog.d(TAG, "onMobileDataSignalChanged info.enabled = " + mInfo.enabled +
                    " mInfo.mobileSignalIconId = " + mInfo.mobileSignalIconId +
                    " mInfo.signalContentDescription = " + mInfo.signalContentDescription +
                    " mInfo.dataTypeIconId = " + mInfo.dataTypeIconId +
                    " mInfo.dataContentDescription = " + mInfo.dataContentDescription +
                    " mInfo.activityIn = " + mInfo.activityIn +
                    " mInfo.activityOut = " + mInfo.activityOut +
                    " mInfo.enabledDesc = " + mInfo.enabledDesc +
                    " mInfo.isDataTypeIconWide = " + mInfo.isDataTypeIconWide);
            }
            refreshState(mInfo);
        }

        @Override
        public void onNoSimVisibleChanged(boolean visible) {
            mInfo.noSim = visible;
            if (mInfo.noSim) {
                // Make sure signal gets cleared out when no sims.
                mInfo.mobileSignalIconId = 0;
                mInfo.dataTypeIconId = 0;
                // Show a No SIMs description to avoid emergency calls message.
                mInfo.enabled = true;
                mInfo.enabledDesc = mContext.getString(
                        R.string.keyguard_missing_sim_message_short);
                mInfo.signalContentDescription = mInfo.enabledDesc;

                Xlog.d(TAG, "NoSim");
            }
            refreshState(mInfo);
        }

        @Override
        public void onAirplaneModeChanged(boolean enabled) {
            mInfo.airplaneModeEnabled = enabled;
            refreshState(mInfo);
        }

        public void onMobileDataEnabled(boolean enabled) {
            mDetailAdapter.setMobileDataEnabled(enabled);
        }
    };

    private final class CellularDetailAdapter implements DetailAdapter {

        @Override
        public int getTitle() {
            return R.string.quick_settings_cellular_detail_title;
        }

        @Override
        public Boolean getToggleState() {
            return mDataController.isMobileDataSupported()
                    ? mDataController.isMobileDataEnabled()
                    : null;
        }

        @Override
        public Intent getSettingsIntent() {
            return CELLULAR_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            mDataController.setMobileDataEnabled(state);
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final DataUsageDetailView v = (DataUsageDetailView) (convertView != null
                    ? convertView
                    : LayoutInflater.from(mContext).inflate(R.layout.data_usage, parent, false));
            final DataUsageInfo info = mDataController.getDataUsageInfo();
            if (info == null) return v;
            v.bind(info);
            return v;
        }

        public void setMobileDataEnabled(boolean enabled) {
			Log.i(TAG, "CellularTile CellularDetailAdapter setMobileDataEnabled = " + enabled);
            fireToggleStateChanged(enabled);
        }
    }
}
