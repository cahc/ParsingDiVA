package org.cc.NorskaModellen;

/**
 * Created by crco0001 on 7/5/2016.
 */
public class StatusInModel {

    /**
    defaults

     */
    private boolean ignorerad = false;
    private String statusInModel = StatusInModelConstants.BEAKTAD_ÄNNU_EJ_MATCHAD_MOT_NORSKA_LISTAN; //uppdatera senare


    public boolean isIgnorerad() {
        return ignorerad;
    }

    public void setIgnorerad(boolean ignorerad) {
        this.ignorerad = ignorerad;
    }

    public String getStatusInModel() {
        return statusInModel;
    }

    public void setStatusInModel(String status) {
        this.statusInModel = status;
    }
}
