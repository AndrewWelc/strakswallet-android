package com.strakswallet.presenter.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.strakswallet.R;
import com.strakswallet.presenter.activities.settings.SecurityCenterActivity;
import com.strakswallet.presenter.activities.settings.SettingsActivity;
import com.strakswallet.presenter.activities.util.BRActivity;
import com.strakswallet.presenter.customviews.BRButton;
import com.strakswallet.presenter.customviews.BRDialogView;
import com.strakswallet.presenter.customviews.BRNotificationBar;
import com.strakswallet.presenter.customviews.BRText;
import com.strakswallet.tools.adapter.WalletListAdapter;
import com.strakswallet.tools.animation.BRAnimator;
import com.strakswallet.tools.animation.BRDialog;
import com.strakswallet.tools.listeners.RecyclerItemClickListener;
import com.strakswallet.tools.manager.BREventManager;
import com.strakswallet.tools.manager.BRSharedPrefs;
import com.strakswallet.tools.manager.InternetManager;
import com.strakswallet.tools.manager.PromptManager;
import com.strakswallet.tools.manager.SyncManager;
import com.strakswallet.tools.sqlite.CurrencyDataSource;
import com.strakswallet.tools.threads.executor.BRExecutor;
import com.strakswallet.tools.util.BRConstants;
import com.strakswallet.tools.util.CurrencyUtils;
import com.strakswallet.wallet.WalletsMaster;
import com.strakswallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Home activity that will show a list of a user's wallets
 */

public class HomeActivity extends BRActivity implements InternetManager.ConnectionReceiverListener, SyncManager.OnProgressUpdate {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private RecyclerView mWalletRecycler;
    private WalletListAdapter mAdapter;
    private BRText mFiatTotal;
    private RelativeLayout mSettings;
    private RelativeLayout mSecurity;
    private RelativeLayout mSupport;
    private PromptManager.PromptItem mCurrentPrompt;
    public BRNotificationBar mNotificationBar;

    private BRText mPromptTitle;
    private BRText mPromptDescription;
    private BRButton mPromptContinue;
    private BRButton mPromptDismiss;
    private CardView mPromptCard;

    private static HomeActivity app;

    private InternetManager mConnectionReceiver;

    public static HomeActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        WalletsMaster.getInstance(this).initWallets(this);

        ArrayList<BaseWalletManager> walletList = new ArrayList<>();

        walletList.addAll(WalletsMaster.getInstance(this).getAllWallets());

        mWalletRecycler = findViewById(R.id.rv_wallet_list);
        mFiatTotal = findViewById(R.id.total_assets_usd);

        mSettings = findViewById(R.id.settings_row);
        mSecurity = findViewById(R.id.security_row);
        mSupport = findViewById(R.id.support_row);
        mNotificationBar = findViewById(R.id.notification_bar);

        mPromptCard = findViewById(R.id.prompt_card);
        mPromptTitle = findViewById(R.id.prompt_title);
        mPromptDescription = findViewById(R.id.prompt_description);
        mPromptContinue = findViewById(R.id.continue_button);
        mPromptDismiss = findViewById(R.id.dismiss_button);

        mAdapter = new WalletListAdapter(this, walletList);

        mWalletRecycler.setLayoutManager(new LinearLayoutManager(this));
        mWalletRecycler.setAdapter(mAdapter);

        mWalletRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, mWalletRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if (position >= mAdapter.getItemCount() || position < 0) return;
                BRSharedPrefs.putCurrentWalletIso(HomeActivity.this, mAdapter.getItemAt(position).getIso(HomeActivity.this));
//                Log.d("HomeActivity", "Saving current wallet ISO as " + mAdapter.getItemAt(position).getIso(HomeActivity.this));

