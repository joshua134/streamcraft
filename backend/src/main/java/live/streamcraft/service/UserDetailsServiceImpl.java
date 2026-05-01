package live.streamcraft.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import live.streamcraft.entity.User;
import live.streamcraft.repository.UserRepository;
import live.streamcraft.security.UserPrincipal;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	private final UserRepository repository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = repository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("username not found."));
		return new UserPrincipal(user);
		//return (UserDetails) repository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("username not found."));
	}

}
