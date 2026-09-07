package com.berlinbrown.mech.umbra.rpg;

public class Character {

    public String name;
    public int strength;
    public int constitution;
    public int dexterity;
    public int defense;
    public int attackDamage;
    public int healthPoints;
    public int maxHealthPoints;

    public Character(final String name) {
        this.name = name;
    }

    public boolean isAlive() {
        return healthPoints > 0;
    }

    public void takeDamage(int damage) {
        int actualDamage = Math.max(0, damage - defense);
        healthPoints = Math.max(0, healthPoints - actualDamage);
        System.out.println(name + " takes " + actualDamage + " damage! Remaining HP: " + healthPoints);
    }

    public void attack(final Character target) {
        System.out.println(name + " attacks " + target.name + "!");
        target.takeDamage(attackDamage);
    }
}
