package org.example.homedatazip.global.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.UserRole;
import org.example.homedatazip.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final List<GrantedAuthority> authorities;

    public static CustomUserDetails from(User user) {
        List<GrantedAuthority> auths = user.getRoles().stream()
                .map(UserRole::getRole)
                .map(Role::getRoleType)
                .distinct()
                .map(rt -> new SimpleGrantedAuthority("ROLE_" + rt.name()))
                .collect(Collectors.toList());

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                auths
        );
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {return email;}

    @Override
    public boolean isAccountNonExpired() {return true;}

    @Override
    public boolean isAccountNonLocked() {return true;}

    @Override
    public boolean isCredentialsNonExpired() {return true;}

    @Override
    public boolean isEnabled() {return true;}
}
