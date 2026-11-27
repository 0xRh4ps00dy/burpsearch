package com.rh4ps00dy.burpsearch.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.http.message.HttpRequestResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SearchPanel extends JPanel {
    private final MontoyaApi api;
    private final JTextField searchField;
    private final JButton searchButton;
    private final JButton clearButton;
    private final JCheckBox regexCheckBox;
    private final JCheckBox caseSensitiveCheckBox;
    private final JCheckBox negativeMatchCheckBox;
    private final JCheckBox inScopeOnlyCheckBox;

    private final JCheckBox reqHeadersCheckBox;
    private final JCheckBox reqBodyCheckBox;
    private final JCheckBox resHeadersCheckBox;
    private final JCheckBox resBodyCheckBox;

    private final JCheckBox targetCheckBox;
    private final JCheckBox proxyCheckBox;
    
    private final JLabel statusLabel;
    
    private final JTable resultsTable;
    private final ResultsTableModel tableModel;
    private final HttpRequestEditor requestEditor;
    private final HttpResponseEditor responseEditor;
    
    private volatile boolean isSearching = false;
    private Thread searchThread;

    public SearchPanel(MontoyaApi api) {
        this.api = api;
        this.setLayout(new BorderLayout());

        // Top Panel with 4 groups
        JPanel topPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Group 1: Search
        JPanel searchGroup = new JPanel(new GridBagLayout());
        searchGroup.setBorder(BorderFactory.createTitledBorder("Search"));
        searchField = new JTextField();
        searchButton = new JButton("Go");
        clearButton = new JButton("Clear");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
        searchGroup.add(searchField, gbc);
        gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        searchButtons.add(clearButton);
        searchButtons.add(searchButton);
        searchGroup.add(searchButtons, gbc);

        // Group 2: Options
        JPanel optionsGroup = new JPanel(new GridLayout(3, 2));
        optionsGroup.setBorder(BorderFactory.createTitledBorder("Options"));
        caseSensitiveCheckBox = new JCheckBox("Case sensitive");
        regexCheckBox = new JCheckBox("Regex");
        negativeMatchCheckBox = new JCheckBox("Negative match");
        inScopeOnlyCheckBox = new JCheckBox("In-scope only");
        optionsGroup.add(caseSensitiveCheckBox);
        optionsGroup.add(inScopeOnlyCheckBox);
        optionsGroup.add(regexCheckBox);
        optionsGroup.add(negativeMatchCheckBox);

        // Group 3: Locations
        JPanel locationsGroup = new JPanel(new GridLayout(2, 2));
        locationsGroup.setBorder(BorderFactory.createTitledBorder("Locations"));
        reqHeadersCheckBox = new JCheckBox("Request headers", true);
        resHeadersCheckBox = new JCheckBox("Response headers", true);
        reqBodyCheckBox = new JCheckBox("Request body", true);
        resBodyCheckBox = new JCheckBox("Response body", true);
        locationsGroup.add(reqHeadersCheckBox);
        locationsGroup.add(resHeadersCheckBox);
        locationsGroup.add(reqBodyCheckBox);
        locationsGroup.add(resBodyCheckBox);

        // Group 4: Tools
        JPanel toolsGroup = new JPanel(new GridLayout(2, 1));
        toolsGroup.setBorder(BorderFactory.createTitledBorder("Tools"));
        targetCheckBox = new JCheckBox("Target", false);
        proxyCheckBox = new JCheckBox("Proxy", true);
        toolsGroup.add(targetCheckBox);
        toolsGroup.add(proxyCheckBox);

        topPanel.add(searchGroup);
        topPanel.add(optionsGroup);
        topPanel.add(locationsGroup);
        topPanel.add(toolsGroup);

        this.add(topPanel, BorderLayout.NORTH);

        // Results Table
        tableModel = new ResultsTableModel();
        resultsTable = new JTable(tableModel);
        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(resultsTable);

        // Editors side-by-side
        requestEditor = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        responseEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
        
        JPanel editorsPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        JPanel reqPanel = new JPanel(new BorderLayout());
        reqPanel.add(new JLabel("Request"), BorderLayout.NORTH);
        reqPanel.add(requestEditor.uiComponent(), BorderLayout.CENTER);
        JPanel resPanel = new JPanel(new BorderLayout());
        resPanel.add(new JLabel("Response"), BorderLayout.NORTH);
        resPanel.add(responseEditor.uiComponent(), BorderLayout.CENTER);
        
        editorsPanel.add(reqPanel);
        editorsPanel.add(resPanel);

        // Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, editorsPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5);
        this.add(splitPane, BorderLayout.CENTER);

        // Column widths
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // Source
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(150);  // Host
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(1000); // URL (Very wide)
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(80);   // Status code
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(80);   // Length
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(160);  // Time requested

        // Bottom status
        statusLabel = new JLabel(" 0 results");
        this.add(statusLabel, BorderLayout.SOUTH);

        // Actions
        searchButton.addActionListener(e -> {
            if (isSearching) {
                stopSearch();
            } else {
                performSearch();
            }
        });
        searchField.addActionListener(e -> {
            if (!isSearching) performSearch();
        });
        clearButton.addActionListener(e -> {
            searchField.setText("");
            tableModel.clear();
            requestEditor.setRequest(null);
            responseEditor.setResponse(null);
            statusLabel.setText(" 0 results");
        });

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = resultsTable.getSelectedRow();
            if (selectedRow >= 0) {
                SearchResult item = tableModel.getItem(resultsTable.convertRowIndexToModel(selectedRow));
                requestEditor.setRequest(item.request);
                responseEditor.setResponse(item.response);
                
                String query = searchField.getText();
                if (!query.isEmpty() && !regexCheckBox.isSelected()) {
                    requestEditor.setSearchExpression(query);
                    responseEditor.setSearchExpression(query);
                }
            }
        });
    }

    private void stopSearch() {
        isSearching = false;
        if (searchThread != null) {
            searchThread.interrupt();
        }
        searchButton.setText("Go");
        searchButton.setEnabled(true);
    }

    private void performSearch() {
        String query = searchField.getText();
        if (query.isEmpty()) return;

        isSearching = true;
        searchButton.setText("Stop");
        tableModel.clear();
        statusLabel.setText(" Initializing search...");

        boolean isRegex = regexCheckBox.isSelected();
        boolean isCaseSensitive = caseSensitiveCheckBox.isSelected();
        boolean isNegative = negativeMatchCheckBox.isSelected();
        boolean isInScopeOnly = inScopeOnlyCheckBox.isSelected();
        
        boolean searchReqBody = reqBodyCheckBox.isSelected();
        boolean searchResBody = resBodyCheckBox.isSelected();
        boolean searchReqHeaders = reqHeadersCheckBox.isSelected();
        boolean searchResHeaders = resHeadersCheckBox.isSelected();

        boolean useProxy = proxyCheckBox.isSelected();
        boolean useTarget = targetCheckBox.isSelected();

        searchThread = new Thread(() -> {
            try {
                Pattern pattern;
                if (isRegex) {
                    pattern = Pattern.compile(query, isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                } else {
                    pattern = Pattern.compile(Pattern.quote(query), isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                }

                List<SearchResult> results = new ArrayList<>();
                
                if (useProxy && isSearching) {
                    List<ProxyHttpRequestResponse> history = api.proxy().history();
                    int total = history.size();
                    for (int i = 0; i < total; i++) {
                        if (!isSearching) break;
                        if (i % 100 == 0) {
                            final int current = i;
                            SwingUtilities.invokeLater(() -> statusLabel.setText(" Searching Proxy... (" + current + "/" + total + ")"));
                        }
                        
                        ProxyHttpRequestResponse item = history.get(i);
                        if (isInScopeOnly && !api.scope().isInScope(item.request().url())) continue;
                        if (matchItem(item.request(), item.hasResponse() ? item.response() : null, pattern, searchReqBody, searchResBody, searchReqHeaders, searchResHeaders) ^ isNegative) {
                            results.add(new SearchResult("Proxy", item.request(), item.hasResponse() ? item.response() : null, item.time()));
                        }
                    }
                }

                if (useTarget && isSearching) {
                    List<HttpRequestResponse> sitemap = api.siteMap().requestResponses();
                    int total = sitemap.size();
                    for (int i = 0; i < total; i++) {
                        if (!isSearching) break;
                        if (i % 100 == 0) {
                            final int current = i;
                            SwingUtilities.invokeLater(() -> statusLabel.setText(" Searching Target... (" + current + "/" + total + ")"));
                        }

                        HttpRequestResponse item = sitemap.get(i);
                        if (isInScopeOnly && !api.scope().isInScope(item.request().url())) continue;
                        if (matchItem(item.request(), item.hasResponse() ? item.response() : null, pattern, searchReqBody, searchResBody, searchReqHeaders, searchResHeaders) ^ isNegative) {
                            results.add(new SearchResult("Target", item.request(), item.hasResponse() ? item.response() : null, null));
                        }
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    tableModel.addItems(results);
                    statusLabel.setText(" " + results.size() + " results" + (isSearching ? "" : " (Stopped)"));
                });
            } catch (Throwable t) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Error during search: " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                isSearching = false;
                SwingUtilities.invokeLater(() -> {
                    searchButton.setText("Go");
                    searchButton.setEnabled(true);
                });
            }
        });
        searchThread.start();
    }

    private boolean matchItem(HttpRequest req, HttpResponse res, Pattern pattern, boolean reqBody, boolean resBody, boolean reqHeaders, boolean resHeaders) {
        if (req != null) {
            if (reqHeaders) {
                // URL and Method are considered part of the request headers/line
                if (pattern.matcher(req.url()).find()) return true;
                if (pattern.matcher(req.method()).find()) return true;

                if (req.headers() != null) {
                    for (var header : req.headers()) {
                        if (pattern.matcher(header.toString()).find()) return true;
                    }
                }
            }
            
            if (reqBody && req.body() != null && req.body().length() > 0) {
                if (pattern.matcher(req.body().toString()).find()) return true;
            }
        }

        if (res != null) {
            if (resHeaders && res.headers() != null) {
                for (var header : res.headers()) {
                    if (pattern.matcher(header.toString()).find()) return true;
                }
            }
            if (resBody && res.body() != null && res.body().length() > 0) {
                if (pattern.matcher(res.body().toString()).find()) return true;
            }
        }

        return false;
    }

    private static class SearchResult {
        final String source;
        final HttpRequest request;
        final HttpResponse response;
        final String time;

        SearchResult(String source, HttpRequest request, HttpResponse response, ZonedDateTime time) {
            this.source = source;
            this.request = request;
            this.response = response;
            if (time != null) {
                this.time = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                this.time = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        }
    }

    private static class ResultsTableModel extends AbstractTableModel {
        private final List<SearchResult> items = new ArrayList<>();
        private final String[] columnNames = {"Source", "Host", "URL", "Status code", "Length", "Time requested"};

        public void clear() {
            int size = items.size();
            items.clear();
            if (size > 0) fireTableRowsDeleted(0, size - 1);
        }

        public void addItems(List<SearchResult> newItems) {
            int start = items.size();
            items.addAll(newItems);
            fireTableRowsInserted(start, items.size() - 1);
        }

        public SearchResult getItem(int row) {
            return items.get(row);
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SearchResult item = items.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> item.source;
                case 1 -> item.request.httpService().host();
                case 2 -> item.request.url();
                case 3 -> item.response != null ? item.response.statusCode() : "";
                case 4 -> item.response != null ? item.response.body().length() : 0;
                case 5 -> item.time;
                default -> "";
            };
        }
    }
}
