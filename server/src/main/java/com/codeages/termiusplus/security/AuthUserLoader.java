package com.codeages.termiusplus.security;

import com.codeages.termiusplus.biz.user.entity.Role;
import com.codeages.termiusplus.biz.user.repository.RoleRepository;
import com.codeages.termiusplus.biz.user.repository.UserRepository;
import com.codeages.termiusplus.biz.user.repository.UserRoleRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AuthUserLoader implements UserDetailsService {

    private final UserRepository userRepo;

    private final RoleRepository roleRepo;

    private final UserRoleRepository userRoleRepository;

    public AuthUserLoader(UserRepository userRepo, RoleRepository roleRepo, UserRoleRepository userRoleRepository) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepo.getByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户(" + username + ")不存在"));

        var roles = new ArrayList<Role>();

        return null;
    }
}
