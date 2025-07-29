package com.textbasedgame.merchants;


import com.textbasedgame.auth.AuthenticationFacade;
import com.textbasedgame.auth.LoggedUserService;
import com.textbasedgame.auth.SecuredRestController;
import com.textbasedgame.characters.CharacterService;
import com.textbasedgame.characters.MainCharacter;
import com.textbasedgame.response.CustomResponse;
import com.textbasedgame.users.User;
import com.textbasedgame.users.UserService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

//TODO: add refreshing commodity after time
@RestController("merchants")
@RequestMapping("merchants")
public class MerchantsController implements SecuredRestController {
    private final AuthenticationFacade authenticationFacade;
    private final MerchantsService service;
    private final CharacterService characterService;
    private final LoggedUserService loggedUserService;

    @Autowired
    public MerchantsController(AuthenticationFacade authenticationFacade, MerchantsService service,
                               CharacterService characterService,
                               LoggedUserService loggedUserService) {
        this.authenticationFacade = authenticationFacade;
        this.service = service;
        this.characterService = characterService;
        this.loggedUserService = loggedUserService;
    }

    @GetMapping("/your-merchant")
    public CustomResponse<Merchant> getMerchant() throws Exception {
        User loggedUser = this.loggedUserService.getLoggedUserDetails();
        Optional<MainCharacter> mainCharacter = this.characterService.findMainCharacterByUserId(loggedUser.getId());

        if(mainCharacter.isEmpty()) throw new BadRequestException("You need to create main character, before visit merchant");

        Merchant merchant = this.service.getOrCreateMerchant(loggedUser, mainCharacter.get().getLevel());

        return new CustomResponse<>(HttpStatus.OK, merchant);
    }

    @PostMapping("/buy-item/{itemId}")
    public CustomResponse<Merchant.MerchantTransaction> buyItemFromMerchant(
            @PathVariable String itemId
    ) throws Exception {
        MerchantsService.MerchantActionReturn returnData = this.service.buyItemFromMerchant(
                this.authenticationFacade.getJwtTokenPayload().id(), itemId);
        if(returnData.success() && returnData.transaction().isPresent())
            return new CustomResponse<>(HttpStatus.OK, returnData.message(), returnData.transaction().get());
        throw new BadRequestException(returnData.message());
    }

    @PostMapping("/sell-item/{itemId}")
    public CustomResponse<Merchant.MerchantTransaction> sellItemToMerchant(
            @PathVariable String itemId
    ) throws Exception {
        MerchantsService.MerchantActionReturn returnData = this.service.sellItemToMerchant(
                this.authenticationFacade.getJwtTokenPayload().id(), itemId);
        if(returnData.success() && returnData.transaction().isPresent())
            return new CustomResponse<>(HttpStatus.OK, returnData.message(), returnData.transaction().get());
        throw new BadRequestException(returnData.message());
    }


}
