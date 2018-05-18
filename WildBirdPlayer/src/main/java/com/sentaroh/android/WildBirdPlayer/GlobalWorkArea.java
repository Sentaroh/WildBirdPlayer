package com.sentaroh.android.WildBirdPlayer;

import android.content.Context;

public class GlobalWorkArea {
    static private GlobalParameters gp=null;
    static public GlobalParameters getGlobalParameters(Context c) {
        if (gp ==null) {
            gp =new GlobalParameters();
            gp.initGlobalParameter(c);
        }
        return gp;
    }
}
