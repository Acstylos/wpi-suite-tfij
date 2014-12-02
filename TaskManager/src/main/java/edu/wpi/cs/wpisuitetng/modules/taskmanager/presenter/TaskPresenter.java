/*******************************************************************************
 * Copyright (c) 2014 -- WPI Suite
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package edu.wpi.cs.wpisuitetng.modules.taskmanager.presenter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.wpi.cs.wpisuitetng.modules.taskmanager.model.TaskModel;
import edu.wpi.cs.wpisuitetng.modules.taskmanager.view.Icons;
import edu.wpi.cs.wpisuitetng.modules.taskmanager.view.MainView;
import edu.wpi.cs.wpisuitetng.modules.taskmanager.view.MiniTaskView;
import edu.wpi.cs.wpisuitetng.modules.taskmanager.view.TaskView;
import edu.wpi.cs.wpisuitetng.modules.taskmanager.view.ViewMode;
import edu.wpi.cs.wpisuitetng.network.Network;
import edu.wpi.cs.wpisuitetng.network.Request;
import edu.wpi.cs.wpisuitetng.network.models.HttpMethod;

/**
 * This class creates a TaskView and updates the task with new information from
 * the database. This lets you modify the task, and will let the user modify it
 * as well.
 */
public class TaskPresenter {

    /** View for the task. */
    private TaskView view;
    private MiniTaskView miniView;
    /** Model for the task. */
    private TaskModel model;
    private ViewMode viewMode;

    private BucketPresenter bucket;
    private List<ActivityPresenter> activityPresenters; 

    /**
     * Constructs a TaskPresenter for the given model. Constructs the view
     * offscreen, available if you call getView().
     * @param id
     *            ID of the bucket to create
     */
    public TaskPresenter(int id, BucketPresenter bucket, ViewMode viewMode) {
        this.bucket = bucket;
        this.viewMode = viewMode;
        this.model = new TaskModel();
        this.model.setId(id);
        this.model.setTitle("New Task");
        this.view = new TaskView(model, viewMode);
        this.miniView = new MiniTaskView(model.getShortTitle(), model.getDueDate(), model.getTitle());
        this.activityPresenters = new ArrayList<ActivityPresenter>(); 
        registerCallbacks();
    }

