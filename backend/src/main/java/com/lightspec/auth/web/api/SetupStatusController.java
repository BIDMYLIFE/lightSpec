package com.lightspec.auth.web.api;

import com.lightspec.auth.service.SetupStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/setup")
public class SetupStatusController {

    private final SetupStatusService setupStatusService;

    public SetupStatusController(SetupStatusService setupStatusService) {
        this.setupStatusService = setupStatusService;
    }

    @GetMapping("/status")
    public SetupStatusResponse status() {
        return new SetupStatusResponse(setupStatusService.isSetupRequired());
    }

    public record SetupStatusResponse(boolean setupRequired) {
    }
}
