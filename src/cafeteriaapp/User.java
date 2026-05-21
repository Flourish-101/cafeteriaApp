/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cafeteriaapp;

/**
 *
 * @author DELL
 */
public class User {

    private String firstName;
    private String lastName;
    private String email;
    private float accountBalance;

    public User(String firstName, String lastName, String email, float accountBalance) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.accountBalance = accountBalance;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public float getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(float accountBalance) {
        this.accountBalance = accountBalance;
    }

}