    /**
     * Register callbacks with the local view.
     */
    private void registerCallbacks() {
        // onclick listener to open new tabs when minitaskview is clicked
        miniView.addOnClickOpenTabView(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MainView.getInstance().addTab(model.getShortTitle(), Icons.TASK, view);//this line chooses tab title
                view.setViewMode(ViewMode.EDITING);
                viewMode = view.getViewMode();
                int tabCount = MainView.getInstance().getTabCount();
                view.setIndex(tabCount-1);
                MainView.getInstance().setSelectedIndex(tabCount - 1);
                MainView.getInstance().setToolTipTextAt(tabCount - 1, model.getTitle());

            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        
        /**
         * Open the task tab when a task is clicked
         * @param ActionListener
         */
        view.addOkOnClickListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	int index = MainView.getInstance().indexOfComponent(view);
            	if(viewMode == ViewMode.CREATING){
            	    //CREATING MODE
            		updateModel();
            		createInDatabase();
                    view.setViewMode(ViewMode.EDITING);
                    MainView.getInstance().remove(index);
                    MainView.getInstance().setSelectedIndex(0);
            	}
            	else {
            		if (view.getStatus().getSelectedIndex() + 1 != bucket.getModel().getId()) {
            			MainView.getInstance().getWorkflowPresenter().moveTask(model.getId(), view.getStatus().getSelectedIndex() + 1, bucket.getModel().getId());
            			bucket.writeModelToView();
            		}
            		saveView();
            		updateView();
            		MainView.getInstance().setTitleAt(index, model.getShortTitle());
            		bucket.writeModelToView();
            	}
            }
        });

        view.addCancelOnClickListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = MainView.getInstance().indexOfComponent(view);
                MainView.getInstance().remove(index);
                updateView();
                view.revalidate();
                view.repaint();
                miniView.revalidate();
                miniView.repaint();
                MainView.getInstance().setSelectedIndex(0);
            }
        });

        view.addClearOnClickListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateView();
                view.revalidate();
                view.repaint();
                miniView.revalidate();
                miniView.repaint();
                MainView.getInstance().setTitleAt(view.getIndex(), model.getTitle());
            }
        });

        view.addDeleteOnClickListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = MainView.getInstance().indexOfComponent(view);
                MainView.getInstance().remove(index);
                MainView.getInstance().getWorkflowPresenter().archiveTask(model.getId(), bucket.getModel().getId());
                MainView.getInstance().getArchive().getArchiveBucket().addTaskToView(miniView);
            }
        });
        
        view.getCommentView().addOnPostListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                addActivity();
            }

        });
    }

    /**
     * Creates a new activity in the Database by using the text provided in the
     * comment box
     */
    public void addActivity() {
        ActivityPresenter activityPresenter = new ActivityPresenter(this, view
                .getCommentView().getCommentText().getText());
        view.getCommentView().postActivity(activityPresenter.getView());
        activityPresenter.createInDatabase();
        activityPresenters.add(activityPresenter);
    }

    /**
     * saves an activity in the model with the given ID from the DataBase
     * @param id
     *            database given ID
     */
    public void saveActivityId(int id) {
        model.addActivityID(id);
        saveView();
    }
    
    /**
     * Create a new task in the database. Initializes an async network request
     * with an observer.
     */
    public void createInDatabase() {
        Request request = Network.getInstance().makeRequest("taskmanager/task",
                HttpMethod.PUT);
        request.setBody(this.model.toJson());
        request.addObserver(new TaskObserver(this));
        request.send();
    }

    /**
     * Save the fields entered in the view.
     */
    private void saveView() {
        updateModel();

        Request request = Network.getInstance().makeRequest("taskmanager/task",
                HttpMethod.POST);
        request.setBody(this.model.toJson());
        request.addObserver(new TaskObserver(this));
        request.send();
    }

    /**
     * Have the presenter reload the view from the model.
     */
    public void updateFromDatabase() {
        Request request = Network.getInstance().makeRequest(
                "taskmanager/task/" + this.model.getId(), HttpMethod.GET);
        request.addObserver(new TaskObserver(this));
        request.send();

    }

    /**
     * Update the model with data from the view
     */
    public void updateModel() {
        model.setTitle(view.getTaskNameField());
        model.setEstimatedEffort(view.getEstimatedEffort());
        model.setActualEffort(view.getActualEffort());
        model.setDescription(view.getDescriptionText());
        model.setDueDate(view.getDueDate());
        model.setStatus(view.getStatus().getSelectedIndex()+1);
        this.bucket = MainView.getInstance().getWorkflowPresenter().getBucket(view.getStatus().getSelectedIndex()+1);
    }

    /**
     * Update the view with data from the model
     */
    public void updateView() {
        view.setStatus(model.getStatus());
        view.setModel(model);
        miniView.setTaskName(model.getShortTitle(), model.getTitle());
        miniView.setDueDate(model.getDueDate());
        miniView.setToolTipText(model.getTitle());
        updateCommentView();
    }
    
    /**
     * takes the current comment view, clears the posts, and puts each comment,
     * one by one back on to the current view.
     */
    public void updateCommentView() {
        view.getCommentView().clearPosts();
        for (ActivityPresenter p : activityPresenters) {
            view.getCommentView().postActivity(p.getView());
        }
        view.getCommentView().revalidate();
        view.getCommentView().repaint();
    }
    
    /**
     * Change the view
     * @param viewMode the viewMode to be switched to 

     */
    public void setTheViewViewMode(ViewMode viewMode){
        view.setViewMode(viewMode);
    }

    /**
     * Get the view for this Task.
     * @return the TaskView for the current TaskPresenter
     */
    public TaskView getView() {
        return view;
    }

    /**
     * Get the miniView for this Task.
     * @return miniView for Task
     */
    public MiniTaskView getMiniView() {
        return miniView;
    }

    /**
     * Get the model for this class.
     * @return This provider's model.
     */
    public TaskModel getModel() {
        return model;
    }

    /**
     * Set the model for this class.
     * @param model
     *            This provider's model.
     */
    public void setModel(TaskModel model) {
        this.model = model;
        activityPresenters = new ArrayList<ActivityPresenter>();
        for (int i : model.getActivityIds()) {
            ActivityPresenter p = new ActivityPresenter(i, this);
            p.load();
            activityPresenters.add(p);
        }
    }

    /** 
     * 
     * @return the bucket the task is contained within
     */
    public BucketPresenter getBucket() {
        return bucket;
    }
}
