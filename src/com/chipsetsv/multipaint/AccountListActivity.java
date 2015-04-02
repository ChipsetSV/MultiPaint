package com.chipsetsv.multipaint;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.chipsetsv.multipaint.connection.xmpp.XMPPConnectionHelper;

public class AccountListActivity extends ListActivity {
	protected AccountManager accountManager;
    protected Intent intent;
    XMPPConnectionHelper helper = new XMPPConnectionHelper();
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		accountManager = AccountManager.get(getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType("com.google");
        this.setListAdapter(new ArrayAdapter<Account>(this, R.layout.activity_account_list, accounts)); 
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Account account = (Account)getListView().getItemAtPosition(position);
		
		if (!helper.getConnected()) { 
			//helper.InitializeGTalk(accountManager, account, "chipset89@gmail.com");
			helper.InitializeXMPPStandart("chipset1989", "pikalapasum", "chipset89@gmail.com");
		}
		else {
			helper.sendMessage("Hello");
		}
	}
	
	
}
