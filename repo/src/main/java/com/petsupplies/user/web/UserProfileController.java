package com.petsupplies.user.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.user.service.UserProfileService;
import com.petsupplies.user.web.dto.ChangePasswordRequest;
import com.petsupplies.user.web.dto.UpdateContactRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserProfileController {
  private final CurrentPrincipal currentPrincipal;
  private final UserProfileService userProfileService;

  public UserProfileController(CurrentPrincipal currentPrincipal, UserProfileService userProfileService) {
    this.currentPrincipal = currentPrincipal;
    this.userProfileService = userProfileService;
  }

  @PatchMapping("/users/me/password")
  public Map<String, Object> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest body) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    userProfileService.changePassword(user.getUserId(), body.newPassword());
    return Map.of("updated", true);
  }

  @PatchMapping("/users/me/contact")
  public Map<String, Object> updateContact(Authentication authentication, @Valid @RequestBody UpdateContactRequest body) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    userProfileService.updateContactPhone(user.getUserId(), body.contactPhone());
    return Map.of("updated", true);
  }
}
