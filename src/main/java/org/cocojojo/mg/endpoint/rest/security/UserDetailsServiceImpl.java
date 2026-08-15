package org.cocojojo.mg.endpoint.rest.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.repository.UserRepository;
import org.cocojojo.mg.util.SecurityUtil;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;
  private final SecurityUtil securityUtil;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    var user =
        userRepository
            .findByEmailIgnoreCase(email)
            .filter(u -> !u.isDeleted())
            .orElseThrow(
                () -> new UsernameNotFoundException("No account found for email " + email));

    var role = securityUtil.getRoleFromUser(user);
    return new User(
        user.getEmail(),
        user.getPassword(),
        List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
  }
}
