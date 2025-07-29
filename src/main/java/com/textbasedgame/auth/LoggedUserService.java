package com.textbasedgame.auth;

import com.textbasedgame.characters.CharacterService;
import com.textbasedgame.characters.MainCharacter;
import com.textbasedgame.common.ResourceNotFoundException;
import com.textbasedgame.settings.XpService;
import com.textbasedgame.users.User;
import com.textbasedgame.users.UserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoggedUserService {
    private final XpService xpService;
    private final UserService userService;
    private final CharacterService characterService;
    private final AuthenticationFacade authenticationFacade;
    private LoggedUserService(XpService xpService, UserService userService, CharacterService characterService, AuthenticationFacade authenticationFacade) {
        this.xpService = xpService;
        this.userService = userService;
        this.characterService = characterService;
        this.authenticationFacade = authenticationFacade;
    }

    public record MainCharacterDetails (int health, int maxHealth, int level, long experience, long neededExp){}
    public record ProfileUserDetails(User user, Optional<MainCharacterDetails> heroDetails){}


    public User getLoggedUserDetails() throws Exception {
        String userId = authenticationFacade.getJwtTokenPayload().id();
        Optional<User> foundUser = this.userService.findOneById(userId);
        return foundUser.orElseThrow(() -> new ResourceNotFoundException("Not found user data. Please try again later"));
    }

    public ProfileUserDetails getProfileUserDetails() throws Exception {
        User user = getLoggedUserDetails();
        return new ProfileUserDetails(user, getMainCharacterDetails());
    }

    private Optional<MainCharacterDetails> getMainCharacterDetails() throws Exception {
        String userId = this.authenticationFacade.getJwtTokenPayload().id();
        Optional<MainCharacter> mainCharacter = this.characterService.findMainCharacterByUserId(userId);
        return mainCharacter.map((character)->new MainCharacterDetails(
                character.getHealth(), character.getStats().getMaxHealthEffValue(), character.getLevel(), character.getExperience(),
                this.xpService.getNeededXpForNextLevel(character.getLevel())
        ));
    }
}