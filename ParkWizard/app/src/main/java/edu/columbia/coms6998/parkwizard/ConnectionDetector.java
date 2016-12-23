package edu.columbia.coms6998.parkwizard;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Administrator on 1/10/2015.
 */

public class ConnectionDetector {

    static private Context context;

    /**
     * Checks the whether the device is connected to the Internet or not
     *
     * @param ctx Pass a Context Object or getActivity()
     * @return true - If connected to Internet<br>
     * false - If not connected to Internet
     */
    public static boolean checkConnection(Context ctx) {
        context = ctx;
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

}
