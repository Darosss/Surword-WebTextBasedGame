package com.textbasedgame.users;

import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.filters.Filters;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final Datastore datastore;

    @Autowired
    public UserService(Datastore datastore) {
        this.datastore = datastore;
    }

    public List<User> findAll() {
        return datastore.find(User.class).iterator(new FindOptions().projection().exclude("password")).toList();
    }

    public Optional<User> findOneByEmail(String email) {
        return Optional.ofNullable(datastore.find(User.class).filter(Filters.eq("email", email)).first());
    }
    public User create(User user) {
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));

        return datastore.save(user);
    }


    public Optional<User> findOneById(String id) {
        return Optional.ofNullable(datastore.find(User.class)
                .filter(Filters.eq("id", new ObjectId(id))).first());
    }
    public Optional<User> findOneByIdNoPopulateCharacter(String id) {
        return Optional.of(datastore.find(User.class)
                .filter(Filters.eq("id", new ObjectId(id)))
                .iterator(new FindOptions().projection().exclude("characters")).tryNext());
    }

    //TODO: make update better later, for now w/e
    public User update(User user) {
        //TODO: add encrypting for password in update if changes.
        return datastore.save(user);
    }
}