                Intent newIntent = new Intent(HomeActivity.this, WalletActivity.class);
                startActivity(newIntent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });
        mSecurity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SecurityCenterActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        mSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(HomeActivity.this, null);
            }
        });

        onConnectionChanged(InternetManager.getInstance().isConnected(this));


        //if (!BRSharedPrefs.wasBchDialogShown(this)) { 
        /*
            BRDialog.showHelpDialog(this, getString(R.string.Dialog_welcomeBchTitle), getString(R.string.Dialog_welcomeBchMessage), getString(R.string.Dialog_Home), getString(R.string.Dialog_Dismiss), new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismissWithAnimation();
                }
            }, new BRDialogView.BROnClickListener() {

                @Override
                public void onClick(BRDialogView brDialogView) {
                    getFragmentManager().popBackStack();
                }
            }, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    Log.d(TAG, "help clicked!");
                    brDialogView.dismissWithAnimation();
                    BRAnimator.showSupportFragment(HomeActivity.this, BRConstants.bchFaq);

                }
            });
        */
            BRSharedPrefs.putBchDialogShown(HomeActivity.this, true);
        //}

        mPromptDismiss.setColor(Color.parseColor("#b3c0c8"));
        mPromptDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePrompt();
            }
        });

        mPromptContinue.setColor(Color.parseColor("#4b77f3"));
        mPromptContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PromptManager.PromptInfo info = PromptManager.getInstance().promptInfo(app, mCurrentPrompt);
                if (info.listener != null)
                    info.listener.onClick(mPromptContinue);
                else
                    Log.e(TAG, "Continue :" + info.title + " (FAILED)");
            }
        });

    }

    public void hidePrompt() {
        mPromptCard.setVisibility(View.GONE);
        Log.e(TAG, "hidePrompt: " + mCurrentPrompt);
        if (mCurrentPrompt == PromptManager.PromptItem.SHARE_DATA) {
            BRSharedPrefs.putShareDataDismissed(app, true);
        }
        if (mCurrentPrompt != null)
            BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(mCurrentPrompt) + ".dismissed");
        mCurrentPrompt = null;

    }

    private void showNextPromptIfNeeded() {
        PromptManager.PromptItem toShow = PromptManager.getInstance().nextPrompt(this);
        if (toShow != null) {
            mCurrentPrompt = toShow;
//            Log.d(TAG, "showNextPrompt: " + toShow);
            PromptManager.PromptInfo promptInfo = PromptManager.getInstance().promptInfo(this, toShow);
            mPromptCard.setVisibility(View.VISIBLE);
            mPromptTitle.setText(promptInfo.title);
            mPromptDescription.setText(promptInfo.description);
            mPromptContinue.setOnClickListener(promptInfo.listener);

        } else {
            Log.i(TAG, "showNextPrompt: nothing to show");
        }
    }

    private void setupNetworking() {
        if (mConnectionReceiver == null) mConnectionReceiver = InternetManager.getInstance();
        IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectionReceiver, mNetworkStateFilter);
        InternetManager.addConnectionListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;

        showNextPromptIfNeeded();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.startObserving();
            }
        }, 500);

        setupNetworking();


        InternetManager.addConnectionListener(new InternetManager.ConnectionReceiverListener() {
            @Override
            public void onConnectionChanged(boolean isConnected) {
                Log.e(TAG, "onConnectionChanged: " + isConnected);
                if (isConnected) {
                    mAdapter.startObserving();
                }
            }
        });

        updateUi();
        CurrencyDataSource.getInstance(this).addOnDataChangedListener(new CurrencyDataSource.OnDataChanged() {
            @Override
            public void onChanged() {
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                });

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAdapter.stopObserving();

    }

    private void updateUi() {
        BigDecimal fiatTotalAmount = WalletsMaster.getInstance(this).getAggregatedFiatBalance(this);
        mFiatTotal.setText(CurrencyUtils.getFormattedAmount(this, BRSharedPrefs.getPreferredFiatIso(this), fiatTotalAmount));
        mAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnectionReceiver != null)
            unregisterReceiver(mConnectionReceiver);
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged");
        if (isConnected) {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.INVISIBLE);
            }
            final BaseWalletManager wm = WalletsMaster.getInstance(HomeActivity.this).getCurrentWallet(HomeActivity.this);
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    final double progress = wm.getPeerManager()
                            .getSyncProgress(BRSharedPrefs.getStartHeight(HomeActivity.this,
                                    BRSharedPrefs.getCurrentWalletIso(HomeActivity.this)));
//                    Log.e(TAG, "run: " + progress);
                    if (progress < 1 && progress > 0) {
                        SyncManager.getInstance().startSyncing(HomeActivity.this, wm, HomeActivity.this);
                    }
                }
            });

        } else {
            if (mNotificationBar != null)
                mNotificationBar.setVisibility(View.VISIBLE);

        }


    }

    public void closeNotificationBar() {
        mNotificationBar.setVisibility(View.INVISIBLE);
    }


    @Override
    public boolean onProgressUpdated(double progress) {
        Log.e(TAG, "onProgressUpdated: " + progress);
        return false;
    }
}
