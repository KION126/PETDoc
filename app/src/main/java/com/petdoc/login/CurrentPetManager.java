package com.petdoc.login;

public class CurrentPetManager {
    private static CurrentPetManager instance;
    private String currentPetId;

    private CurrentPetManager() {}

    public static synchronized CurrentPetManager getInstance() {
        if (instance == null) {
            instance = new CurrentPetManager();
        }
        return instance;
    }

    public void setCurrentPetId(String petId) {
        this.currentPetId = petId;
    }

    public String getCurrentPetId() {
        return currentPetId;
    }
}

