/*
 * Autopsy Forensic Browser
 * 
 * Copyright 2011-2016 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.keywordsearch;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;
import org.sleuthkit.autopsy.corecomponents.OptionsPanel;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.ingest.IngestManager;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import static javax.swing.SwingConstants.CENTER;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.openide.util.NbBundle.Messages;

/**
 * GlobalEditListPanel widget to manage keywords in lists
 */
class GlobalEditListPanel extends javax.swing.JPanel implements ListSelectionListener, OptionsPanel {

    private static final Logger logger = Logger.getLogger(GlobalEditListPanel.class.getName());
    private static final long serialVersionUID = 1L;
    private final KeywordTableModel tableModel;
    private KeywordList currentKeywordList;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Creates new form GlobalEditListPanel
     */
    GlobalEditListPanel() {
        tableModel = new KeywordTableModel();
        initComponents();
        customizeComponents();
    }

    private void customizeComponents() {
        chRegex.setToolTipText(NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.customizeComponents.kwReToolTip"));
        addWordButton.setToolTipText((NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.customizeComponents.addWordToolTip")));
        addWordField.setToolTipText(NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.customizeComponents.enterNewWordToolTip"));
        exportButton.setToolTipText(NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.customizeComponents.exportToFile"));
        saveListButton.setToolTipText(NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.customizeComponents.saveCurrentWIthNewNameToolTip"));
        deleteWordButton.setToolTipText(NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.customizeComponents.removeSelectedMsg"));

        keywordTable.getParent().setBackground(keywordTable.getBackground());
        final int width = jScrollPane1.getPreferredSize().width;
        keywordTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        TableColumn column;
        for (int i = 0; i < keywordTable.getColumnCount(); i++) {
            column = keywordTable.getColumnModel().getColumn(i);
            if (i == 0) {
                column.setPreferredWidth(((int) (width * 0.90)));
            } else {
                column.setPreferredWidth(((int) (width * 0.10)));
                column.setCellRenderer(new CheckBoxRenderer());
                column.setHeaderRenderer(new HeaderRenderer(keywordTable));
            }
        }
        keywordTable.setCellSelectionEnabled(false);
        keywordTable.setRowSelectionAllowed(true);

        final ListSelectionModel lsm = keywordTable.getSelectionModel();
        lsm.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (lsm.isSelectionEmpty() || currentKeywordList.isLocked() || IngestManager.getInstance().isIngestRunning()) {
                    deleteWordButton.setEnabled(false);
                } else {
                    deleteWordButton.setEnabled(true);
                }
            }
        });

        setButtonStates();

        addWordField.setComponentPopupMenu(rightClickMenu);
        ActionListener actList = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenuItem jmi = (JMenuItem) e.getSource();
                if (jmi.equals(cutMenuItem)) {
                    addWordField.cut();
                } else if (jmi.equals(copyMenuItem)) {
                    addWordField.copy();
                } else if (jmi.equals(pasteMenuItem)) {
                    addWordField.paste();
                } else if (jmi.equals(selectAllMenuItem)) {
                    addWordField.selectAll();
                }
            }
        };
        cutMenuItem.addActionListener(actList);
        copyMenuItem.addActionListener(actList);
        pasteMenuItem.addActionListener(actList);
        selectAllMenuItem.addActionListener(actList);

        setButtonStates();

        IngestManager.getInstance().addIngestJobEventListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Object source = evt.getSource();
                if (source instanceof String && ((String) source).equals("LOCAL")) { //NON-NLS
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setButtonStates();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    void setButtonStates() {
        boolean isIngestRunning = IngestManager.getInstance().isIngestRunning();
        boolean isListSelected = currentKeywordList != null;

        // items that only need a selected list
        boolean canEditList = ((isListSelected == true) && (isIngestRunning == false));
        ingestMessagesCheckbox.setEnabled(canEditList);
        ingestMessagesCheckbox.setSelected(currentKeywordList != null && currentKeywordList.getIngestMessages());
        listOptionsLabel.setEnabled(canEditList);
        listOptionsSeparator.setEnabled(canEditList);

        // items that need an unlocked list w/out ingest running
        boolean isListLocked = ((isListSelected == false) || (currentKeywordList.isLocked()));
        boolean canAddWord = isListSelected && !isIngestRunning && !isListLocked;
        addWordButton.setEnabled(canAddWord);
        addWordField.setEnabled(canAddWord);
        chRegex.setEnabled(canAddWord);
        keywordOptionsLabel.setEnabled(canAddWord);
        keywordOptionsSeparator.setEnabled(canAddWord);
        deleteListButton.setEnabled(canAddWord);

        // items that need a non-empty list
        if ((currentKeywordList == null) || (currentKeywordList.getKeywords().isEmpty())) {
            saveListButton.setEnabled(false);
            exportButton.setEnabled(false);
            deleteWordButton.setEnabled(false);
        } else {
            saveListButton.setEnabled(true);
            exportButton.setEnabled(true);
            // We do not set deleteWordButton because it will be set by the list select model code when a word is selected.
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        rightClickMenu = new javax.swing.JPopupMenu();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        selectAllMenuItem = new javax.swing.JMenuItem();
        listEditorPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        keywordTable = new javax.swing.JTable();
        addKeywordPanel = new javax.swing.JPanel();
        addWordButton = new javax.swing.JButton();
        addWordField = new javax.swing.JTextField();
        chRegex = new javax.swing.JCheckBox();
        deleteWordButton = new javax.swing.JButton();
        ingestMessagesCheckbox = new javax.swing.JCheckBox();
        keywordsLabel = new javax.swing.JLabel();
        keywordOptionsLabel = new javax.swing.JLabel();
        listOptionsLabel = new javax.swing.JLabel();
        keywordOptionsSeparator = new javax.swing.JSeparator();
        listOptionsSeparator = new javax.swing.JSeparator();
        deleteListButton = new javax.swing.JButton();
        saveListButton = new javax.swing.JButton();
        exportButton = new javax.swing.JButton();

        cutMenuItem.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.cutMenuItem.text")); // NOI18N
        rightClickMenu.add(cutMenuItem);

        copyMenuItem.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.copyMenuItem.text")); // NOI18N
        rightClickMenu.add(copyMenuItem);

        pasteMenuItem.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.pasteMenuItem.text")); // NOI18N
        rightClickMenu.add(pasteMenuItem);

        selectAllMenuItem.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.selectAllMenuItem.text")); // NOI18N
        rightClickMenu.add(selectAllMenuItem);

        setMinimumSize(new java.awt.Dimension(0, 0));

        listEditorPanel.setMinimumSize(new java.awt.Dimension(0, 0));

        jScrollPane1.setPreferredSize(new java.awt.Dimension(340, 300));

        keywordTable.setModel(tableModel);
        keywordTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        keywordTable.setGridColor(new java.awt.Color(153, 153, 153));
        keywordTable.setMaximumSize(new java.awt.Dimension(30000, 30000));
        keywordTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(keywordTable);

        addWordButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/sleuthkit/autopsy/keywordsearch/new16.png"))); // NOI18N
        addWordButton.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.addWordButton.text")); // NOI18N
        addWordButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addWordButtonActionPerformed(evt);
            }
        });

        addWordField.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.addWordField.text")); // NOI18N
        addWordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addWordFieldActionPerformed(evt);
            }
        });

