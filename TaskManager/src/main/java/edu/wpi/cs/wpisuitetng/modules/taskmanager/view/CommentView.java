/*******************************************************************************
 * Copyright (c) 2014 -- WPI Suite
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package edu.wpi.cs.wpisuitetng.modules.taskmanager.view;

import java.awt.Color;

import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.JXTextArea;

import net.miginfocom.swing.MigLayout;

/**
 * Part of Task editor containing comments and history
 */
public class CommentView extends JTabbedPane {

    private static final long serialVersionUID = 6161339014039149740L;

    private JPanel commentPanel = new JPanel();
    private JPanel historyPanel = new JPanel();
    private JPanel postedHistoryPanel = new JPanel();
    private JScrollPane historyScroll = new JScrollPane();
    private JScrollPane commentScroll = new JScrollPane();
    private JPanel postedCommentPanel = new JPanel();
    private JScrollPane editCommentScroll = new JScrollPane();
    private JXTextArea commentText = new JXTextArea("", Color.GRAY);
    private JButton postCommentButton = new JButton("Post");
    private JButton clearCommentButton = new JButton("Clear");

    /**
     * Constructor sets up Comments and History
     * 
     * @param currentView
     *            the currentView of the Task. Either Editing, Creating, or
     *            Deleting.
     */
    public CommentView(ViewMode currentView) {
        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        this.addTab("Comments", Icons.COMMENTS, commentPanel, null);
        this.addTab("History", Icons.HISTORY, historyPanel, null);

        // Set layouts
        this.commentPanel.setLayout(new MigLayout("", "[grow]",
                "[grow][50px:n][min]"));
        this.historyPanel.setLayout(new MigLayout("", "[grow]", "[grow]"));
        this.postedCommentPanel.setLayout(new MigLayout("fill"));
        this.postedHistoryPanel.setLayout(new MigLayout("fill"));

        // Add components to commentPanel
        this.commentPanel.add(commentScroll, "cell 0 0,grow");
        this.commentPanel.add(editCommentScroll, "cell 0 1,grow");
        this.commentPanel.add(postCommentButton, "cell 0 2,alignx left,growy");
        this.commentPanel.add(clearCommentButton, "cell 0 2,alignx left,growy");
        this.commentText.setWrapStyleWord(true);
        this.commentText.setLineWrap(true);

        this.historyPanel.add(historyScroll, "cell 0 0,grow");

        // Set scrollpane viewports
        this.commentScroll.setViewportView(postedCommentPanel);
        this.editCommentScroll.setViewportView(commentText);
        this.historyScroll.setViewportView(postedHistoryPanel);
        this.postCommentButton.setEnabled(false);
        this.postCommentButton.setIcon(Icons.COMMENT);
        this.clearCommentButton.setEnabled(false);
        this.clearCommentButton.setIcon(Icons.CLEAR);
        this.setupListeners();
    }

    /**
     * If the current ViewMode is CREATING, then we disable the post and clear
     * comment buttons, and replace the background text for JTextBox. Otherwise,
     * We enable the buttons
     * 
     * @param currentView
     *            the current ViewMode
     */
    public void toggleTextField(ViewMode currentView) {
        if (currentView != null) {
            if (currentView.equals(ViewMode.CREATING)) {
                this.commentText.setEditable(false);
                this.commentText
                        .setPrompt("Comments unavailable until task created.");
                this.commentText.setPromptForeground(Color.GRAY);
                this.postCommentButton.setEnabled(false);
                this.clearCommentButton.setEnabled(false);
            } else if (currentView.equals(ViewMode.ARCHIVING)) {
                this.commentText.setEditable(false);
                this.commentText
                        .setPrompt("Comments unavailable because task is Archived.");
                this.commentText.setPromptForeground(Color.GRAY);
                this.postCommentButton.setEnabled(false);
                this.clearCommentButton.setEnabled(false);
            } else {
                this.commentText.setEditable(true);
                this.commentText.setPrompt("Write a comment...");
                this.commentText.setPromptForeground(Color.GRAY);
                this.postCommentButton.setEnabled(true);
                this.clearCommentButton.setEnabled(true);
            }
        }
    }

    /**
     * Enable or disable the post and reset buttons depending of if there's
     * something entered in the comment box.
     */
    private void validateFields() {
        if (commentText.getText().trim().isEmpty()) {
            this.postCommentButton.setEnabled(false);
            this.clearCommentButton.setEnabled(false);
        } else {
            this.postCommentButton.setEnabled(true);
            this.clearCommentButton.setEnabled(true);
        }
    }

    /**
     * Posts the new activity to the view
     * 
     * @param newComment
     *            the activity to be posted, either auto generated or manually
     *            added
     */
    public void postActivity(ActivityView newComment) {
        postedCommentPanel.add(newComment, "dock south");
        commentText.setText("");
        JScrollBar vertical = commentScroll.getVerticalScrollBar();
        JScrollBar horizontal = commentScroll.getHorizontalScrollBar();
        postedCommentPanel.revalidate();
        postedCommentPanel.repaint();
        vertical.setValue(vertical.getMinimum());
        horizontal.setValue(horizontal.getMinimum());
        validateFields();
    }

    /**
     * Posts the new activity to the history panel view
     * 
     * @param newComment
     *            the activity to be posted, auto generated
     */
    public void postHistory(ActivityView newComment) {
        postedHistoryPanel.add(newComment, "dock south");
        JScrollBar vertical = historyScroll.getVerticalScrollBar();
        JScrollBar horizontal = historyScroll.getHorizontalScrollBar();
        postedHistoryPanel.revalidate();
        postedHistoryPanel.repaint();
        vertical.setValue(vertical.getMinimum());
        horizontal.setValue(horizontal.getMinimum());
    }

    /**
     * Sets up the button listeners so that buttons can do things.
     */
    private void setupListeners() {
        // Clear button (resets comment)
        this.clearCommentButton.addActionListener((ActionEvent) -> {
            commentText.setText("");
            validateFields();
        });

        this.commentText.getDocument().addDocumentListener(
                new DocumentListener() {

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        validateFields();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        validateFields();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent arg0) {
                        validateFields();
                    }
                });
    }

    /**
     * Adds ActionListener to post comment button
     * 
     * @param listener
     *            triggers post
     */
    public void addOnPostListener(ActionListener listener) {
        this.postCommentButton.addActionListener(listener);
    }

    /**
     * @return the commentText
     */
    public JXTextArea getCommentText() {
        return this.commentText;
    }

    /**
     * revalidates and repaints the history Panel
     */
    public void revalidateHistoryPanel() {
        this.postedHistoryPanel.revalidate();
        this.postedHistoryPanel.repaint();
    }

    /**
     * Clears the posts in the view
     */
    public void clearPosts() {
        postedCommentPanel.removeAll();
        postedHistoryPanel.removeAll();
    }

    /**
     * @return the posted comment panel for testing
     */
    public JPanel getPostedCommentPanel() {
        return this.postedCommentPanel;
    }

    /**
     * @return the posted history panel for testing
     */
    public JPanel getPostedHistoryPanel() {
        return this.postedHistoryPanel;
    }

    /**
     * Sets the comment text to a specified string, for testing
     * 
     * @param string
     *            the string to put into the comment text
     */
    public void setCommentText(String string) {
        this.commentText.setText(string);
    }

    /**
     * @return the clear button for testing
     */
    public JButton getClearButton() {
        return this.clearCommentButton;
    }
}
