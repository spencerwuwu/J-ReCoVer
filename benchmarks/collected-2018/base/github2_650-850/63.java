// https://searchcode.com/api/result/113896542/

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package info.guardianproject.otr.app.im.app;

import info.guardianproject.otr.app.im.IImConnection;
import info.guardianproject.otr.app.im.R;
import info.guardianproject.otr.app.im.plugin.BrandingResourceIDs;
import info.guardianproject.otr.app.im.provider.Imps;
import info.guardianproject.otr.app.im.service.ImServiceConstants;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AccountActivity extends Activity {
    public static final String TAG = "AccountActivity";
    private static final String ACCOUNT_URI_KEY = "accountUri";

    private long mProviderId;
    private long mAccountId;
    
    static final int REQUEST_SIGN_IN = RESULT_FIRST_USER + 1;

    private static final String[] ACCOUNT_PROJECTION = {
        Imps.Account._ID,
        Imps.Account.PROVIDER,
        Imps.Account.USERNAME,
        Imps.Account.PASSWORD,
        Imps.Account.KEEP_SIGNED_IN,
        Imps.Account.LAST_LOGIN_STATE
    };
    

    private static final int ACCOUNT_PROVIDER_COLUMN = 1;
    private static final int ACCOUNT_USERNAME_COLUMN = 2;
    private static final int ACCOUNT_PASSWORD_COLUMN = 3;
    private static final int ACCOUNT_KEEP_SIGNED_IN_COLUMN = 4;
    private static final int ACCOUNT_LAST_LOGIN_STATE = 5;

    Uri mAccountUri;

    EditText mEditUserAccount;
    EditText mEditPass;
    CheckBox mRememberPass;
  //  CheckBox mKeepSignIn; //n8fr8 removed 2011/04/20 
    CheckBox mUseTor;
    Button   mBtnSignIn;
    Button	 mBtnAdvanced;
    
    boolean isEdit = false;
    boolean isSignedIn = false;

    String mUserName;
    String mDomain;
    int mPort;
    private boolean mHaveSetUseTor = false;
    private String mOriginalUserAccount;
    
   // String mToAddress;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.account_activity);
        mEditUserAccount = (EditText)findViewById(R.id.edtName);
        mEditPass = (EditText)findViewById(R.id.edtPass);
        mRememberPass = (CheckBox)findViewById(R.id.rememberPassword);
 //       mKeepSignIn = (CheckBox)findViewById(R.id.keepSignIn);
        mUseTor = (CheckBox)findViewById(R.id.useTor);
        mBtnSignIn = (Button)findViewById(R.id.btnSignIn);
        
        mBtnAdvanced = (Button)findViewById(R.id.btnAdvanced);
        
        mRememberPass.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                updateWidgetState();
            }
        });

        ImApp app = ImApp.getApplication(this);
        Intent i = getIntent();
        String action = i.getAction();
        
        if (i.hasExtra("isSignedIn"))
        	isSignedIn = i.getBooleanExtra("isSignedIn", false);
        
    //    mToAddress = i.getStringExtra(ImApp.EXTRA_INTENT_SEND_TO_USER);
        final ProviderDef provider;

        ContentResolver cr = getContentResolver();
		Uri uri = i.getData();
		// check if there is account information and direct accordingly
        if (Intent.ACTION_INSERT_OR_EDIT.equals(action)) {
            if ((uri == null) || !Imps.Account.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {
        		action = Intent.ACTION_INSERT;
            } else {
        		action = Intent.ACTION_EDIT;
            }
        }

        if(Intent.ACTION_INSERT.equals(action)) {
            mOriginalUserAccount = "";
            // TODO once we implement multiple IM protocols
            mProviderId = ContentUris.parseId(i.getData());
            provider = app.getProvider(mProviderId);
            setTitle(getResources().getString(R.string.add_account, provider.mFullName));
        } else if(Intent.ACTION_EDIT.equals(action)) {
            if ((uri == null) || !Imps.Account.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {
                Log.w(ImApp.LOG_TAG, "<AccountActivity>Bad data");
                return;
            }
            
            isEdit = true;

            Cursor cursor = cr.query(uri, ACCOUNT_PROJECTION, null, null, null);

            if (cursor == null) {
                finish();
                return;
            }

            if (!cursor.moveToFirst()) {
                cursor.close();
                finish();
                return;
            }

            setTitle(R.string.sign_in);

            mAccountId = cursor.getLong(cursor.getColumnIndexOrThrow(Imps.Account._ID));

            mProviderId = cursor.getLong(ACCOUNT_PROVIDER_COLUMN);
            provider = app.getProvider(mProviderId);

    		ContentResolver contentResolver = getContentResolver();
    		Imps.ProviderSettings.QueryMap settings = 
    			new Imps.ProviderSettings.QueryMap(contentResolver,
    					mProviderId, false, null);

            mOriginalUserAccount = cursor.getString(ACCOUNT_USERNAME_COLUMN) + "@" + settings.getDomain();
            mEditUserAccount.setText(mOriginalUserAccount);
            mEditPass.setText(cursor.getString(ACCOUNT_PASSWORD_COLUMN));

            mRememberPass.setChecked(!cursor.isNull(ACCOUNT_PASSWORD_COLUMN));

//            boolean keepSignIn = cursor.getInt(ACCOUNT_KEEP_SIGNED_IN_COLUMN) == 1;
 //           mKeepSignIn.setChecked(keepSignIn);

            mUseTor.setChecked(settings.getUseTor());
            
           
            
            settings.close();
            cursor.close();
        } else {
            Log.w(ImApp.LOG_TAG, "<AccountActivity> unknown intent action " + action);
            finish();
            return;
        }

        if (isSignedIn)
        {
        	mBtnSignIn.setText(getString(R.string.menu_sign_out));
        	mBtnSignIn.setBackgroundResource(R.drawable.btn_red);
        }
        
        final BrandingResources brandingRes = app.getBrandingResource(mProviderId);
        /*
        mKeepSignIn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                CheckBox keepSignIn = (CheckBox) v;
                if ( keepSignIn.isChecked() ) {
                    String msg = brandingRes.getString(BrandingResourceIDs.STRING_TOAST_CHECK_AUTO_SIGN_IN);
                    Toast.makeText(AccountActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
        });
        */
        
        
        mRememberPass.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	
                CheckBox mRememberPass = (CheckBox) v;
                
                if ( mRememberPass.isChecked() ) {
                    String msg = brandingRes.getString(BrandingResourceIDs.STRING_TOAST_CHECK_SAVE_PASSWORD);
                    Toast.makeText(AccountActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
        });

        mEditUserAccount.setOnFocusChangeListener(new OnFocusChangeListener() {

        	@Override
        	public void onFocusChange(View v, boolean hasFocus) {
        		if (! hasFocus) {
        			String username = mEditUserAccount.getText().toString();

        			Log.i(TAG, "Username changed: " + mOriginalUserAccount + " != " + username);
        			if (parseAccount(username)) {
        				if (username != mOriginalUserAccount) {
        					settingsForDomain(mDomain, mPort);
        					mHaveSetUseTor = false;
        				}
        			} else {
        				// TODO if bad account name, bump back to the account EditText
        				//mEditUserAccount.requestFocus();
        			}
        		}
        	}
        });
        mEditUserAccount.addTextChangedListener(mTextWatcher);
        mEditPass.addTextChangedListener(mTextWatcher);

        mBtnAdvanced.setOnClickListener(new OnClickListener() {
        	
        	public void onClick(View v) {
        		showAdvanced();
        	}
        });
        
        mBtnSignIn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	
            	
                final String pass = mEditPass.getText().toString();
                final boolean rememberPass = mRememberPass.isChecked();

                ContentResolver cr = getContentResolver();

                if (! parseAccount(mEditUserAccount.getText().toString())) {
                	mEditUserAccount.selectAll();
                	mEditUserAccount.requestFocus();
                	return;
                }
                
                long accountId = ImApp.insertOrUpdateAccount(cr, mProviderId, mUserName,
                        rememberPass ? pass : null);
                
                mAccountUri = ContentUris.withAppendedId(Imps.Account.CONTENT_URI, accountId);

                //if remember pass is true, set the "keep signed in" property to true
            
                
                
                if (isSignedIn)
                {
                	//if you are signing out, then we will deactive "auto" sign in
                    ContentValues values = new ContentValues();
                    values.put(Imps.Account.KEEP_SIGNED_IN, false ? 1 : 0);
                    getContentResolver().update(mAccountUri, values, null, null);
                    
                	signOut();
                }
                else
                {
                    ContentValues values = new ContentValues();
                    values.put(Imps.Account.KEEP_SIGNED_IN, rememberPass ? 1 : 0);
                    getContentResolver().update(mAccountUri, values, null, null);
                    
	                if (!mOriginalUserAccount.equals(mUserName + mDomain) && shouldShowTermOfUse(brandingRes)) {
	                    confirmTermsOfUse(brandingRes, new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int which) {
	                            signIn(rememberPass, pass);
	                        }
	                    });
	                } else {
	                    signIn(rememberPass, pass);
	                }
                }
            }

            void signIn(boolean rememberPass, String pass) {
                final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                        getContentResolver(),
                        mProviderId,
                        false /* don't keep updated */,
                        null /* no handler */);
                
                if (!mHaveSetUseTor && mUseTor.isChecked()) {
                	// if using Tor, disable DNS SRV to reduce anonymity leaks
                	settings.setDoDnsSrv(false);
                	mHaveSetUseTor = true;
                }
            	settings.setUseTor(mUseTor.isChecked());
            	settings.close();
                
                Intent intent = new Intent(AccountActivity.this, SigningInActivity.class);
                intent.setData(mAccountUri);
                if (!rememberPass) {
                    intent.putExtra(ImApp.EXTRA_INTENT_PASSWORD, pass);
                }

            	/*
                if (mToAddress != null) {
                    intent.putExtra(ImApp.EXTRA_INTENT_SEND_TO_USER, mToAddress);
                }*/
                
                startActivityForResult(intent, REQUEST_SIGN_IN);
            }
        });

        /*
        // Make link for signing up.
        String publicXmppServices = "http://xmpp.org/services/";
        	
        String text = brandingRes.getString(BrandingResourceIDs.STRING_LABEL_SIGN_UP);
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        builder.setSpan(new URLSpan(publicXmppServices), 0, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView signUp = (TextView)findViewById(R.id.signUp);
        signUp.setText(builder);
        signUp.setMovementMethod(LinkMovementMethod.getInstance());
         */
        // repurposing R.id.signUp for short term kludge for account settings message
       
        updateWidgetState();
                
    }

    boolean parseAccount(String userField) {
    	boolean isGood = true;
    	String[] splitAt = userField.split("@");
    	mUserName = splitAt[0];
    	mDomain = null;
    	mPort = 5222;

    	if (splitAt.length > 1) {
    		mDomain = splitAt[1].toLowerCase();
    		String[] splitColon = mDomain.split(":");
    		mDomain = splitColon[0];
    		if(splitColon.length > 1) {
    			try {
    				mPort = Integer.parseInt(splitColon[1]);
    			} catch (NumberFormatException e) {
    				// TODO move these strings to strings.xml
    				isGood = false;
    				Toast.makeText(AccountActivity.this, 
    						"The port value '" + splitColon[1] +
    						"' after the : could not be parsed as a number!",
    						Toast.LENGTH_LONG).show();
    			}
    		}
    	}

    	if (mDomain == null) {
    		isGood = false;
    		Toast.makeText(AccountActivity.this, 
    				R.string.account_wizard_no_domain_warning,
    				Toast.LENGTH_LONG).show();
    	} else if (mDomain.indexOf(".") == -1) {
    		isGood = false;
    		Toast.makeText(AccountActivity.this, 
    				R.string.account_wizard_no_root_domain_warning,
    				Toast.LENGTH_LONG).show();
    	}

    	return isGood;
    }

    void settingsForDomain(String domain, int port) {
    	final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
    			getContentResolver(),
    			mProviderId,
    			false /* don't keep updated */,
    			null /* no handler */);
    	if (domain.equals("gmail.com")) {
			// Google only supports a certain configuration for XMPP:
			// http://code.google.com/apis/talk/open_communications.html
    		// TODO we should probably use DNS SRV for gmail.com so we can validate the cert
    		// then perhaps we could enable RequireTls
    		settings.setDoDnsSrv(false);
    		settings.setDomain(domain);
    		settings.setPort(5222);
    		settings.setServer("talk.google.com");
    		settings.setRequireTls(false);
    		settings.setTlsCertVerify(false);
    	} else if (domain.equals("jabber.org")) {
    		settings.setDoDnsSrv(false);
    		settings.setDomain(domain);
    		settings.setPort(5222);
    		settings.setServer(null);
    		settings.setRequireTls(true);
    		settings.setTlsCertVerify(true);
    	} else if (domain.equals("chat.facebook.com")) {
    		settings.setDoDnsSrv(false);
    		settings.setDomain(domain);
    		settings.setPort(5222);
    		settings.setServer(domain);
    		settings.setRequireTls(false);
    		settings.setTlsCertVerify(false);
    	} else {
    		settings.setDoDnsSrv(true);
    		settings.setDomain(domain);
    		settings.setPort(port);
    		settings.setServer(null);
    		settings.setRequireTls(false);
    		settings.setTlsCertVerify(true);
    	}
    	settings.close();
    }

    void confirmTermsOfUse(BrandingResources res, DialogInterface.OnClickListener accept) {
        SpannableString message = new SpannableString(
                res.getString(BrandingResourceIDs.STRING_TOU_MESSAGE));
        Linkify.addLinks(message, Linkify.ALL);

        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(res.getString(BrandingResourceIDs.STRING_TOU_TITLE))
            .setMessage(message)
            .setPositiveButton(res.getString(BrandingResourceIDs.STRING_TOU_DECLINE), null)
            .setNegativeButton(res.getString(BrandingResourceIDs.STRING_TOU_ACCEPT), accept)
            .show();
    }

    boolean shouldShowTermOfUse(BrandingResources res) {
        return !TextUtils.isEmpty(res.getString(BrandingResourceIDs.STRING_TOU_MESSAGE));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAccountUri = savedInstanceState.getParcelable(ACCOUNT_URI_KEY);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ACCOUNT_URI_KEY, mAccountUri);
    }
    
    
    void signOutUsingActivity () {
        final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
                getContentResolver(),
                mProviderId,
                false /* don't keep updated */,
                null /* no handler */);
        
        Intent intent = new Intent(AccountActivity.this, SignoutActivity.class);
        intent.setData(mAccountUri);
        
		settings.close();
	
        startActivity(intent);
    }
    
    
    private Handler mHandler = new Handler();
    private ImApp mApp = null;
    
    void signOut ()
    {
    	
    	mApp = ImApp.getApplication(AccountActivity.this);
    	
        mApp.callWhenServiceConnected(mHandler, new Runnable() {
            public void run() {
              
            	signOut(mProviderId, mAccountId);
            }
        });
    	
    }
  
    void signOut (long providerId, long accountId)
    {

        try {	
        	
       
            IImConnection conn = mApp.getConnection(providerId);
            if (conn != null) {
                conn.logout();
            } else {
                // Normally, we can always get the connection when user chose to
                // sign out. However, if the application crash unexpectedly, the
                // status will never be updated. Clear the status in this case
                // to make it recoverable from the crash.
                ContentValues values = new ContentValues(2);
                values.put(Imps.AccountStatus.PRESENCE_STATUS,
                        Imps.Presence.OFFLINE);
                values.put(Imps.AccountStatus.CONNECTION_STATUS,
                        Imps.ConnectionStatus.OFFLINE);
                String where = Imps.AccountStatus.ACCOUNT + "=?";
                getContentResolver().update(Imps.AccountStatus.CONTENT_URI,
                        values, where,
                        new String[] { Long.toString(accountId) });
            }
        } catch (RemoteException ex) {
            Log.e(ImApp.LOG_TAG, "signout: caught ", ex);
        } finally {
          
        	
        	
           Toast.makeText(this, getString(R.string.signed_out_prompt,this.mEditUserAccount.getText()), Toast.LENGTH_LONG).show();
           isSignedIn = false;
           
           mBtnSignIn.setText(getString(R.string.sign_in));
           mBtnSignIn.setBackgroundResource(R.drawable.btn_green);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	
    	
        if (requestCode == REQUEST_SIGN_IN) {
            if (resultCode == RESULT_OK) {
             
                finish();
            } else {
                // sign in failed, disable keep sign in, clear the password.
                
                mEditPass.setText("");
                ContentValues values = new ContentValues();
                values.put(Imps.Account.PASSWORD, (String) null);
                getContentResolver().update(mAccountUri, values, null, null);
            }
        }
    }

    /*
    void updateKeepSignedIn(boolean keepSignIn) {
        ContentValues values = new ContentValues();
        values.put(Imps.Account.KEEP_SIGNED_IN, keepSignIn ? 1 : 0);
        getContentResolver().update(mAccountUri, values, null, null);
    }*/

    void updateWidgetState() {
        boolean goodUsername = mEditUserAccount.getText().length() > 0;
        boolean goodPassword = mEditPass.getText().length() > 0;
        boolean hasNameAndPassword = goodUsername && goodPassword;

        mEditPass.setEnabled(goodUsername);
        mEditPass.setFocusable(goodUsername);
        mEditPass.setFocusableInTouchMode(goodUsername);

        // enable keep sign in only when remember password is checked.
        boolean rememberPass = mRememberPass.isChecked();
        if (rememberPass && !hasNameAndPassword) {
            mRememberPass.setChecked(false);
            rememberPass = false;
        }
        mRememberPass.setEnabled(hasNameAndPassword);
        mRememberPass.setFocusable(hasNameAndPassword);

        /*
        if (!rememberPass) {
            mKeepSignIn.setChecked(false);
        }
        mKeepSignIn.setEnabled(rememberPass);
        mKeepSignIn.setFocusable(rememberPass);
        */

        mBtnSignIn.setEnabled(hasNameAndPassword);
        mBtnSignIn.setFocusable(hasNameAndPassword);
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int before, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int after) {
            updateWidgetState();
        }

        public void afterTextChanged(Editable s) {
        }
    };

    private void showAdvanced() {
    	// if using Tor, disable DNS SRV to reduce anonymity leaks
    	if (!mHaveSetUseTor && mUseTor.isChecked()) {
    		final Imps.ProviderSettings.QueryMap settings = new Imps.ProviderSettings.QueryMap(
    				getContentResolver(),
    				mProviderId,
    				false /* don't keep updated */,
    				null /* no handler */);
    		settings.setDoDnsSrv(false);
    		mHaveSetUseTor = true;
    		settings.setUseTor(mUseTor.isChecked());
    		settings.close();
    	}
    	Intent intent = new Intent(this, AccountSettingsActivity.class);
    	intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
    	startActivity(intent);
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_settings_menu, menu);
        
        if(isEdit) {
        	//add delete menu option
        	
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_account_settings:
            Intent intent = new Intent(this, AccountSettingsActivity.class);
            //Intent intent = new Intent(this, SettingActivity.class);
            intent.putExtra(ImServiceConstants.EXTRA_INTENT_PROVIDER_ID, mProviderId);
            startActivity(intent);
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    */
}

