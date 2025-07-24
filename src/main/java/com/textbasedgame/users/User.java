package com.textbasedgame.users;

import com.textbasedgame.auth.JwtTokenPayload;
import com.textbasedgame.characters.Character;
import com.textbasedgame.characters.MainCharacter;
import com.textbasedgame.users.inventory.Inventory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity("users")
public class User {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    private ObjectId id;
    private String username;
    @JsonIgnore
    private String password;
    private String email;
    private boolean isActive = true;
    private List<String> roles = new ArrayList<>();
//    @JsonIgnoreProperties("user")
    @JsonIgnore
    @Reference(idOnly = true)
    private Inventory inventory;


//    @JsonIgnoreProperties("user")
    @JsonIgnore
    @Reference(idOnly = true, ignoreMissing = true, lazy = true)
    private HashSet<Character> characters;

    private long gold = 0L;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    public User() {}

    public User(CreateUserDTO data) {
       this.username = data.getUsername();
       this.password = data.getPassword();
       this.email = data.getEmail();
       this.lastLogin = LocalDateTime.now();
       this.characters = new HashSet<>();
       this.roles.add("USER");
       this.isActive = true;
       this.inventory = data.getInventory();
    }

    public static UpdateOperator getMorphiaIncreaseGold(long value) {
        return UpdateOperators.inc("gold", value);
    }

    public static UpdateOperator getMorphiaDecreaseGold(long value) {
        return UpdateOperators.dec("gold", value);
    }

    public ObjectId getId() { return this.id; }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getGold() { return this.gold; }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void increaseGold(long value) {
        this.gold += value;
    }
    public void decreaseGold(long value) {
        this.gold -= value;
    }

    public LocalDateTime getLastLogin() {
        return this.lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public HashSet<Character> getCharacters() {
        return this.characters;
    }

    public Optional<MainCharacter> getMainCharacter() {
        if(this.characters == null) {
        return Optional.empty();
        }
        return this.characters.stream()
                .filter(v -> v instanceof MainCharacter)
                .map(v -> (MainCharacter) v)
                .findFirst();
    }

    public void setCharacters(HashSet<Character> characters) {
        this.characters = characters;
    }

    public void addCharacter(Character character){
        if(this.characters == null) this.characters = new HashSet<>();

        this.characters.add(character);
    }
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return this.inventory;
    }
    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public List<String> getRoles() {
        return this.roles;
    }
    public List<String> getRolesForToken() {
        return this.roles.stream().map((role)->"ROLE_"+role).toList();
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public JwtTokenPayload getDetailsForToken(Date expirationTime){
        return new JwtTokenPayload(this.id.toString(), this.email, this.username, expirationTime);
    }
    @Override
    public String toString() {
        return "User{" +
                "id='" + this.id + '\'' +
                ", username='" + this.username + '\'' +
                ", email='" + this.email + '\'' +
                ", lastLogin=" + this.lastLogin +
                '}';
    }
}
