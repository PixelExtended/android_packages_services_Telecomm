/*
 * Copyright (C) 2013 The Android Open Source Project
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
 Ca* See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.telecomm.testcallservice;

import com.android.telecomm.tests.R;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import java.util.Date;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.RemoteException;
import android.telecomm.CallInfo;
import android.telecomm.CallService;
import android.telecomm.ICallServiceAdapter;
import android.text.TextUtils;
import android.util.Log;

/**
 * Service which provides fake calls to test the ICallService interface.
 * TODO(santoscordon): Rename all classes in the directory to Dummy* (e.g., DummyCallService).
 */
public class TestCallService extends CallService {
    private static final String TAG = TestCallService.class.getSimpleName();

    /**
     * The application context.
     */
    private final Context mContext;

    /**
     * Set of call IDs for live (active, ringing, dialing) calls.
     * TODO(santoscordon): Reference CallState javadoc when available for the different call states.
     */
    private Set<String> mLiveCallIds;

    /**
     * Adapter to call back into CallsManager.
     */
    private ICallServiceAdapter mCallsManagerAdapter;

    /**
     * Used to play an audio tone during a call.
     */
    private MediaPlayer mMediaPlayer;

    /** Persists the specified parameters. */
    public TestCallService(Context context) {
        mContext = context;
    }

    /** {@inheritDoc} */
    @Override
    public void setCallServiceAdapter(ICallServiceAdapter callServiceAdapter) {
        Log.i(TAG, "setCallServiceAdapter()");

        mCallsManagerAdapter = callServiceAdapter;
        mLiveCallIds = Sets.newHashSet();

        // Prepare the media player to play a tone when there is a call.
        mMediaPlayer = MediaPlayer.create(mContext, R.raw.beep_boop);
        mMediaPlayer.setLooping(true);

        // TODO(santoscordon): Re-enable audio through voice-call stream.
        //mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
    }

    /**
     * Responds as compatible for all calls except those starting with the number 7 (arbitrarily
     * chosen for testing purposes).
     *
     * {@inheritDoc}
     */
    @Override
    public void isCompatibleWith(CallInfo callInfo) {
        Log.i(TAG, "isCompatibleWith(" + callInfo + ")");
        Preconditions.checkNotNull(callInfo.getHandle());

        // Is compatible if the handle doesn't start with 7.
        boolean isCompatible = !callInfo.getHandle().startsWith("7");

        try {
            // Tell CallsManager whether this call service can place the call (is compatible).
            // Returning positively on setCompatibleWith() doesn't guarantee that we will be chosen
            // to place the call. If we *are* chosen then CallsManager will execute the call()
            // method below.
            mCallsManagerAdapter.setCompatibleWith(callInfo.getId(), isCompatible);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to setCompatibleWith().", e);
        }
    }

    /**
     * Starts a call by calling into the adapter. For testing purposes this methods acts as if a
     * call was successfully connected every time.
     *
     * {@inheritDoc}
     */
    @Override
    public void call(CallInfo callInfo) {
        Log.i(TAG, "call(" + callInfo + ")");

        createCall(callInfo.getId());

        try {
            mCallsManagerAdapter.handleSuccessfulOutgoingCall(callInfo.getId());
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to create a newOutgoingCall().", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void disconnect(String callId) {
        Log.i(TAG, "disconnect(" + callId + ")");

        try {
            destroyCall(callId);
            mCallsManagerAdapter.setDisconnected(callId);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to setDisconnected().", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean onUnbind(Intent intent) {
        mMediaPlayer = null;

        return super.onUnbind(intent);
    }

    /**
     * Adds the specified call ID to the set of live call IDs and starts playing audio on the
     * voice-call stream.
     *
     * @param callId The identifier of the call to create.
     */
    private void createCall(String callId) {
        Preconditions.checkState(!Strings.isNullOrEmpty(callId));
        mLiveCallIds.add(callId);

        // Starts audio if not already started.
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    /**
     * Removes the specified call ID from the set of live call IDs and stops playing audio if
     * there exist no more live calls.
     *
     * @param callId The identifier of the call to destroy.
     */
    private void destroyCall(String callId) {
        Preconditions.checkState(!Strings.isNullOrEmpty(callId));
        mLiveCallIds.remove(callId);

        // Stops audio if there are no more calls.
        if (mLiveCallIds.isEmpty() && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }
}
