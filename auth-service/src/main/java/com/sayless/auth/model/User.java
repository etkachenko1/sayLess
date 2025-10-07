//User Schema
package com.sayless.auth.model;

// classes from the Spring Data MongoDB library
import org.springframework.data.annotation.Id; //mark primary key
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document; //to map this class to a MongoDB collection

@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;
    private String email;
    private String password;

    private String profilePic;
    private String bio;

    public User() {}
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    //getters and setters
    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    
    public String getUsername() {return username;}
    public void setUsername(String username) {this.username = username;}

    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}

    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}

    public String getProfilePic() {return profilePic;}
    public void setProfilePic(String profilePic) {this.profilePic = profilePic;}

    public String getBio() {return bio;}
    public void setBio(String bio) {this.bio = bio;}

    
}
