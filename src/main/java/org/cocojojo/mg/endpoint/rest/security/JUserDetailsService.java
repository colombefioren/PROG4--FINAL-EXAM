package org.cocojojo.mg.endpoint.rest.security;

import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.repository.UserRepository;
import org.cocojojo.mg.util.SecurityUtil;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class JUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final SecurityUtil securityUtil;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    var user =
        userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    if (user.isDeleted()) {
      throw new UsernameNotFoundException("User is disabled");
    }
    var role = securityUtil.getRoleFromUser(user);
    return User.withUsername(user.getEmail())
        .password(user.getPassword())
        .authorities("ROLE_" + role.name())
        .build();
  }
}
