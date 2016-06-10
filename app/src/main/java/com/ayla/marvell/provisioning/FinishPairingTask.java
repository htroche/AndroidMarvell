package com.ayla.marvell.provisioning;

/**
 * Created by hugotroche on 6/9/16.
 */
public class FinishPairingTask extends PairingTask {

    @Override
    protected String doInBackground(String... uri) {
        String responseString = null;
        try {
            this.checkAccessoryStatus();

        } catch (Exception e) {
            e.printStackTrace();
            this.handler.error(e.getMessage());
        }
        return "";
    }
}
