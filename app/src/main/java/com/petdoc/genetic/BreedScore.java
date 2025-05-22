package com.petdoc.genetic;

import java.io.Serializable;

public class BreedScore implements Serializable {
    private String breed;
    private int score;

    public BreedScore(String breed, int score) {
        this.breed = breed;
        this.score = score;
    }

    public String getBreed() {
        return breed;
    }

    public int getScore() {
        return score;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