        chRegex.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.chRegex.text")); // NOI18N

        deleteWordButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/sleuthkit/autopsy/keywordsearch/delete16.png"))); // NOI18N
        deleteWordButton.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.deleteWordButton.text")); // NOI18N
        deleteWordButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteWordButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout addKeywordPanelLayout = new javax.swing.GroupLayout(addKeywordPanel);
        addKeywordPanel.setLayout(addKeywordPanelLayout);
        addKeywordPanelLayout.setHorizontalGroup(
            addKeywordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addKeywordPanelLayout.createSequentialGroup()
                .addGroup(addKeywordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(addKeywordPanelLayout.createSequentialGroup()
                        .addComponent(addWordField, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(addWordButton))
                    .addComponent(chRegex, javax.swing.GroupLayout.Alignment.LEADING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deleteWordButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        addKeywordPanelLayout.setVerticalGroup(
            addKeywordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addKeywordPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(addKeywordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addWordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addWordButton)
                    .addComponent(deleteWordButton))
                .addGap(6, 6, 6)
                .addComponent(chRegex)
                .addGap(43, 43, 43))
        );

        ingestMessagesCheckbox.setSelected(true);
        ingestMessagesCheckbox.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.ingestMessagesCheckbox.text")); // NOI18N
        ingestMessagesCheckbox.setToolTipText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.ingestMessagesCheckbox.toolTipText")); // NOI18N
        ingestMessagesCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ingestMessagesCheckboxActionPerformed(evt);
            }
        });

        keywordsLabel.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.keywordsLabel.text")); // NOI18N

        keywordOptionsLabel.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.keywordOptionsLabel.text")); // NOI18N

        listOptionsLabel.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.listOptionsLabel.text")); // NOI18N

        deleteListButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/sleuthkit/autopsy/keywordsearch/delete16.png"))); // NOI18N
        deleteListButton.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.deleteListButton.text")); // NOI18N
        deleteListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteListButtonActionPerformed(evt);
            }
        });

        saveListButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/sleuthkit/autopsy/keywordsearch/save16.png"))); // NOI18N
        saveListButton.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.saveListButton.text")); // NOI18N

        exportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/sleuthkit/autopsy/keywordsearch/export16.png"))); // NOI18N
        exportButton.setText(org.openide.util.NbBundle.getMessage(GlobalEditListPanel.class, "KeywordSearchEditListPanel.exportButton.text")); // NOI18N
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout listEditorPanelLayout = new javax.swing.GroupLayout(listEditorPanel);
        listEditorPanel.setLayout(listEditorPanelLayout);
        listEditorPanelLayout.setHorizontalGroup(
            listEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(listEditorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(listEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(listEditorPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(addKeywordPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(listEditorPanelLayout.createSequentialGroup()
                        .addGroup(listEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(listEditorPanelLayout.createSequentialGroup()
                                .addComponent(listOptionsLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(listOptionsSeparator))
                            .addGroup(listEditorPanelLayout.createSequentialGroup()
                                .addComponent(keywordOptionsLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(keywordOptionsSeparator))
                            .addGroup(listEditorPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(listEditorPanelLayout.createSequentialGroup()
                                .addGroup(listEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(keywordsLabel)
                                    .addGroup(listEditorPanelLayout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addGroup(listEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(ingestMessagesCheckbox)
                                            .addGroup(listEditorPanelLayout.createSequentialGroup()
                                                .addComponent(exportButton)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(saveListButton)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(deleteListButton)))))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())))
        );
        listEditorPanelLayout.setVerticalGroup(
            listEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, listEditorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(keywordsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                .addGap(10, 10, 10)
                .addGroup(listEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(listEditorPanelLayout.createSequentialGroup()
                        .addGroup(listEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(keywordOptionsSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 7, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(keywordOptionsLabel))
                        .addGap(7, 7, 7)
                        .addComponent(addKeywordPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(listOptionsLabel))
                    .addGroup(listEditorPanelLayout.createSequentialGroup()
                        .addGap(123, 123, 123)
                        .addComponent(listOptionsSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ingestMessagesCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(listEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportButton)
                    .addComponent(saveListButton)
                    .addComponent(deleteListButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(listEditorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(listEditorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(5, 5, 5))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addWordButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addWordButtonActionPerformed
        String newWord = addWordField.getText().trim();
        boolean isLiteral = !chRegex.isSelected();
        final Keyword keyword = new Keyword(newWord, isLiteral);

        if (newWord.equals("")) {
            return;
        } else if (currentKeywordList.hasKeyword(keyword)) {
            KeywordSearchUtil.displayDialog(NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.newKwTitle"),
                    NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.addWordButtonAction.kwAlreadyExistsMsg"), KeywordSearchUtil.DIALOG_MESSAGE_TYPE.INFO);
            return;
        }

        //check if valid
        boolean valid = true;
        try {
            Pattern.compile(newWord);
        } catch (PatternSyntaxException ex1) {
            valid = false;
        } catch (IllegalArgumentException ex2) {
            valid = false;
        }
        if (!valid) {
            KeywordSearchUtil.displayDialog(NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.newKwTitle"),
                    NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.invalidKwMsg"), KeywordSearchUtil.DIALOG_MESSAGE_TYPE.ERROR);
            return;
        }

        //add & reset checkbox
        tableModel.addKeyword(keyword);
        XmlKeywordSearchList.getCurrent().addList(currentKeywordList);
        chRegex.setSelected(false);
        addWordField.setText("");
        pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, null, null);

        setButtonStates();
    }//GEN-LAST:event_addWordButtonActionPerformed

    private void deleteWordButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteWordButtonActionPerformed
        if (KeywordSearchUtil.displayConfirmDialog(NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.removeKwMsg"), NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.deleteWordButtonActionPerformed.delConfirmMsg"), KeywordSearchUtil.DIALOG_MESSAGE_TYPE.WARN)) {

            tableModel.deleteSelected(keywordTable.getSelectedRows());
            XmlKeywordSearchList.getCurrent().addList(currentKeywordList);
            setButtonStates();
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, null, null);
        }
    }//GEN-LAST:event_deleteWordButtonActionPerformed

    private void addWordFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addWordFieldActionPerformed
        addWordButtonActionPerformed(evt);
    }//GEN-LAST:event_addWordFieldActionPerformed

    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed

        final String FEATURE_NAME = NbBundle.getMessage(this.getClass(),
                "KeywordSearchEditListPanel.exportButtonAction.featureName.text");

        JFileChooser chooser = new JFileChooser();
        final String EXTENSION = "xml"; //NON-NLS
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.exportButtonActionPerformed.fileFilterLabel"), EXTENSION);
        chooser.setFileFilter(filter);
        chooser.setSelectedFile(new File(currentKeywordList.getName()));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selFile = chooser.getSelectedFile();
            if (selFile == null) {
                return;
            }

            //force append extension if not given
            String fileAbs = selFile.getAbsolutePath();
            if (!fileAbs.endsWith("." + EXTENSION)) {
                fileAbs = fileAbs + "." + EXTENSION;
                selFile = new File(fileAbs);
            }

            boolean shouldWrite = true;
            if (selFile.exists()) {
                shouldWrite = KeywordSearchUtil.displayConfirmDialog(FEATURE_NAME,
                        NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.exportButtonActionPerformed.fileExistPrompt",
                                selFile.getName()), KeywordSearchUtil.DIALOG_MESSAGE_TYPE.WARN);
            }
            if (!shouldWrite) {
                return;
            }

            XmlKeywordSearchList reader = XmlKeywordSearchList.getCurrent();

            List<KeywordList> toWrite = new ArrayList<>();
            toWrite.add(reader.getList(currentKeywordList.getName()));
            final XmlKeywordSearchList exporter = new XmlKeywordSearchList(fileAbs);
            boolean written = exporter.saveLists(toWrite);
            if (written) {
                KeywordSearchUtil.displayDialog(FEATURE_NAME,
                        NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.exportButtonActionPerformed.kwListExportedMsg"),
                        KeywordSearchUtil.DIALOG_MESSAGE_TYPE.INFO);
            }
        }
    }//GEN-LAST:event_exportButtonActionPerformed

    private void ingestMessagesCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ingestMessagesCheckboxActionPerformed
        currentKeywordList.setIngestMessages(ingestMessagesCheckbox.isSelected());
        XmlKeywordSearchList updater = XmlKeywordSearchList.getCurrent();
        updater.addList(currentKeywordList);
        pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, null, null);
    }//GEN-LAST:event_ingestMessagesCheckboxActionPerformed

    private void deleteListButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteListButtonActionPerformed
        pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, null, null);
    }//GEN-LAST:event_deleteListButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel addKeywordPanel;
    private javax.swing.JButton addWordButton;
    private javax.swing.JTextField addWordField;
    private javax.swing.JCheckBox chRegex;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JButton deleteListButton;
    private javax.swing.JButton deleteWordButton;
    private javax.swing.JButton exportButton;
    private javax.swing.JCheckBox ingestMessagesCheckbox;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel keywordOptionsLabel;
    private javax.swing.JSeparator keywordOptionsSeparator;
    private javax.swing.JTable keywordTable;
    private javax.swing.JLabel keywordsLabel;
    private javax.swing.JPanel listEditorPanel;
    private javax.swing.JLabel listOptionsLabel;
    private javax.swing.JSeparator listOptionsSeparator;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JPopupMenu rightClickMenu;
    private javax.swing.JButton saveListButton;
    private javax.swing.JMenuItem selectAllMenuItem;
    // End of variables declaration//GEN-END:variables

    @Override
    public void valueChanged(ListSelectionEvent e) {
        //respond to list selection changes in KeywordSearchListManagementPanel
        ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();
        if (!listSelectionModel.isSelectionEmpty()) {
            int index = listSelectionModel.getMinSelectionIndex();

            listSelectionModel.setSelectionInterval(index, index);
            XmlKeywordSearchList loader = XmlKeywordSearchList.getCurrent();

            currentKeywordList = loader.getListsL(false).get(index);
            tableModel.resync();
            setButtonStates();
        } else {
            currentKeywordList = null;
            tableModel.resync();
            setButtonStates();
        }
    }

    @Override
    public void store() {
        // Implemented by parent panel
    }

    @Override
    public void load() {
        // Implemented by parent panel
    }

    KeywordList getCurrentKeywordList() {
        return currentKeywordList;
    }

    void setCurrentKeywordList(KeywordList list) {
        currentKeywordList = list;
    }

    void addDeleteButtonActionPerformed(ActionListener l) {
        deleteListButton.addActionListener(l);
    }

    void addSaveButtonActionPerformed(ActionListener l) {
        saveListButton.addActionListener(l);
    }

    private class KeywordTableModel extends AbstractTableModel {

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return currentKeywordList == null ? 0 : currentKeywordList.getKeywords().size();
        }

        @Override
        public String getColumnName(int column) {
            String colName = null;

            switch (column) {
                case 0:
                    colName = NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.kwColName");
                    break;
                case 1:
                    colName = NbBundle.getMessage(this.getClass(), "KeywordSearchEditListPanel.exportButtonActionPerformed.regExColName");
                    break;
                default:
                    ;

            }
            return colName;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object ret = null;
            if (currentKeywordList == null) {
                return "";
            }
            Keyword word = currentKeywordList.getKeywords().get(rowIndex);
            switch (columnIndex) {
                case 0:
                    ret = (Object) word.getQuery();
                    break;
                case 1:
                    ret = (Object) !word.isLiteral();
                    break;
                default:
                    logger.log(Level.SEVERE, "Invalid table column index: {0}", columnIndex); //NON-NLS
                    break;
            }
            return ret;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        void addKeyword(Keyword keyword) {
            if (!currentKeywordList.hasKeyword(keyword)) {
                currentKeywordList.getKeywords().add(keyword);
            }
            fireTableDataChanged();
        }

        void resync() {
            fireTableDataChanged();
        }

        //delete selected from handle, events are fired from the handle
        void deleteSelected(int[] selected) {
            List<Keyword> words = currentKeywordList.getKeywords();
            Arrays.sort(selected);
            for (int arrayi = selected.length - 1; arrayi >= 0; arrayi--) {
                words.remove(selected[arrayi]);
            }
            resync();
        }
    }

    /**
     * A cell renderer for boolean cells that shows a center-aligned green check
     * mark if true, nothing if false.
     */
    private class CheckBoxRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;
        final ImageIcon theCheck = new javax.swing.ImageIcon(getClass().getResource("/org/sleuthkit/autopsy/keywordsearch/checkmark.png")); // NON-NLS

        CheckBoxRenderer() {
            setHorizontalAlignment(CENTER);
        }

        @Override
        @Messages("IsRegularExpression=Keyword is a regular expression")
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if ((value instanceof Boolean)) {
                if ((Boolean) value) {
                    setIcon(theCheck);
                    setToolTipText(Bundle.IsRegularExpression());
                } else {
                    setIcon(null);
                    setToolTipText(null);
                }
            }
            return this;
        }
    }

    /**
     * A cell renderer for header cells that center-aligns the header text.
     */
    private static class HeaderRenderer implements TableCellRenderer {

        private DefaultTableCellRenderer renderer;

        public HeaderRenderer(JTable table) {
            renderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
            renderer.setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int col) {
            return renderer.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
        }
    }
}
