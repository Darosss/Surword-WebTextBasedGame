package com.textbasedgame.skirmishes;

import com.textbasedgame.users.User;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SkirmishesService {
    private final Datastore datastore;
    public record UpdateOnDungeonSuccessFight(
            int currentLevel,
            LocalDateTime canFightDate,
            Dungeons.DungeonData dungeonData){};

    public record UpdateOnDungeonFailedFight(LocalDateTime canFightDate) {}
    @Autowired
    public SkirmishesService(Datastore datastore) {
        this.datastore = datastore;
    }

    public Optional<Skirmish> findOneByUserId(ObjectId userId) {
        return Optional.ofNullable(datastore.find(Skirmish.class).filter(Filters.eq("user", userId)).first());
    }

    public List<Skirmish> findAll(){
        return this.datastore.find(Skirmish.class).stream().toList();
    }

    public Optional<Skirmish> findOneByUserId(String userId) {
        return this.findOneByUserId(new ObjectId(userId));
    }
    public Skirmish create(User user, int challengesIteration) {
        Skirmish skirmish = new Skirmish(user, challengesIteration );
        return datastore.save(skirmish);
    }

    public Skirmish getOrCreateSkirmish(User user, int challengesIteration){
        Optional<Skirmish> foundSkirmish = this.findOneByUserId(user.getId());
        return foundSkirmish.orElseGet(() -> this.create(user, challengesIteration));
    }

    public Skirmish generateNewChallengesForUser(User user, int challengesIteration){
        Optional<Skirmish> foundSkirmish = this.findOneByUserId(user.getId());
        if(foundSkirmish.isPresent()) {
            Skirmish skirmishInstance = foundSkirmish.get();
            Map<String, SkirmishData> newChallenges = skirmishInstance.generateChallenges(challengesIteration);
            this.updateNewChallenges(skirmishInstance.getId(), newChallenges);
            return skirmishInstance;
        };
        return create(user, challengesIteration);
    }

    public Skirmish update(Skirmish skirmish) {
        return datastore.save(skirmish);
    }

    public void updateNewChallenges(ObjectId skirmishId, Map<String, SkirmishData> challenges) {
        this.updateNewChallenges(skirmishId, challenges.values().stream().toList());
    }
    public void updateNewChallenges(ObjectId skirmishId, List<SkirmishData> challenges) {
        datastore.find(Skirmish.class).filter(Filters.eq("id", skirmishId))
                .update(new UpdateOptions(),
                        Skirmish.getMorphiaUpdateChallenges(challenges)
                );
    }
    public void updateSetChosenChallenge(ObjectId skirmishId, Skirmish.ChosenChallenge challenge) {
        datastore.find(Skirmish.class).filter(Filters.eq("id", skirmishId))
                .update(new UpdateOptions(),
                        Skirmish.getMorphiaSetChosenChallenge(challenge)
                );
    }
    public void updateUnsetChosenChallenge(ObjectId skirmishId) {
        datastore.find(Skirmish.class).filter(Filters.eq("id", skirmishId))
                .update(new UpdateOptions(),
                        Skirmish.getMorphiaUnsetChosenChallenge()
                );
    }

    public void updateOnDungeonSuccessFight(ObjectId skirmishId, UpdateOnDungeonSuccessFight update) {
        datastore.find(Skirmish.class).filter(Filters.eq("id", skirmishId))
                .update(new UpdateOptions(),
                        Skirmish.getMorphiaUpdateOnSuccessDungeon(
                                update.currentLevel(),
                                update.canFightDate(),
                                update.dungeonData()
                        )
                );
    }
    public void updateOnDungeonFailedFight(ObjectId skirmishId, UpdateOnDungeonFailedFight update) {
        datastore.find(Skirmish.class).filter(Filters.eq("id", skirmishId))
                .update(new UpdateOptions(),
                        Skirmish.getMorphiaUpdateOnFailedDungeon(update.canFightDate())
                );
    }

    public void removeById(ObjectId id){
        datastore
                .find(Skirmish.class)
                .filter(Filters.eq("id", id))
                .delete(new DeleteOptions().multi(true));

    }
}
