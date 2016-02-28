package util;

import java.util.ArrayList;

/**
 * Created by Milenko on 28/02/2016.
 */
public interface FinishScanListener {

    /**
     * Interface called when the scan method finishes. Network operations should not execute on UI thread
     */

    public void onFinishScan(ArrayList<ClientScanResult> clients);


}
