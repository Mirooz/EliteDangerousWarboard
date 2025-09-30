package be.mirooz.elitedangerous.dashboard.ui;

import be.mirooz.elitedangerous.dashboard.controller.FooterController;
import be.mirooz.elitedangerous.dashboard.controller.HeaderController;
import be.mirooz.elitedangerous.dashboard.controller.MissionListController;

public class UIRefreshManager {
    private static final UIRefreshManager INSTANCE = new UIRefreshManager();

    private HeaderController headerController;
    private MissionListController missionListController;
    private FooterController footerController;


    private UIRefreshManager() {}

    public static UIRefreshManager getInstance() {
        return INSTANCE;
    }

    public void registerControllers(HeaderController header, MissionListController missionList, FooterController footer) {
        this.headerController = header;
        this.missionListController = missionList;
        this.footerController = footer;
    }


    public void refresh() {
        if (missionListController != null) missionListController.applyFilter();
        if (headerController != null) headerController.updateStats();
        if (footerController != null) footerController.updateFactionStats();
    }
}
