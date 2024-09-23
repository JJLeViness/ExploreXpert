package com.leviness.explorexpert;


import java.util.List;
import com.leviness.explorexpert.scavengerHuntTask;


public class scavengerHunt {
    private String name;
    private List<scavengerHuntTask> tasks;
    private String description;

    // Constructor
    public scavengerHunt(String name, String description, List<scavengerHuntTask> tasks) {
        this.name = name;
        this.description = description;
        this.tasks = tasks;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<scavengerHuntTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<scavengerHuntTask> tasks) {
        this.tasks = tasks;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Method to add a task to the scavenger hunt
    public void addTask(scavengerHuntTask task) {
        this.tasks.add(task);
    }

    // Method to remove a task from the scavenger hunt
    public void removeTask(scavengerHuntTask task) {
        this.tasks.remove(task);
    }
}
