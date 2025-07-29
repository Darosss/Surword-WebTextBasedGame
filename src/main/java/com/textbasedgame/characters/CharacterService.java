package com.textbasedgame.characters;

import com.textbasedgame.characters.equipment.CharacterEquipment;
import com.textbasedgame.users.User;
import com.textbasedgame.utils.TransactionsUtils;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.transactions.MorphiaSession;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CharacterService {
    private final Datastore datastore;

    @Autowired
    public CharacterService(Datastore datastore) {
        this.datastore = datastore;
    }

    public record  CreateCharacterReturn<T extends Character>(boolean success, String message, Optional<T> character){}

    //TODO: make update better later, for now w/e
    public Character update(Character character) {
        return datastore.save(character);
    }

    public boolean handlePostFightUpdate(String id, long newExp, int level, Optional<Integer> newHP) {
        UpdateOperator[] updates = newHP
                .<UpdateOperator[]>map(hp -> new UpdateOperator[] {
                        MainCharacter.getMorphiaSetExperience(newExp),
                        MainCharacter.getMorphiaSetLevel(level),
                        MainCharacter.getMorphiaSetCharacterHealth(newHP.get()),
                })
                .orElseGet(() -> new UpdateOperator[] {
                        MainCharacter.getMorphiaSetExperience(newExp),
                        MainCharacter.getMorphiaSetLevel(level),
                });

        datastore.find(MainCharacter.class)
                .filter(Filters.eq("id", new ObjectId(id)))
                .update(new UpdateOptions(),updates);

        return true;
    }

    public <T extends Character> List<T> findAll(Class<T> characterClass){
        return this.datastore.find(characterClass).stream().toList();
    }
    public List<MercenaryCharacter> findUserMercenaries(ObjectId userId) {
        return datastore.find(MercenaryCharacter.class).filter(Filters.eq("user", userId)).stream().toList();
    }
    public List<MercenaryCharacter> findUserMercenaries(String userId) {
        return this.findUserMercenaries(new ObjectId(userId));
    }

    public Integer getMainCharacterLevelByUserId(ObjectId userId) {
        return datastore.find(MainCharacter.class).filter(Filters.eq("user", userId)).iterator(new FindOptions().projection().include("id", "level")).
                tryNext().getLevel();
    }

    public <T extends Character> CreateCharacterReturn<T> createCharacter(Class<T> characterClass, User user, String name) throws Exception {
        try(MorphiaSession session = datastore.startSession()) {
            session.startTransaction();
            CharacterEquipment equipment = TransactionsUtils.createNewEquipment(session);
            Optional<T> createdChar = Optional.empty();

            if (MainCharacter.class.isAssignableFrom(characterClass)) {
               createdChar = (Optional<T>) Optional.of(session.save(new MainCharacter(name, user, equipment, true)));
            } else if (MercenaryCharacter.class.isAssignableFrom(characterClass)) {
               createdChar = (Optional<T>) Optional.of(session.save(new MercenaryCharacter(name, user, equipment, true)));
            }

            if(createdChar.isEmpty()) {
                session.abortTransaction();
                return new CreateCharacterReturn<T>(false, "Something went wrong when trying to create character", createdChar);
            }

            user.addCharacter(createdChar.get());
            //this is for update user <- change later?
            session.save(user);

            equipment.setCharacter(createdChar.get());

            // same as above omment  but for equipment
            session.save(equipment);

            session.commitTransaction();
            return new CreateCharacterReturn<T>(true, "Successfully created character", createdChar);

        }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }
    public Optional<Character> findById(String id) {
        return Optional.ofNullable(datastore.find(Character.class)
                .filter(
                        Filters.eq("id", new ObjectId(id))).first());
    }
    public Optional<Character> findOneByUserId(String userId) {
        return Optional.ofNullable(datastore.find(Character.class).filter(Filters.eq("user", new ObjectId(userId))).first());
    }
    public Character findOne(){
        return datastore.find(Character.class).first();
    }

    public Optional<MainCharacter> findMainCharacterByUserId(ObjectId userId){
        return Optional.ofNullable(datastore.find(MainCharacter.class).filter(
                Filters.eq("user", userId)
        ).first());
    }
    public Optional<MainCharacter> findMainCharacterByUserId(String userId){
        return findMainCharacterByUserId(new ObjectId(userId));
    }

    public void removeAllCharactersAndEquipments(){
        datastore
                .find(Character.class)
                .delete(new DeleteOptions().multi(true));

        datastore
                .find(CharacterEquipment.class)
                .delete(new DeleteOptions().multi(true));
    }
}
