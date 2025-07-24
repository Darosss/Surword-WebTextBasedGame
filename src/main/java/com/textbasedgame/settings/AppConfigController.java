package com.textbasedgame.settings;

import com.textbasedgame.auth.SecuredRestController;
import com.textbasedgame.response.CustomResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController("admin/app-config")
@RequestMapping("admin/app-config")
public class AppConfigController implements SecuredRestController {
    private final AppConfigManager appConfigManager;

    @Autowired
    public AppConfigController(AppConfigManager appConfigManager){
        this.appConfigManager = appConfigManager;
    }

    @GetMapping("/")
    public CustomResponse<AppConfig> getConfigs() throws Exception {
        AppConfig cfg = this.appConfigManager.getConfig();
        System.out.println(cfg.toString());
        return new CustomResponse<>(HttpStatus.OK, cfg);
    }

    @PostMapping("/")
    public CustomResponse<AppConfig> updateConfigs(
            @RequestBody AppConfig updateConfigBody
            ) throws Exception{

        this.appConfigManager.updateConfig(updateConfigBody);
        return new CustomResponse<>(HttpStatus.OK, "Successfully updated configs", this.appConfigManager.getConfig());
    }
}
