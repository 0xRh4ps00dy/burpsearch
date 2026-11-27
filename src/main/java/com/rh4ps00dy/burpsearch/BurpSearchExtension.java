package com.rh4ps00dy.burpsearch;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.rh4ps00dy.burpsearch.ui.SearchPanel;

public class BurpSearchExtension implements BurpExtension {
    private MontoyaApi api;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Burp Search Community");

        // Create the search panel
        SearchPanel searchPanel = new SearchPanel(api);

        // Register the user interface
        api.userInterface().registerSuiteTab("Burp Search", searchPanel);

        api.logging().logToOutput("Burp Search Community extension initialized.");
    }
}
